"""Collects the reproducers for every target that regressed, into one self-contained directory.

Usage: archive_regressions.py WORKDIR ARCHIVE_DIR [--repos repos.csv]

A target regressed if the base build's output compiled and the new build's did not -- including
the case where the new build crashed, timed out, or emitted nothing, since none of those produce
a compilable program either. For each one this writes ARCHIVE_DIR/<id>/ containing a README with
the exact command to reproduce it, the javac diagnostics that the new build's output provokes,
and both builds' emitted programs (run run_experiment.sh with --keep-all-output if you want the
outputs of non-regressing targets too).

The archive is meant to be read later, possibly on a different machine, so the README records
the repository and commit rather than a path into the work directory. Each case's README.md is
regenerated on every run of this script; put anything hand-written, such as a diagnosis, in a
NOTES.md beside it instead.
"""

import csv
import pathlib
import shutil
import sys

# How many lines of javac output to inline into the README; the full log is copied alongside.
DIAGNOSTIC_LINES = 40


def outcome(row, version):
    """Returns one row's outcome. row: a results row. version: "base" or "new"."""
    return "na" if row[f"{version}_status"] != "ok" else row[f"{version}_compiles"]


def read_repos(path):
    """Returns {name: (url, sha, srcroot)}. path: the repos.csv used for the run."""
    repos = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            name, url, sha, srcroot = line.split(",")
            repos[name] = (url, sha, srcroot)
    return repos


def main() -> None:
    workdir = pathlib.Path(sys.argv[1])
    archive = pathlib.Path(sys.argv[2])
    repos_csv = sys.argv[4] if len(sys.argv) > 4 else pathlib.Path(__file__).parent / "repos.csv"
    repos = read_repos(repos_csv)

    with open(workdir / "results.csv", encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))

    regressions = [
        r for r in rows if outcome(r, "base") == "compiles" and outcome(r, "new") != "compiles"
    ]
    if not regressions:
        print("no regressions to archive")
        return

    archive.mkdir(parents=True, exist_ok=True)
    for row in regressions:
        target_id = row["id"]
        url, sha, srcroot = repos[row["repo"]]
        destination = archive / target_id
        if destination.exists():
            shutil.rmtree(destination)
        destination.mkdir(parents=True)

        option = "--targetField" if row["kind"] == "field" else "--targetMethod"
        javac_log = workdir / "out" / "new" / f"{target_id}.javac.log"
        diagnostics = javac_log.read_text(encoding="utf-8", errors="replace") if javac_log.exists() else ""
        specimin_log = workdir / "out" / "new" / f"{target_id}.specimin.log"

        readme = [
            f"# {target_id}: {row['signature']}",
            "",
            f"Specimin's output for this target compiled before the change and does not after",
            f"(new build: {outcome(row, 'new')}).",
            "",
            "## Reproducing",
            "",
            "```sh",
            f"git clone {url} repo && git -C repo checkout {sha}",
            "./gradlew run -PskipCheckerFramework --args='"
            + f"--root \"repo/{srcroot}\" --outputDirectory \"out\" "
            + f"--targetFile \"{row['target_file']}\" {option} \"{row['signature']}\"'",
            "```",
            "",
            "Then compile the result:",
            "",
            "```sh",
            "javac -classpath src/test/resources/shared/checker-qual-3.42.0.jar $(find out -name '*.java')",
            "```",
            "",
            "## Result",
            "",
            f"| | files emitted | Specimin exit | output compiles |",
            f"| --- | --- | --- | --- |",
            f"| before | {row['base_files']} | {row['base_status']} | {row['base_compiles']} |",
            f"| after | {row['new_files']} | {row['new_status']} | {row['new_compiles']} |",
            "",
        ]
        if diagnostics.strip():
            lines = diagnostics.splitlines()
            readme += [
                f"## javac on the new output (first {DIAGNOSTIC_LINES} lines; full log in javac.log)",
                "",
                "```",
                *lines[:DIAGNOSTIC_LINES],
                "```",
                "",
            ]
        (destination / "README.md").write_text("\n".join(readme), encoding="utf-8")

        for name, path in (("javac.log", javac_log), ("specimin.log", specimin_log)):
            if path.exists():
                shutil.copy(path, destination / name)
        for version in ("base", "new"):
            source = workdir / "out" / version / target_id
            if source.is_dir():
                shutil.copytree(source, destination / f"{version}-output")

    print(f"archived {len(regressions)} regression(s) to {archive}")
    for row in regressions:
        print(f"  {row['id']}: {row['signature']}")


if __name__ == "__main__":
    main()
