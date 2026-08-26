#!/bin/sh

# Compares two builds of Specimin on every (or a sample of every) method and field of a set of
# GitHub repositories, and reports how often each build's output compiles. The point is to find
# targets whose output compiles with one build but not the other: Specimin's output is allowed to
# be imprecise, but it is not allowed to fail to compile, so a compiling -> non-compiling
# transition is a regression. See README.md for the full workflow.
#
# Usage:
#   run_experiment.sh --base-jar JAR --new-jar JAR [options]
#
# Options:
#   --base-jar JAR     Specimin jar for the "before" version (required)
#   --new-jar JAR      Specimin jar for the "after" version (required)
#   --repos FILE       CSV of repositories to run on (default: experiments/repos.csv)
#   --workdir DIR      where to clone repositories and write results (default: ./ab-experiment)
#   --per-repo N       how many targets to sample per repository; 0 means all (default: 25)
#   --seed N           seed for the target sample, so a run can be repeated exactly (default: 0)
#   --timeout SECS     per-Specimin-invocation timeout (default: 120)
#   --jobs N           how many targets to run concurrently (default: 4)
#   --java PATH        JVM used to run Specimin; must be the JDK the jars were built for
#   --javac PATH       compiler used to check Specimin's output
#   --keep-all-output  keep every output directory, not just the ones where the builds disagreed
#
# The results land in WORKDIR/results.csv, one row per target, and are summarized on standard
# output by summarize.py.

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

REPOS="${SCRIPT_DIR}/repos.csv"
WORKDIR="$(pwd)/ab-experiment"
PER_REPO=25
SEED=0
TIMEOUT_SECS=120
JOBS=4
JAVA_BIN=java
JAVAC_BIN=javac
BASE_JAR=""
NEW_JAR=""
KEEP_ALL_OUTPUT=no

while [ $# -gt 0 ]; do
  case "$1" in
    --base-jar) BASE_JAR="$2"; shift 2 ;;
    --new-jar) NEW_JAR="$2"; shift 2 ;;
    --repos) REPOS="$2"; shift 2 ;;
    --workdir) WORKDIR="$2"; shift 2 ;;
    --per-repo) PER_REPO="$2"; shift 2 ;;
    --seed) SEED="$2"; shift 2 ;;
    --timeout) TIMEOUT_SECS="$2"; shift 2 ;;
    --jobs) JOBS="$2"; shift 2 ;;
    --java) JAVA_BIN="$2"; shift 2 ;;
    --javac) JAVAC_BIN="$2"; shift 2 ;;
    --keep-all-output) KEEP_ALL_OUTPUT=yes; shift ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

if [ -z "${BASE_JAR}" ] || [ -z "${NEW_JAR}" ]; then
  echo "both --base-jar and --new-jar are required" >&2
  exit 2
fi

BASE_JAR=$(cd "$(dirname "${BASE_JAR}")" && pwd)/$(basename "${BASE_JAR}")
NEW_JAR=$(cd "$(dirname "${NEW_JAR}")" && pwd)/$(basename "${NEW_JAR}")
CHECKER_QUAL="${SCRIPT_DIR}/../src/test/resources/shared/checker-qual-3.42.0.jar"

mkdir -p "${WORKDIR}/repos" "${WORKDIR}/targets" "${WORKDIR}/results/parts" "${WORKDIR}/out"
JOBS_FILE="${WORKDIR}/jobs.tsv"
: > "${JOBS_FILE}"

# Step 1: clone each repository at its pinned commit and enumerate its targets.
# A pinned commit keeps the experiment reproducible; --depth 1 keeps the clone cheap.
while IFS=, read -r name url sha srcroot; do
  case "${name}" in ''|'#'*) continue ;; esac
  repo_dir="${WORKDIR}/repos/${name}"
  if [ ! -d "${repo_dir}/.git" ]; then
    echo "cloning ${name} at ${sha}"
    rm -rf "${repo_dir}"
    mkdir -p "${repo_dir}"
    git -C "${repo_dir}" init -q
    git -C "${repo_dir}" remote add origin "${url}"
    git -C "${repo_dir}" fetch -q --depth 1 origin "${sha}"
    git -C "${repo_dir}" checkout -q FETCH_HEAD
  fi

  full_srcroot="${repo_dir}/${srcroot}"
  if [ ! -d "${full_srcroot}" ]; then
    echo "no such source root, skipping ${name}: ${full_srcroot}" >&2
    continue
  fi

  all_targets="${WORKDIR}/targets/${name}.all.tsv"
  if [ ! -s "${all_targets}" ]; then
    echo "enumerating targets in ${name}"
    (cd "${SCRIPT_DIR}" && "${JAVA_BIN}" -cp "${BASE_JAR}" EnumerateTargets.java "${full_srcroot}") \
      > "${all_targets}" 2> "${WORKDIR}/targets/${name}.enumerate.log"
  fi

  sampled="${WORKDIR}/targets/${name}.sample.tsv"
  python3 "${SCRIPT_DIR}/sample_targets.py" "${all_targets}" "${PER_REPO}" "${SEED}" "${name}" \
    > "${sampled}"

  # Build the job list: one line per target, which run_one_target.sh consumes as six arguments.
  awk -v repo="${name}" -v root="${full_srcroot}" -F'\t' \
    '{ printf "%s-%04d\t%s\t%s\t%s\t%s\t%s\n", repo, NR, repo, root, $1, $2, $3 }' \
    "${sampled}" >> "${JOBS_FILE}"
done < "${REPOS}"

total=$(wc -l < "${JOBS_FILE}" | tr -d ' ')
echo "running ${total} targets, ${JOBS} at a time, ${TIMEOUT_SECS}s timeout per invocation"

# Step 2: run both builds on every target. Fields are passed NUL-separated so that xargs does
# not try to interpret anything inside a signature.
export BASE_JAR NEW_JAR TIMEOUT_SECS JAVA_BIN JAVAC_BIN CHECKER_QUAL KEEP_ALL_OUTPUT
export RESULTS_DIR="${WORKDIR}/results"
export OUT_DIR="${WORKDIR}/out"
start=$(date +%s)
tr '\t\n' '\0\0' < "${JOBS_FILE}" \
  | xargs -0 -P "${JOBS}" -n 6 sh "${SCRIPT_DIR}/run_one_target.sh"
end=$(date +%s)
echo "finished in $((end - start)) seconds of wall-clock time"

# Step 3: collect and summarize.
{
  echo "id,repo,target_file,kind,signature,base_status,base_compiles,base_files,base_secs,base_hash,new_status,new_compiles,new_files,new_secs,new_hash"
  cat "${WORKDIR}"/results/parts/*.csv
} > "${WORKDIR}/results.csv"

python3 "${SCRIPT_DIR}/summarize.py" "${WORKDIR}/results.csv" | tee "${WORKDIR}/summary.txt"
echo
echo "per-target results: ${WORKDIR}/results.csv"
echo "kept output for disagreeing targets: ${WORKDIR}/out"
