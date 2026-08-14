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

$libraries = Join-Path $env:APPDATA '.minecraft\libraries'
$asm = Join-Path $libraries 'org\ow2\asm\asm\9.7.1\asm-9.7.1.jar'
$launchwrapper = Join-Path $libraries 'net\minecraft\launchwrapper\1.12\launchwrapper-1.12.jar'

if (!(Test-Path $asm)) {
    throw "ASM 9.7.1 was not found at $asm"
}
if (!(Test-Path $launchwrapper)) {
    throw "LaunchWrapper 1.12 was not found at $launchwrapper"
}

$buildDir = Join-Path $root 'build\agent'
$classesDir = Join-Path $buildDir 'classes'
$manifest = Join-Path $buildDir 'MANIFEST.MF'
$sourcesList = Join-Path $buildDir 'sources.txt'
$outputDir = Join-Path $root 'outputs'
$outputJar = Join-Path $outputDir 'fair-av-standalone-agent-1.8.9-fireball-gen-pro-esp.jar'

if (Test-Path $buildDir) {
    Remove-Item -LiteralPath $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir | Out-Null
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

Push-Location $classesDir
try {
    & $jar xf $asm
} finally {
    Pop-Location
}

Get-ChildItem -Path (Join-Path $root 'source\agent\java') -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName } |
        Set-Content -LiteralPath $sourcesList -Encoding ASCII

& $javac -source 1.8 -target 1.8 -encoding UTF-8 `
    -cp "$asm;$launchwrapper" `
    -d $classesDir `
    "@$sourcesList"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

$asmDir = Join-Path $classesDir 'org\objectweb\asm'
$relocatedAsmDir = Join-Path $classesDir 'fbp\objectweb\asm'
New-Item -ItemType Directory -Path (Split-Path -Parent $relocatedAsmDir) -Force | Out-Null
Move-Item -LiteralPath $asmDir -Destination $relocatedAsmDir
$emptyOrg = Join-Path $classesDir 'org'
if ((Test-Path -LiteralPath $emptyOrg) -and -not (Get-ChildItem -LiteralPath $emptyOrg -Force)) {
    Remove-Item -LiteralPath $emptyOrg -Recurse -Force
}

$classFiles = Get-ChildItem -Path $classesDir -Recurse -Filter '*.class'
$latin1 = [Text.Encoding]::GetEncoding('ISO-8859-1')
foreach ($classFile in $classFiles) {
    $bytes = [IO.File]::ReadAllBytes($classFile.FullName)
    $text = $latin1.GetString($bytes)
    $text = $text.Replace('org/objectweb/asm', 'fbp/objectweb/asm')
    $text = $text.Replace('org.objectweb.asm', 'fbp.objectweb.asm')
    [IO.File]::WriteAllBytes($classFile.FullName, $latin1.GetBytes($text))
}

@'
Manifest-Version: 1.0
Premain-Class: com.example.fireballpredictor.agent.FireballPredictorAgent
Can-Redefine-Classes: false
Can-Retransform-Classes: false
TweakClass: com.example.fireballpredictor.agent.FireballPredictorTweaker

'@ | Set-Content -LiteralPath $manifest -Encoding ASCII

if (Test-Path $outputJar) {
    Remove-Item -LiteralPath $outputJar -Force
}

& $jar cfm $outputJar $manifest -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "Built $outputJar"
