#!/bin/sh

## This script runs javac on a single expected test output. If the test output
## is compilable, it exits with code 0. If not, it exits with code 1. In all other
# cases (e.g., if a cd command fails), it exits with code 2. It includes
## logic for skipping test cases that don't need to compile, etc. It should be
## run in the directory src/test/resources.

testcase=$1

if [ "${testcase}" = "shared" ]; then exit 0; fi
# https://bugs.openjdk.org/browse/JDK-8319461 wasn't actually fixed (this test is based on that bug)
if [ "${testcase}" = "superinterfaceextends" ]; then exit 0; fi
# incomplete handling of method references: https://github.com/njit-jerse/specimin/issues/291
# this test exists to check that no crash occurs, not that Specimin produces the correct output
if [ "${testcase}" = "methodref2" ]; then exit 0; fi
# this test will not compile right now; this is a TODO in UnsolvedSymbolVisitor#lookupTypeArgumentFQN
if [ "${testcase}" = "methodreturnfullyqualifiedgeneric" ]; then exit 0; fi
cd "${testcase}/expected/" || exit 2
# javac relies on word splitting
# shellcheck disable=SC2046
javac -classpath "../../shared/checker-qual-3.42.0.jar" $(find . -name "*.java") \
  || { echo "Running javac on ${testcase}/expected issues one or more errors, which are printed above."; \
  find . -name "*.class" -exec rm {} \; ; exit 1; }

# clean up
find . -name "*.class" -exec rm {} \;

cd ../.. || exit 2
exit 0