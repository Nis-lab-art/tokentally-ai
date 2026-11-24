@echo off
rem build the api (it compiles ..\core too) and start it on port 8080 (windows)
cd /d "%~dp0"
call mvn -q -DskipTests package
if errorlevel 1 exit /b 1
java -jar target\tokentally-api-1.0.jar
