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

$buildDir = Join-Path $root 'build\noop-agent'
$classesDir = Join-Path $buildDir 'classes'
$manifest = Join-Path $buildDir 'MANIFEST.MF'
$sourceDir = Join-Path $root 'src\agent\java\com\example\fireballpredictor\agent'
$source = Join-Path $sourceDir 'FireballPredictorNoopAgent.java'
$logSource = Join-Path $sourceDir 'FireballPredictorAgentLog.java'
$outputDir = Join-Path $root 'outputs'
$outputJar = Join-Path $outputDir 'fireballpredictor-noop-agent.jar'

if (Test-Path $buildDir) {
    Remove-Item -LiteralPath $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir | Out-Null
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

& $javac -source 1.8 -target 1.8 -encoding UTF-8 -d $classesDir $source $logSource
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

@'
Manifest-Version: 1.0
Premain-Class: com.example.fireballpredictor.agent.FireballPredictorNoopAgent

'@ | Set-Content -LiteralPath $manifest -Encoding ASCII

if (Test-Path $outputJar) {
    Remove-Item -LiteralPath $outputJar -Force
}

& $jar cfm $outputJar $manifest -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "Built $outputJar"
