@echo off

set /p VERSION=<"C:\Users\quant\eclipse-workspace\HeistMC\VERSION.txt"

cd ..
git add .
echo "Committing"
git commit -m "Bump to version %VERSION%"
echo "Tagging..."
git tag -a v%VERSION% "Bump to version %VERSION%"
echo "Pushing..."
git push origin v%VERSION%