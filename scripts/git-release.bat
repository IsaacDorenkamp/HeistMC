@echo off

set /p VERSION=<"C:\Users\quant\eclipse-workspace\HeistMC\VERSION.txt"

git add .
git commit -m Bump to version %VERSION%
git tag -a %VERSION% Bump to version %VERSION%
git push origin %VERSION%