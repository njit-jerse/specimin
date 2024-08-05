@echo off

rem This script runs javac on a single expected test output. If the test output
rem is compilable, it exits with code 0. If not, it exits with code 1. In all other
rem cases (e.g., if a cd command fails), it exits with code 2. It includes
rem logic for skipping test cases that don't need to compile, etc. It should be
rem run in the directory src/test/resources.

setlocal enabledelayedexpansion

set testcase=%1

if "%testcase%"=="shared" exit /b 0
rem https://bugs.openjdk.org/browse/JDK-8319461 wasn't actually fixed (this test is based on that bug)
if "%testcase%"=="superinterfaceextends" exit /b 0
rem incomplete handling of method references: https://github.com/njit-jerse/specimin/issues/291
rem this test exists to check that no crash occurs, not that Specimin produces the correct output
if "%testcase%"=="methodref2" exit /b 0
rem this test will not compile right now; this is a TODO in UnsolvedSymbolVisitor#lookupTypeArgumentFQN
if "%testcase%"=="methodreturnfullyqualifiedgeneric" exit /b 0

cd "%testcase%\expected\" || exit /b 2

set JAVA_FILES=
for /r %%F in (*.java) do (
  set "JAVA_FILES=!JAVA_FILES! %%F"
)

javac -classpath "../../shared/checker-qual-3.42.0.jar" !JAVA_FILES!
if errorlevel 1 (
  echo Running javac on %testcase% resulted in one or more errors, which are printed above.
  set returnval=1
)

rem clean up
for /r %%F in (*.class) do (
  del "%%F"
)
endlocal

exit /b 0