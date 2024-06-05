#!/bin/sh

# This script runs javac on all of the expected test outputs under src/test/resources.
# It returns 2 if any of them fail to compile, 1 if there are any malformed test directories,
# and 0 if all of them do compile.
#
# It is desirable that all of the expected test outputs compile, because Specimin
# should produce independently-compilable programs.

returnval=0

cd src/test/resources || exit 1
for testcase in * ; do
    if [ "${testcase}" = "shared" ]; then continue; fi
    # https://bugs.openjdk.org/browse/JDK-8319461 wasn't actually fixed (this test is based on that bug)
    if [ "${testcase}" = "superinterfaceextends" ]; then continue; fi
    # incomplete handling of method references: https://github.com/njit-jerse/specimin/issues/291
    # this test exists to check that no crash occurs, not that Specimin produces the correct output
    if [ "${testcase}" = "methodref2" ]; then continue; fi
    cd "${testcase}/expected/" || exit 1
    # javac relies on word splitting
    # shellcheck disable=SC2046
    javac -classpath "../../shared/checker-qual-3.42.0.jar" $(find . -name "*.java") \
      || { echo "Running javac on ${testcase}/expected issues one or more errors, which are printed above."; returnval=2; }
    cd ../.. || exit 1
done

if [ "${returnval}" = 0 ]; then
  echo "All expected test outputs compiled successfully."
elif [ "${returnval}" = 2 ]; then
  echo "Some expected test outputs do not compile successfully. See the above error output for details."
fi

find . -name "*.class" -exec rm {} \;

exit ${returnval}
