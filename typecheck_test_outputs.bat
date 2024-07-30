@echo off

rem This script runs javac on all of the expected test outputs under src/test/resources.
rem It returns 2 if any of them fail to compile, 1 if there are any malformed test directories,
rem and 0 if all of them do compile.

rem It is desirable that all of the expected test outputs compile, because Specimin
rem should produce independently-compilable programs.

setlocal enabledelayedexpansion
set returnval=0

cd src\test\resources || exit /b 1

for /d %%t in (*) do (
  set continue=0
  if "%%t"=="shared" set continue=1
  rem https://bugs.openjdk.org/browse/JDK-8319461 wasn't actually fixed (this test is based on that bug)
  if "%%t"=="superinterfaceextends" set continue=1
  rem incomplete handling of method references: https://github.com/njit-jerse/specimin/issues/291
  rem this test exists to check that no crash occurs, not that Specimin produces the correct output
  if "%%t"=="methodref2" set continue=1
  rem this test will not compile right now; this is a TODO in UnsolvedSymbolVisitor#lookupTypeArgumentFQN
  if "%%t"=="methodreturnfullyqualifiedgeneric" set continue=1
  if !continue!==0 (
    cd "%%t/expected/" || exit 1
    rem javac relies on word splitting
    rem shellcheck disable=SC2046
    set JAVA_FILES=
    for /r %%F in (*.java) do (
      set "JAVA_FILES=!JAVA_FILES! %%F"
    )
    javac -classpath "../../shared/checker-qual-3.42.0.jar" !JAVA_FILES!
    if errorlevel 1 (
        echo Running javac on %%F resulted in one or more errors, which are printed above.
        set returnval=2
    )
    cd ../.. || exit 1
  )
)

if !returnval!==0 (
  echo All expected test outputs compiled successfully.
) else (
  if !returnval!==2 (
    echo Some expected test outputs do not compile successfully. See the above error output for details.
  )
)

for /r %%F in (*.class) do (
    del "%%F"
)

exit /b !returnval!
endlocal