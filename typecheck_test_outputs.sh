#!/bin/sh

# This script runs javac on all of the expected test outputs under src/test/resources.
# It returns 1 if any of them fail to compile, and zero if all of them do compile.
# It is desirable that all of the expected test outputs compile, because Specimin
# should produce independently-compilable programs.

returnval=0

cd src/test/resources || exit 1
for testcase in * ; do
    cd "${testcase}/expected/" || exit 1
    javaccmd="javac -proc:only -nowarn $(find . -name "*.java")"
    # javac relies on word splitting
    # shellcheck disable=SC2046
    javac -proc:only -nowarn $(find . -name "*.java") \
      || echo "${testcase}/expected issues one or more errors, which are printed above." ; returnval=2
    cd ../.. || exit 1
done

if [ "${returnval}" == 0 ]; then
  echo "All expected test outputs compiled successfully."
elif [ "${returnval}" == 2 ]; then
  echo "Some expected test outputs do not compile successfully. See the above error output for details."
fi

exit ${returnval}