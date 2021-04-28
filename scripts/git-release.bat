@echo off

set /p VERSION=<"C:\Users\quant\eclipse-workspace\HeistMC\VERSION.txt"

rem prepare release JAR

if exist "..\target\HeistMC-%VERSION%.jar" (
    copy "..\target\HeistMC-%VERSION%.jar" "..\release"
    if exist "..\release\HeistMC-latest.jar" (
        del "..\release\HeistMC-latest.jar"
    )
    rename "..\release\HeistMC-%VERSION%.jar" "HeistMC-latest.jar"
    echo Successfully prepared release JAR.
) else (
    echo No version '%1' was found.
    goto:eof
)

rem commit, tag, and push

cd ..
git add .
git commit -m "Bump to version %VERSION%"
git tag -a v%VERSION% -m "Bump to version %VERSION%"
git push origin v%VERSION%
git push origin main