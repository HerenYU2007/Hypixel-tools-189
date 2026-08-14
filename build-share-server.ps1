param(
    [string]$OutputJarPath = ''
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = Join-Path $env:APPDATA '.minecraft\runtime\java-runtime-beta\bin'
$javac = Join-Path $javaHome 'javac.exe'
$jar = Join-Path $javaHome 'jar.exe'

if (!(Test-Path $javac)) {
    throw "javac.exe was not found at $javac"
}
if (!(Test-Path $jar)) {
    throw "jar.exe was not found at $jar"
}

$buildDir = Join-Path $root 'build\share-server'
$classesDir = Join-Path $buildDir 'classes'
$manifest = Join-Path $buildDir 'MANIFEST.MF'
$sourcesList = Join-Path $buildDir 'sources.txt'
$outputDir = Join-Path $root 'outputs'
$defaultOutputJar = Join-Path $outputDir 'fireball-share-server.jar'
$outputJar = if ([string]::IsNullOrWhiteSpace($OutputJarPath)) {
    $defaultOutputJar
} elseif ([IO.Path]::IsPathRooted($OutputJarPath)) {
    $OutputJarPath
} else {
    Join-Path $root $OutputJarPath
}
$outputDir = Split-Path -Parent $outputJar

if (Test-Path $buildDir) {
    Remove-Item -LiteralPath $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

Get-ChildItem -Path (Join-Path $root 'src\share_server\java') -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName } |
        Set-Content -LiteralPath $sourcesList -Encoding ASCII

& $javac -source 1.8 -target 1.8 -encoding UTF-8 `
    -d $classesDir `
    "@$sourcesList"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

@'
Manifest-Version: 1.0
Main-Class: com.example.fireballpredictor.share.ShareServer

'@ | Set-Content -LiteralPath $manifest -Encoding ASCII

if (Test-Path $outputJar) {
    Remove-Item -LiteralPath $outputJar -Force
}

& $jar cfm $outputJar $manifest -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "Built $outputJar"
