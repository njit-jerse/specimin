#!/bin/sh

# This script runs javac on all of the expected test outputs under src/test/resources.
# It returns 1 if any of them fail to compile, 2 if there are any malformed test directories,
# and 0 if all of them do compile.
#
# It is desirable that all of the expected test outputs compile, because Specimin
# should produce independently-compilable programs.

returnval=0

cd src/test/resources || exit 1
for testcase in * ; do
    sh ../../../typecheck_one_test.sh "${testcase}"
    # update overall return value
    test_retval=$?
    if [ ! "${test_retval}" = 0 ]; then
      returnval=1
    fi
done

if [ "${returnval}" = 0 ]; then
  echo "All expected test outputs compiled successfully."
elif [ "${returnval}" = 1 ]; then
  echo "Some expected test outputs do not compile successfully. See the above error output for details."
fi

exit ${returnval}
