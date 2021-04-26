@echo off

set /p VERSION=<"C:\Users\quant\eclipse-workspace\HeistMC\VERSION.txt"

if exist "..\target\HeistMC-%VERSION%.jar" (
    copy "..\target\HeistMC-%VERSION%.jar" "..\release"
    if exist "..\release\HeistMC-latest.jar" (
        del "..\release\HeistMC-latest.jar"
    )
    rename "..\release\HeistMC-%VERSION%.jar" "HeistMC-latest.jar"
    echo Successfully prepared release JAR.
) else (
    echo No version '%1' was found.
)