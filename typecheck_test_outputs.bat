@echo off

rem This script runs javac on all of the expected test outputs under src/test/resources.
rem It returns 2 if any of them fail to compile, 1 if there are any malformed test directories,
rem and 0 if all of them do compile.

rem It is desirable that all of the expected test outputs compile, because Specimin
rem should produce independently-compilable programs.

setlocal enabledelayedexpansion
set returnval=0

set "current_directory=%cd%"

cd src\test\resources || exit /b 1

for /d %%t in (*) do (
  call %current_directory%\typecheck_one_test.bat %%t
  set test_retval=!errorlevel!
  rem update overall return value
  if !test_retval! NEQ 0 set returnval=1
)

if !returnval!==0 (
  echo All expected test outputs compiled successfully.
) else (
  if !returnval!==1 (
    echo Some expected test outputs do not compile successfully. See the above error output for details.
  )
)

exit /b !returnval!
endlocal