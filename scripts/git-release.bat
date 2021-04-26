@echo off

set /p VERSION=<"C:\Users\quant\eclipse-workspace\HeistMC\VERSION.txt"

cd ..
git add .
git commit -m "Bump to version %VERSION%"
git tag -a v%VERSION% -m "Bump to version %VERSION%"
git push origin v%VERSION%