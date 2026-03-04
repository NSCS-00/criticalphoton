@echo off
set JAVA_HOME=D:\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"
D:\GRADLE\bin\gradle.bat build --no-daemon
pause
