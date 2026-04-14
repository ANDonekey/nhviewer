param(
    [int]$MaxGalleryPages = 40,
    [int]$PerPage = 25,
    [int]$DelayMs = 2300,
    [string]$OutputPath = "app/src/main/assets/tag_catalog_seed.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-NhApi {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [object]$Body = $null
    )

    $args = @{
        Uri = $Uri
        Method = $Method
        TimeoutSec = 30
        UseBasicParsing = $true
    }
    if ($Body -ne $null) {
        $args["ContentType"] = "application/json"
        $args["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }
    $maxAttempts = 5
    $attempt = 1
    while ($true) {
        try {
            $resp = Invoke-WebRequest @args
            Start-Sleep -Milliseconds $DelayMs
            return ($resp.Content | ConvertFrom-Json)
        } catch {
            $isTooMany = $_.Exception.Message -like "*429*"
            if (-not $isTooMany -or $attempt -ge $maxAttempts) {
                throw
            }
            $waitSeconds = 65
            Write-Host "  hit 429, wait ${waitSeconds}s and retry (attempt $attempt/$maxAttempts): $Uri"
            Start-Sleep -Seconds $waitSeconds
            $attempt += 1
        }
    }
}

function Add-Tag {
    param(
        [hashtable]$Map,
        [object]$Tag
    )
    if ($null -eq $Tag.id) { return }
    $id = [int64]$Tag.id
    $type = [string]$Tag.type
    $name = [string]$Tag.name
    $slug = [string]$Tag.slug
    $count = 0
    if ($null -ne $Tag.count) {
        $count = [int]$Tag.count
    }
    $Map[$id] = [ordered]@{
        id = $id
        type = $type
        name = $name
        slug = $slug
        name_zh = $null
        count = $count
    }
}

$types = @("artist", "category", "character", "group", "language", "parody", "tag")
$tagMap = @{}

Write-Host "Step1: pull typed tags from /api/v2/tags/search ..."
foreach ($type in $types) {
    try {
        $result = Invoke-NhApi -Uri "https://nhentai.net/api/v2/tags/search" -Method "POST" -Body @{
            type = $type
            query = ""
            limit = 200
        }
        foreach ($tag in $result) {
            Add-Tag -Map $tagMap -Tag $tag
        }
        Write-Host "  $type => $($result.Count)"
    } catch {
        Write-Host "  $type => failed: $($_.Exception.Message)"
    }
}

Write-Host "Step2: crawl gallery pages to collect tag_ids ..."
$allTagIds = New-Object System.Collections.Generic.HashSet[long]
for ($page = 1; $page -le $MaxGalleryPages; $page++) {
    try {
        $result = Invoke-NhApi -Uri "https://nhentai.net/api/v2/galleries?page=$page&per_page=$PerPage"
        if ($null -eq $result.result -or $result.result.Count -eq 0) {
            break
        }
        foreach ($gallery in $result.result) {
            if ($null -eq $gallery.tag_ids) { continue }
            foreach ($id in $gallery.tag_ids) {
                [void]$allTagIds.Add([int64]$id)
            }
        }
        Write-Host "  page $page => galleries=$($result.result.Count), uniqueTagIds=$($allTagIds.Count)"
    } catch {
        Write-Host "  page $page failed: $($_.Exception.Message)"
        break
    }
}

Write-Host "Step3: resolve ids via /api/v2/tags/ids ..."
$ids = @($allTagIds)
$chunkSize = 50
for ($i = 0; $i -lt $ids.Count; $i += $chunkSize) {
    $chunk = $ids[$i..([Math]::Min($i + $chunkSize - 1, $ids.Count - 1))]
    $csv = ($chunk -join ",")
    try {
        $result = Invoke-NhApi -Uri "https://nhentai.net/api/v2/tags/ids?ids=$csv"
        foreach ($tag in $result) {
            Add-Tag -Map $tagMap -Tag $tag
        }
        $end = [Math]::Min($i + $chunkSize, $ids.Count)
        Write-Host "  ids $i-$end => resolved=$($result.Count), totalTags=$($tagMap.Count)"
    } catch {
        Write-Host "  ids chunk failed: $($_.Exception.Message)"
    }
}

$outputDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$list = $tagMap.Values | Sort-Object -Property type, name
$json = $list | ConvertTo-Json -Depth 10
Set-Content -Path $OutputPath -Value $json -Encoding UTF8

Write-Host "Done. wrote $($list.Count) tags => $OutputPath"
