"""Deterministically samples targets from an enumerated target list.

Usage: sample_targets.py TARGETS_TSV COUNT SEED SALT

Reads the tab-separated output of EnumerateTargets, writes COUNT randomly chosen lines to
standard output (all of them, if COUNT is 0 or exceeds the number available). The choice depends
only on SEED and SALT, so re-running the experiment with the same seed re-runs the same targets.
SALT is normally the repository name, so that one seed gives every repository a different sample.
"""

import random
import sys


def main() -> None:
    path, count, seed, salt = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
    with open(path, encoding="utf-8") as f:
        lines = [line.rstrip("\n") for line in f if line.strip()]
    if 0 < count < len(lines):
        lines = random.Random(f"{seed}:{salt}").sample(lines, count)
    print("\n".join(lines))


if __name__ == "__main__":
    main()
