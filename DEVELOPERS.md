## Developer documentation

This document contains information that might be useful if you want to
contribute to Specimin. We welcome improvements, bug fixes, new test
cases, and any other contributions that you'd like to make. To suggest
a change to Specimin, please open a [GitHub pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request).

### Testing infrastructure

Even Specimin's "unit tests" are actually full system tests: each
runs Specimin on a (small) input program and checks that it produces
an expected output program; note that the test cases themselves are
also Java programs (this sometimes confuses IDEs).

Each test case has a name (e.g., "onefilesimple") and three parts:
* the test runner, which is a JUnit test stored under
`src/test/java/org/checkerframework/specimin`. The name of a new
test runner should always be the test name in CamelCase plus the word
"Test" (e.g., `OneFileSimpleTest.java`).
* the test input. This is the program that Specimin should minimize.
It is stored under `src/test/resources/$testname/input`, where `$testname`
is the name of the test. Note that this must be a Java program,
and therefore must also respect the usual Java conventions - for example,
if the test input is in a package, there must be a corresponding file structure
here.
* the expected test output. It is stored in `src/test/resources/$testname/expected`.
It must be an independently-compilable Java program; our CI system checks this requirement
by running the `./gradlew expectedTestOutputsMustCompile` command.

You can run all of the tests via `./gradlew test`. When debugging a specific
test, I usually use a command like this one (replacing `MethodRef2Test` with the
name of the test runner for the test of interest):
```
./gradlew test -PskipCheckerFramework --tests "MethodRef2Test"
```

The `-PskipCheckerFramework` is useful to keep the edit-test-debug cycle
short: re-running the Checker Framework to test each small code change
is too time-consuming.

### Continuous Integration

To pass a CI build, a PR must meet the following requirements:
* the code must be properly formatted; `./gradlew spotlessJava` should
pass with no errors. You can fix formatting errors by running
`./gradlew spotlessApply` and then committing the result. Watch out
for the formatter adding ugly line breaks in code comments!
* the code must typecheck with the Checker Framework's Nullness,
Resource Leak, Interning, and Signature checkers. See
[the Checker Framework manual](https://checkerframework.org/manual/)
for more information about these checkers and how to use them.
* the code must not trigger any warnings from [ErrorProne](https://errorprone.info/)
* all expected test outputs must compile
* all tests must pass
* the [specimin-evaluation integration tests](https://github.com/njit-jerse/specimin-evaluation)
must have the expected outcomes (which may not always be passing). More on this below.

### Integration Tests

Specimin's integration tests come from a set of 20 historical typechecker
bugs from the javac and Checker Framework issue trackers. Ideally,
Specimin would reproduce the typechecker's output on all 20 of these bugs.
However, due to Specimin's approximation, that isn't true for all of the historical
bugs. The repo therefore has a set of files that indicate the expected status for
each of these bugs along three axes:
* `./src/main/resources/target_status.json` records which bugs do and do not
trigger a crash in Specimin
* `./src/main/resources/min_program_compile_status.json` records for which bugs
Specimin produces an independently-compilable program
* `./src/main/resources/min_program_compile_status.json` records for which bugs
Specimin successfully preserves the typechecker's behavior

If you fix a bug in Specimin, and CI fails because one of these changes from
"FAIL" to "PASS", then congratulations---the bug you fixed also fixed that
integration test! Just change the appropriate `.json` file(s) to record the new
status and re-run CI.

If you make a PR and one of these changes from "PASS" to "FAIL", you won't be
able to merge the PR until that integration test is fixed. You can reproduce the
failure locally by checking out the [specimin-evaluation repo](https://github.com/njit-jerse/specimin-evaluation)
and running a command like this:
```
export SPECIMIN=/path/to/your/copy/of/specimin/with/your/changes
python3 main.py --debug cf-691
```

(Replace "cf-691" above with the name of the failing integration test.)
