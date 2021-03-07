@echo off
if exist "..\target\HeistMC-%1.jar" (
    copy "..\target\HeistMC-%1.jar" "..\release"
    if exist "..\release\HeistMC-latest.jar" (
        del "..\release\HeistMC-latest.jar"
    )
    rename "..\release\HeistMC-%1.jar" "HeistMC-latest.jar"
    echo Successfully prepared release JAR.
) else (
    echo No version '%1' was found.
)