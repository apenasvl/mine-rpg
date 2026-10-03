$ErrorActionPreference = 'Continue'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$logPath = Join-Path $projectRoot 'build-forge.log'
"RPG Stats Forge 1.20.1 - $(Get-Date -Format o)" | Out-File -FilePath $logPath -Encoding utf8
"Pasta: $projectRoot" | Tee-Object -FilePath $logPath -Append
try {
    if ($env:JAVA_HOME) {
        $javaExe = Join-Path ($env:JAVA_HOME.Trim('"')) 'bin\java.exe'
    } else {
        $javaExe = (Get-Command java.exe -ErrorAction Stop).Source
    }
    $javaOutput = & $javaExe -version 2>&1
    $javaExit = $LASTEXITCODE
    $javaOutput | Tee-Object -FilePath $logPath -Append
    if ($javaExit -ne 0) { throw 'Java nao iniciou. Confira JAVA_HOME.' }
    if (($javaOutput -join ' ') -notmatch 'version "17\.') {
        throw 'Use JDK 17 para compilar este projeto. Confira JAVA_HOME e PATH.'
    }
    'Compilando. A primeira execucao baixa dependencias e pode demorar.' |
        Tee-Object -FilePath $logPath -Append
    & cmd.exe /d /c 'gradlew.bat --console=plain build' 2>&1 |
        Tee-Object -FilePath $logPath -Append
    $buildExit = $LASTEXITCODE
    if ($buildExit -ne 0) {
        "Falha no Gradle (codigo $buildExit). Log: $logPath" | Tee-Object -FilePath $logPath -Append
        exit $buildExit
    }
    $jarPath = Join-Path $projectRoot 'build\libs\rpgstats-forge-2.3.0-forge-port-alpha.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw 'Gradle terminou, mas o JAR esperado nao foi encontrado em build\libs.'
    }
    "BUILD CONCLUIDO. Instale este arquivo: $jarPath" | Tee-Object -FilePath $logPath -Append
    exit 0
} catch {
    "ERRO: $_" | Tee-Object -FilePath $logPath -Append
    exit 1
}
