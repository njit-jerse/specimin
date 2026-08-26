# Results of past runs

Kept so that a later run has something to compare against. Each pass is named for its seed and
its `--per-repo` value; re-running `run_experiment.sh` with the same seed, the same `repos.csv`
and the same two commits reproduces it.

| Pass | Targets | Base commit | New commit | Regressions | Improvements |
| --- | --- | --- | --- | --- | --- |
| `pass1-seed20260826-150per` | 1050 | c260d00e (main) | 2f8caccb (issue526, PR #536) | 1 | 2 |
| `pass2-seed777-100per` | 700 | c260d00e (main) | 2f8caccb (issue526, PR #536) | 0 | 2 |

The two samples overlap on 62 targets, so together they cover 1688 distinct ones.
`pass2` also carries the `base_hash`/`new_hash` columns, which `pass1` predates.
