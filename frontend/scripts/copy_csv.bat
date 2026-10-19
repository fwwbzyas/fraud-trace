@echo off
node "%~dp0copy_csv_files.js" "%~1"
exit /b %errorlevel%
