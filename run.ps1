$m2 = Join-Path $env:USERPROFILE ".m2\repository"
$jars = Get-ChildItem $m2 -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch "sources|javadoc" }
$cp = ".\target\classes;" + (($jars | ForEach-Object { $_.FullName }) -join ";")
$srcFiles = Get-ChildItem ".\src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

Write-Host "=== Compiling $($srcFiles.Count) source files ==="
& ".\src\oracleJdk-25\bin\javac.exe" --release 17 -d ".\target\classes" -cp $cp @srcFiles
if ($LASTEXITCODE -ne 0) { Write-Host "COMPILE FAILED"; exit 1 }
Write-Host "=== Compilation successful ==="

Write-Host "=== Running fencing planner ==="
& ".\src\oracleJdk-25\bin\java.exe" -cp $cp com.fencingplanner.App
Write-Host "=== Done (exit code: $LASTEXITCODE) ==="
