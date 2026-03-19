$jdk = "C:\Users\W61RO3Y\Downloads\GitClone\Turnierplanung\src\oracleJdk-25"
$repo = "C:\Users\W61RO3Y\.m2\repository"
$base = "C:\Users\W61RO3Y\Downloads\GitClone\Turnierplanung"
$outDir = "$base\target\classes"

$jars = @(
    "$repo\ai\timefold\solver\timefold-solver-core\1.26.0\timefold-solver-core-1.26.0.jar",
    "$repo\org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar",
    "$repo\org\slf4j\slf4j-simple\2.0.9\slf4j-simple-2.0.9.jar",
    "$repo\org\apache\poi\poi\5.2.3\poi-5.2.3.jar",
    "$repo\org\apache\poi\poi-ooxml\5.2.3\poi-ooxml-5.2.3.jar",
    "$repo\org\apache\poi\poi-ooxml-lite\5.2.3\poi-ooxml-lite-5.2.3.jar",
    "$repo\org\apache\xmlbeans\xmlbeans\5.1.1\xmlbeans-5.1.1.jar",
    "$repo\org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar",
    "$repo\org\apache\commons\commons-compress\1.21\commons-compress-1.21.jar",
    "$repo\commons-io\commons-io\2.11.0\commons-io-2.11.0.jar",
    "$repo\commons-codec\commons-codec\1.15\commons-codec-1.15.jar",
    "$repo\com\github\virtuald\curvesapi\1.07\curvesapi-1.07.jar",
    "$repo\com\zaxxer\SparseBitSet\1.2\SparseBitSet-1.2.jar",
    "$repo\org\apache\commons\commons-math3\3.6.1\commons-math3-3.6.1.jar",
    "$repo\org\apache\logging\log4j\log4j-api\2.18.0\log4j-api-2.18.0.jar",
    "$repo\jakarta\xml\bind\jakarta.xml.bind-api\4.0.2\jakarta.xml.bind-api-4.0.2.jar",
    "$repo\org\glassfish\jaxb\jaxb-runtime\4.0.5\jaxb-runtime-4.0.5.jar",
    "$repo\org\glassfish\jaxb\jaxb-core\4.0.5\jaxb-core-4.0.5.jar",
    "$repo\org\glassfish\jaxb\txw2\4.0.5\txw2-4.0.5.jar",
    "$repo\jakarta\activation\jakarta.activation-api\2.1.3\jakarta.activation-api-2.1.3.jar",
    "$repo\org\eclipse\angus\angus-activation\2.0.2\angus-activation-2.0.2.jar",
    "$repo\com\sun\istack\istack-commons-runtime\4.1.2\istack-commons-runtime-4.1.2.jar",
    "$repo\io\micrometer\micrometer-core\1.14.7\micrometer-core-1.14.7.jar",
    "$repo\io\micrometer\micrometer-commons\1.14.7\micrometer-commons-1.14.7.jar",
    "$repo\io\micrometer\micrometer-observation\1.14.7\micrometer-observation-1.14.7.jar",
    "$repo\org\hdrhistogram\HdrHistogram\2.2.2\HdrHistogram-2.2.2.jar",
    "$repo\org\latencyutils\LatencyUtils\2.0.3\LatencyUtils-2.0.3.jar"
)

$cp = ($jars -join ";")

# Compile
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
Copy-Item "$base\src\main\resources\*" $outDir -Force

$javaSrc = Get-ChildItem "$base\src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Host "Compiling $($javaSrc.Count) files..."
& "$jdk\bin\javac.exe" -d $outDir -cp $cp --release 17 @javaSrc 2>&1 | ForEach-Object { $_.ToString() } | Out-Host
if ($LASTEXITCODE -ne 0) { Write-Host "COMPILE FAILED"; exit 1 }
Write-Host "Compile OK"

# Run
Write-Host "`n=== Running App ===`n"
$runCp = "$outDir;$cp"
& "$jdk\bin\java.exe" -cp $runCp com.fencingplanner.App 2>&1 | ForEach-Object { $_.ToString() } | Out-Host
Write-Host "`nExit code: $LASTEXITCODE"
