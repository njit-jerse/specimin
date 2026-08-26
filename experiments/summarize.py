"""Summarizes the per-target results of run_experiment.sh.

Usage: summarize.py RESULTS_CSV

Prints the compile-status transition matrix between the two Specimin builds, the same broken down
by repository, the crash counts, and how long an invocation took. The number that matters is
"compiles -> does not compile": Specimin's output is allowed to be imprecise but not to fail to
compile, so any target in that cell is a regression that should be reduced into a test case.
"""

import collections
import csv
import statistics
import sys

# Compile outcomes, in the order they are reported.
OUTCOMES = ["compiles", "fails", "empty", "na"]
LABELS = {
    "compiles": "compiles",
    "fails": "does not compile",
    "empty": "empty output",
    "na": "no output (crash/timeout)",
}


def read(path):
    """Returns the rows of the results file. path: the results CSV written by run_experiment.sh."""
    with open(path, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def outcome(row, version):
    """Returns one row's outcome for one version. row: a results row. version: "base" or "new"."""
    status = row[f"{version}_status"]
    if status != "ok":
        return "na"
    return row[f"{version}_compiles"]


def main() -> None:
    rows = read(sys.argv[1])
    if not rows:
        print("no results")
        return

    transitions = collections.Counter(
        (outcome(row, "base"), outcome(row, "new")) for row in rows
    )

    print(f"targets run: {len(rows)}")
    print(f"repositories: {len(set(row['repo'] for row in rows))}")
    print()
    print("Outcome of each build:")
    for version in ("base", "new"):
        counts = collections.Counter(outcome(row, version) for row in rows)
        parts = [f"{LABELS[o]}={counts[o]}" for o in OUTCOMES if counts[o]]
        print(f"  {version:5s} {', '.join(parts)}")
    print()

    print("Transition matrix (rows: base, columns: new):")
    header = "".join(f"{o:>12s}" for o in OUTCOMES)
    print(f"  {'':>26s}{header}")
    for before in OUTCOMES:
        cells = "".join(f"{transitions[(before, after)]:>12d}" for after in OUTCOMES)
        print(f"  {LABELS[before]:>26s}{cells}")
    print()

    # Fingerprints are only present in results produced after this column was added.
    if rows[0].get("base_hash") is not None:
        comparable = [r for r in rows if r["base_status"] == "ok" and r["new_status"] == "ok"]
        differing = [r for r in comparable if r["base_hash"] != r["new_hash"]]
        share = 100 * len(differing) / len(comparable) if comparable else 0
        print(
            f"Emitted programs differ textually on {len(differing)} of {len(comparable)} targets"
            f" that both builds completed ({share:.0f}%)."
        )
        print()

    regressions = [r for r in rows if outcome(r, "base") == "compiles" and outcome(r, "new") != "compiles"]
    improvements = [r for r in rows if outcome(r, "base") != "compiles" and outcome(r, "new") == "compiles"]
    print(f"REGRESSIONS   (compiled before, does not now): {len(regressions)}")
    print(f"IMPROVEMENTS  (did not compile before, does now): {len(improvements)}")
    print()

    if regressions:
        print("Regressing targets:")
        for row in regressions:
            print(f"  [{row['id']}] {row['signature']}")
            print(f"      {row['target_file']}  base=compiles  new={outcome(row, 'new')}")
        print()

    if improvements:
        print("Improved targets:")
        for row in improvements:
            print(f"  [{row['id']}] {row['signature']}")
            print(f"      {row['target_file']}  base={outcome(row, 'base')}  new=compiles")
        print()

    print("By repository (targets / regressions / improvements):")
    for repo in sorted(set(row["repo"] for row in rows)):
        repo_rows = [r for r in rows if r["repo"] == repo]
        reg = sum(1 for r in repo_rows if r in regressions)
        imp = sum(1 for r in repo_rows if r in improvements)
        base_ok = sum(1 for r in repo_rows if outcome(r, "base") == "compiles")
        new_ok = sum(1 for r in repo_rows if outcome(r, "new") == "compiles")
        print(f"  {repo:24s} {len(repo_rows):4d}   {reg:3d}   {imp:3d}   (compiling: base {base_ok}, new {new_ok})")
    print()

    for version in ("base", "new"):
        statuses = collections.Counter(row[f"{version}_status"] for row in rows)
        secs = [float(row[f"{version}_secs"]) for row in rows]
        print(
            f"{version:5s} status: " + ", ".join(f"{k}={v}" for k, v in sorted(statuses.items()))
            + f" | seconds per invocation: mean {statistics.mean(secs):.1f},"
            + f" median {statistics.median(secs):.1f}, max {max(secs):.1f}"
        )


if __name__ == "__main__":
    main()
