@echo off
cd /d "%~dp0"
set JAVA_HOME=D:\Java\jdk-21
call D:\GRADLE\bin\gradle.bat build --no-daemon --rerun-tasks
pause
