# A/B compilability experiment

This directory holds a harness for answering one question about a risky change to Specimin:

> Does this change make Specimin's output stop compiling for any target where it used to compile?

Specimin's output is allowed to be imprecise, but it is not allowed to fail to compile, so a
target that goes from *compiles* to *does not compile* is a regression and should become a test
case. The harness runs two builds of Specimin over the methods and fields of real GitHub
projects, compiles both outputs with `javac`, and reports the transition matrix.

Nothing here runs in CI. It is a thing you run by hand when you want evidence about a change.

## Running it

Build a jar from each side of the change. The `jar` task already produces a fat jar, so each
one is self-contained:

```sh
git clone --no-hardlinks . /tmp/specimin-base && cd /tmp/specimin-base
git checkout <the commit the branch is based on>
./gradlew jar -PskipCheckerFramework      # -> build/libs/specimin.jar
```

Repeat for the branch under test, then:

```sh
sh experiments/run_experiment.sh \
  --base-jar /tmp/specimin-base/build/libs/specimin.jar \
  --new-jar  /tmp/specimin-new/build/libs/specimin.jar \
  --workdir  /tmp/ab-experiment \
  --per-repo 150 --seed 20260826 --jobs 6 --timeout 180 \
  --javac /Library/Java/JavaVirtualMachines/jdk-21.0.2.jdk/Contents/Home/bin/javac
```

`--javac` is worth pointing at a newer JDK than the one Specimin itself runs under: Specimin
must run on the JDK `build.gradle` pins (17), because the running JDK is an input to Specimin,
but the *output* should be compiled by something new enough to accept whatever syntax the input
project used. `--java` defaults to whatever `java` is on the path and must be that pinned JDK.

Run `run_experiment.sh` with no arguments to see the rest of the options. The interesting ones
are `--per-repo 0`, which runs every target in every repository rather than a sample, and
`--keep-all-output`, which keeps every emitted source tree instead of only the ones where the
two builds disagreed.

Re-running with the same `--seed` and the same `repos.csv` runs exactly the same targets.
Cloning and enumeration are cached in the work directory, so a re-run with a different seed
only pays for the Specimin invocations.

## What comes out

* `WORKDIR/summary.txt` — the transition matrix, per-repository breakdown, crash counts, and
  timing; also printed to standard output.
* `WORKDIR/results.csv` — one row per target: the two builds' exit status, whether their output
  compiled, how many files each emitted, and how long each took.
* `WORKDIR/out/{base,new}/ID.specimin.log` and `ID.javac.log` — Specimin's stderr and javac's
  diagnostics for every target.
* `WORKDIR/out/{base,new}/ID/` — the emitted programs, kept for the targets where the two
  builds disagreed. These are the reproducers: each one pairs with a `--targetMethod` line in
  `results.csv` and can be re-run by hand.

## The pieces

| File | What it does |
| --- | --- |
| `repos.csv` | The projects to run on: name, clone URL, pinned commit, source root. Add a line to add a project. |
| `EnumerateTargets.java` | Lists every targetable method, constructor and field of a source tree in `--targetMethod`/`--targetField` format. Run directly: `java -cp specimin.jar EnumerateTargets.java SRCROOT`. |
| `sample_targets.py` | Picks a deterministic random sample of the enumerated targets. |
| `run_experiment.sh` | The driver: clones, enumerates, samples, runs both builds on every target in parallel, summarizes. |
| `run_one_target.sh` | One target: runs both builds, compiles both outputs, writes one CSV row. |
| `summarize.py` | Turns `results.csv` into the summary. Re-runnable on its own. |

## Choosing repositories

`EnumerateTargets` skips members Specimin cannot target anyway — those in local classes,
anonymous classes and enum-constant bodies, and those in a top-level type whose name does not
match its file, since targeting those needs `--disable-root-validation`.

Two properties make a project a good choice. It should build from a single source root, because
`--root` takes one directory and Specimin resolves what it can from source under it. And it
should be Java rather than Kotlin or Scala, and not have a heavily generated source tree, since
generated code is not representative of what Specimin is aimed at.

The harness deliberately does *not* pass `--jarPath`. Every dependency outside the JDK is
therefore an unsolved symbol that Specimin has to synthesize, which is the code most changes to
type inference touch. It also means the absolute compile rate is lower than a user with a full
classpath would see — that is fine, because the measurement of interest is the difference
between the two builds, and both builds see identical inputs.
