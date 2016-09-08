@echo off
setlocal ENABLEDELAYEDEXPANSION
call %1 %*
if %errorlevel% neq 0 exit /b %errorlevel%
set token=%EXPECTED_OUTPUTS%
:loop
if "!token!" EQU "" goto END
FOR /F "tokens=1* delims=;" %%a IN ("%token%") DO (
  echo EXPECTED_OUTPUT_%%a=!%%a!
  SET token=%%b
)
goto loop
:END
endlocal