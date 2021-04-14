@echo off

echo Configuring path...
set TEMP_PATH=%PATH%
path C:\Program Files\Java\jdk1.8.0_271/bin;%PATH%

rem TODO - detect correct version!

echo Copying plugin JAR...
copy "C:\Users\quant\eclipse-workspace\HeistMC\target\HeistMC-1.0.0.jar" .
rename HeistMC-1.0.0.jar HeistMC.jar

echo Extracting AnvilGUI JAR...
jar xf ../lib/anvilgui-1.4.0-SNAPSHOT.jar

echo Cleaning up META-INF...
rmdir /s /q META-INF

echo Injecting AnvilGUI into HeistMC.jar...
jar uf HeistMC.jar net

rem The target directory may be changed, but it is fixed here for convenience.
echo Installing HeistMC.jar...
copy HeistMC.jar "C:\Users\quant\Desktop\stuff\MCHeist_testing\plugins"

echo Cleaning up temporary resources...
del HeistMC.jar
rmdir /s /q net

echo Restoring original path...
path %TEMP_PATH%