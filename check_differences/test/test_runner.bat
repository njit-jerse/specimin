@echo off

rem Test script to check if check_differences.bat outputs the correct exit code under different file conditions.
rem We do not need to explicitly test Java code since running ./gradlew test already verifies that
rem check_differences.bat correctly handles all Specimin test cases. These test cases are to verify that
rem the script works as expected under conditions not tested in Specimin.

setlocal enabledelayedexpansion

set failed=0

for /d %%t in (*) do (
    call :handleCase %%t
    if !errorlevel!==1 (
        set failed=1
    )
)

if !failed!==0 (
    echo All test cases executed successfully.
    exit /b 0
) else (
    exit /b 1
)

endlocal

:handleCase
    setlocal enabledelayedexpansion
        rem Each test case folder is guaranteed to have a `base` folder to compare to. However, depending on the
        rem test, there are tests for exit codes 0, 1, or both.
        cd "%~1"
        for /d %%t in (*) do (
            if "%%t" NEQ "base" (
                @echo off
                rem Redirect to nul because we don't want any output messages
                call "../../check_differences.bat" "base" "%%t" > nul
                set exitcode=!errorlevel!
                rem "%%t" represents the folder name (i.e. base, 0, 1) since we have changed the directory
                rem to the test case directory
                if !exitcode! NEQ %%t (
                    echo Test case %~1/%%t failed
                    echo    Expected: %%t
                    echo    Actual: !exitcode!
                    echo.
                    cd ..
                    exit /b 1
                ) else (
                    echo Test case %~1/%%t succeeded
                    echo.
                )
            )
        )
        cd ..
    endlocal
exit /b 0