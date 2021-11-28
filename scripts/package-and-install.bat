@echo off

echo Configuring path...
set TEMP_PATH=%PATH%
path C:\Program Files\Java\jdk1.8.0_271/bin;%PATH%

rem TODO - detect correct version!

set /p VERSION=<"C:\Users\quant\eclipse-workspace\HeistMC\VERSION.txt"
echo Installing version %VERSION%...

set JAR_NAME=HeistMC-%VERSION%.jar
set TARGET=C:\Users\quant\eclipse-workspace\HeistMC\target\%JAR_NAME%

echo Copying plugin JAR...
copy %TARGET% .
rename %JAR_NAME% HeistMC.jar

rem echo Extracting AnvilGUI JAR...
rem jar xf ../lib/anvilgui-1.4.0-SNAPSHOT.jar

rem echo Cleaning up META-INF...
rem rmdir /s /q META-INF

rem echo Injecting AnvilGUI into HeistMC.jar...
rem jar uf HeistMC.jar net

rem The target directory may be changed, but it is fixed here for convenience.
echo Installing HeistMC.jar...
copy HeistMC.jar "C:\Users\quant\Desktop\stuff\MCHeist_testing\plugins"

echo Cleaning up temporary resources...
del HeistMC.jar
rem rmdir /s /q net

echo Restoring original path...
path %TEMP_PATH%