@echo off
rem compile everything and run the checks (windows)
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out -sourcepath "src\main\java;src\test\java" src\test\java\tokentally\TestRunner.java
if errorlevel 1 exit /b 1
java -cp out tokentally.TestRunner data
