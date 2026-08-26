#!/bin/sh

# Runs two builds of Specimin on a single target and records whether each one's output compiles.
# This script is invoked (usually in parallel) by run_experiment.sh; see that script and README.md.
#
# Usage: run_one_target.sh ID REPO SRCROOT TARGET_FILE KIND SIGNATURE
#   ID          unique identifier for this target, used to name output directories
#   REPO        name of the repository the target came from
#   SRCROOT     absolute path of the Java source root to pass to --root
#   TARGET_FILE path of the target's file, relative to SRCROOT
#   KIND        "method" or "field"
#   SIGNATURE   the target signature, in --targetMethod / --targetField format
#
# The following environment variables must be set by the caller:
#   BASE_JAR, NEW_JAR   the two Specimin jars to compare
#   RESULTS_DIR         where to write this target's one-line CSV result
#   OUT_DIR             where to write Specimin's output and javac's diagnostics
#   TIMEOUT_SECS        per-Specimin-invocation timeout
#   JAVA_BIN, JAVAC_BIN the JVM to run Specimin with and the compiler to check its output with
#   CHECKER_QUAL        path to checker-qual.jar, which Specimin output often refers to
#   KEEP_ALL_OUTPUT     if "yes", keep output directories even when nothing regressed

ID="$1"
REPO="$2"
SRCROOT="$3"
TARGET_FILE="$4"
KIND="$5"
SIGNATURE="$6"

if [ "${KIND}" = "field" ]; then
  TARGET_OPT="--targetField"
else
  TARGET_OPT="--targetMethod"
fi

now() {
  if command -v gdate > /dev/null 2>&1; then gdate +%s.%N; else date +%s; fi
}

# Runs one Specimin build on the target, then javac on whatever it produced.
# Sets the globals STATUS (ok/crash/timeout), COMPILES (compiles/fails/empty/na),
# FILES (number of .java files emitted) and SECS (wall-clock seconds for Specimin).
# $1: the jar to run. $2: a short name for it, used in path names.
run_version() {
  jar="$1"
  tag="$2"
  outdir="${OUT_DIR}/${tag}/${ID}"
  rm -rf "${outdir}"
  mkdir -p "${outdir}"

  start=$(now)
  # google-java-format, which Specimin runs over its output, reaches into jdk.compiler
  # internals; these are the same exports build.gradle's run task passes.
  timeout "${TIMEOUT_SECS}" "${JAVA_BIN}" -Xmx4g \
    --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
    --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
    --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
    --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
    --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
    --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
    -jar "${jar}" \
    --root "${SRCROOT}" \
    --outputDirectory "${outdir}" \
    --targetFile "${TARGET_FILE}" \
    "${TARGET_OPT}" "${SIGNATURE}" \
    > "${outdir}.specimin.log" 2>&1
  exit_code=$?
  end=$(now)
  SECS=$(awk -v s="${start}" -v e="${end}" 'BEGIN { printf "%.1f", e - s }')

  if [ "${exit_code}" = 124 ]; then
    STATUS=timeout
  elif [ "${exit_code}" = 0 ]; then
    STATUS=ok
  else
    STATUS=crash
  fi

  FILES=$(find "${outdir}" -name "*.java" | wc -l | tr -d ' ')

  # A fingerprint of the emitted program, so that the summary can say how often the two builds
  # produced different output at all. Without it, "no regressions" is ambiguous: it could mean the
  # change is safe, or that it did not change anything on these inputs.
  HASH=$(find "${outdir}" -name "*.java" | LC_ALL=C sort | xargs cat 2>/dev/null | shasum | cut -c1-12)

  if [ "${STATUS}" != "ok" ]; then
    COMPILES=na
  elif [ "${FILES}" = 0 ]; then
    COMPILES=empty
  else
    classes="${outdir}.classes"
    rm -rf "${classes}"
    mkdir -p "${classes}"
    # javac relies on word splitting here.
    # shellcheck disable=SC2046
    "${JAVAC_BIN}" -nowarn -proc:none -encoding UTF-8 -classpath "${CHECKER_QUAL}" \
      -d "${classes}" $(find "${outdir}" -name "*.java") > "${outdir}.javac.log" 2>&1
    if [ $? = 0 ]; then COMPILES=compiles; else COMPILES=fails; fi
    rm -rf "${classes}"
  fi
}

run_version "${BASE_JAR}" base
BASE_STATUS="${STATUS}"; BASE_COMPILES="${COMPILES}"; BASE_FILES="${FILES}"; BASE_SECS="${SECS}"
BASE_HASH="${HASH}"

run_version "${NEW_JAR}" new
NEW_STATUS="${STATUS}"; NEW_COMPILES="${COMPILES}"; NEW_FILES="${FILES}"; NEW_SECS="${SECS}"
NEW_HASH="${HASH}"

# Output is only worth keeping when the two versions disagree; otherwise it is many
# megabytes of duplicate Java source per target.
if [ "${KEEP_ALL_OUTPUT}" != "yes" ] \
    && [ "${BASE_COMPILES}" = "${NEW_COMPILES}" ] && [ "${BASE_STATUS}" = "${NEW_STATUS}" ]; then
  rm -rf "${OUT_DIR}/base/${ID}" "${OUT_DIR}/new/${ID}"
fi

# One CSV line per target, written to its own file so that parallel jobs cannot interleave.
# The signature is quoted because it contains commas.
printf '%s,%s,%s,%s,"%s",%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
  "${ID}" "${REPO}" "${TARGET_FILE}" "${KIND}" "${SIGNATURE}" \
  "${BASE_STATUS}" "${BASE_COMPILES}" "${BASE_FILES}" "${BASE_SECS}" "${BASE_HASH}" \
  "${NEW_STATUS}" "${NEW_COMPILES}" "${NEW_FILES}" "${NEW_SECS}" "${NEW_HASH}" \
  > "${RESULTS_DIR}/parts/${ID}.csv"
