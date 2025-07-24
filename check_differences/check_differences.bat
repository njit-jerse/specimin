@echo off
rem On Windows, instead of using the diff command for SpeciminTestExecutor, we can use this
rem command instead since diff is not included by default. This script outputs 0 if no differences
rem are found, 1 if a difference in file structure or file content (all whitespace removed) is found

goto :MAIN

rem Function to remove all whitespace from a file
:RemoveWhitespace
    setlocal enabledelayedexpansion
    set "content="
    rem Simply calling (%~1) is not enough; in edge cases, empty lines containing delimiters (i.e. ;)
    rem may be removed. Therefore, we need findstr to add line numbers so no line is empty.
    for /F "tokens=* delims=" %%i in ('findstr /n "^" "%~1"') do (
        set "line=%%i"
        set "line=!line:*:=!"
        if "!line!" NEQ "" set "line=!line: =!"
        set "content=!content!!line!"
    )
    echo !content! > "%~2"
    endlocal
    exit /b 0

:MAIN

setlocal enabledelayedexpansion

if "%1"=="" (
    echo No path provided.
    exit /b 1
)
if "%2"=="" (
    echo Second path not provided.
    exit /b 1
)

set "CURRENT_DIRECTORY=%cd%"

rem use /D to handle drive differences (C:\ vs. D:\)
cd /D "%1" || exit /b 1
set "DIRECTORY_1=%cd%"

cd /D "%CURRENT_DIRECTORY%"

cd /D "%2" || exit /b 1
set "DIRECTORY_2=%cd%"

cd /D "%DIRECTORY_1%"
set DIR_1_STRUCTURE=
for /r %%i in (*) do (
    rem Convert absolute to relative path
    set "ABSOLUTE_PATH=%%i"
    set "RELATIVE_PATH=!ABSOLUTE_PATH:%DIRECTORY_1%\=!"
    rem Add each file path to the DIR_1_STRUCTURE list of files
    set "DIR_1_STRUCTURE=!DIR_1_STRUCTURE!;!RELATIVE_PATH!"
)

cd /D "%DIRECTORY_2%"
set DIR_2_STRUCTURE=
for /r %%i in (*) do (
    rem Convert absolute to relative path
    set "ABSOLUTE_PATH=%%i"
    set "RELATIVE_PATH=!ABSOLUTE_PATH:%DIRECTORY_2%\=!"
    rem Add each file path to the DIR_2_STRUCTURE list of files
    set "DIR_2_STRUCTURE=!DIR_2_STRUCTURE!;!RELATIVE_PATH!"
)

if "!DIR_1_STRUCTURE!" NEQ "!DIR_2_STRUCTURE!" (
    echo %DIRECTORY_1% and %DIRECTORY_2% have different directory structures
    endlocal
    exit /b 1
)

rem At this point, we have guaranteed that the files are the same, so we only
rem need to iterate through one of the lists.
for %%f in (!DIR_1_STRUCTURE!) do (
    set "FILE_1=!DIRECTORY_1!\%%f"
    set "FILE_2=!DIRECTORY_2!\%%f"
    call :RemoveWhitespace "!FILE_1!" "!FILE_1!.tmp"
    call :RemoveWhitespace "!FILE_2!" "!FILE_2!.tmp"

    set fail=0
    rem set fail=1 will only be called if fc exits with exit code 1 (differences)
    fc "!FILE_1!.tmp" "!FILE_2!.tmp" > nul || set fail=1

    del "!FILE_1!.tmp"
    del "!FILE_2!.tmp"
    if !fail!==1 (
        echo "!FILE_1!" and "!FILE_2!" are different
        goto :differences_found
    )
)

endlocal
exit /b 0

:differences_found
endlocal
exit /b 1
