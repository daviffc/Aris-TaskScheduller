
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$logDirectory = Join-Path $projectRoot ".staging-logs"
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null

$logFile = Join-Path $logDirectory "staging-updater.log"

Start-Transcript -Path $logFile -Append | Out-Null

$registryImage = "ghcr.io/daviffc/aris-taskscheduller"
$stagingTag = "${registryImage}:staging"
$healthUrl = "http://127.0.0.1:8081/workflows"

$composeArgs = @(
    "compose",
    "--env-file", ".env.staging",
    "-f", "compose.staging.yml"
)

$previousImage = $null
$deploymentStarted = $false
$exitCode = 0

$originalImageOverride = $env:ARIS_STAGING_IMAGE

function Test-StagingEndpoint {
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $response = Invoke-WebRequest `
                -Uri $healthUrl `
                -UseBasicParsing `
                -TimeoutSec 5

            if ($response.StatusCode -eq 200) {
                return $true
            }
        }
        catch {
        }

        Start-Sleep -Seconds 2
    }

    return $false
}

try {
    Write-Output "Verificando Docker Desktop"

    docker info --format '{{.ServerVersion}}' | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Docker Desktop indisponivel"
    }

    Write-Output "Consultando imagem de homologacao no GHCR"

    $manifestJson = docker buildx imagetools inspect `
        $stagingTag `
        --format '{{json .Manifest}}'

    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel consultar o GHCR"
    }

    $manifest = $manifestJson | ConvertFrom-Json
    $digest = $manifest.digest

    if ($digest -notmatch '^sha256:[a-f0-9]{64}$') {
        throw "Digest da imagem invalido"
    }

    $targetImage = "${registryImage}@${digest}"

    Write-Output "Imagem publicada: $targetImage"

    $containerId = docker @composeArgs ps -a -q app

    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel consultar os containers"
    }

    $isRunning = $false

    if ($containerId) {
        $previousImage = docker inspect `
            --format '{{.Config.Image}}' `
            $containerId

        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel consultar a imagem atual"
        }

        $runningStatus = docker inspect `
            --format '{{.State.Running}}' `
            $containerId

        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel consultar o estado do container"
        }

        $isRunning = $runningStatus.Trim() -eq "true"
        $previousImage = $previousImage.Trim()
    }

    if ($isRunning -and $previousImage -eq $targetImage) {
        Write-Output "A homologacao ja utiliza a imagem mais recente"

        if (-not (Test-StagingEndpoint)) {
            throw "A imagem esta atualizada, mas a API nao responde"
        }

        Write-Output "Smoke test aprovado"
    }
    else {
        Write-Output "Nova versao identificada"
        Write-Output "Imagem anterior: $previousImage"

        $env:ARIS_STAGING_IMAGE = $targetImage

        Write-Output "Baixando imagem por digest"

        docker pull $targetImage

        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao baixar a imagem"
        }

        Write-Output "Atualizando homologacao"

        $deploymentStarted = $true

        docker @composeArgs up -d --wait --wait-timeout 120

        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao iniciar a nova versao"
        }

        Write-Output "Executando smoke test"

        if (-not (Test-StagingEndpoint)) {
            throw "Smoke test da nova versao falhou"
        }

        Write-Output "Smoke test aprovado"
        Write-Output "Deploy concluido: $targetImage"
    }
}
catch {
    $exitCode = 1

    Write-Output "ERRO: $($_.Exception.Message)"

    if ($deploymentStarted -and $previousImage) {
        Write-Output "Tentando restaurar a imagem anterior"

        $env:ARIS_STAGING_IMAGE = $previousImage

        docker @composeArgs up -d --wait --wait-timeout 120

        if ($LASTEXITCODE -eq 0) {
            if (Test-StagingEndpoint) {
                Write-Output "Versao anterior restaurada"
            }
            else {
                Write-Output "Rollback iniciado, mas a API nao respondeu"
            }
        }
        else {
            Write-Output "Falha ao restaurar a imagem anterior"
        }
    }
}
finally {
    $env:ARIS_STAGING_IMAGE = $originalImageOverride

    Write-Output "Verificacao encerrada"
    Stop-Transcript | Out-Null
}

exit $exitCode
