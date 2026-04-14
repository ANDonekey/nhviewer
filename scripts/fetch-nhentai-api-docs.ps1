param(
    [string]$BaseUrl = "https://nhentai.net",
    [string]$OutDir = "docs/external"
)

$ErrorActionPreference = "Stop"
$headers = @{
    "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
}

New-Item -ItemType Directory -Force $OutDir | Out-Null

$docsUrl = "$BaseUrl/api/v2/docs#/"
$openApiUrl = "$BaseUrl/api/v2/openapi.json"

$docsResponse = Invoke-WebRequest -Uri $docsUrl -Headers $headers -UseBasicParsing
$openApiResponse = Invoke-WebRequest -Uri $openApiUrl -Headers $headers -UseBasicParsing

$docsPath = Join-Path $OutDir "nhentai-api-v2-docs.html"
$openApiPath = Join-Path $OutDir "nhentai-api-v2-openapi.json"
$snapshotPath = Join-Path $OutDir "nhentai-api-v2-snapshot.md"

$docsContent = $docsResponse.Content
# Rewrite upstream absolute OpenAPI URL for local snapshot browsing.
$docsContent = $docsContent -replace "url:\s*'/api/v2/openapi\.json'", "url: './nhentai-api-v2-openapi.json'"
$docsContent | Set-Content -LiteralPath $docsPath -Encoding UTF8
$openApiResponse.Content | Set-Content -LiteralPath $openApiPath -Encoding UTF8

$openApiHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $openApiPath).Hash
$now = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"

@"
# nhentai API v2 Snapshot

- Source docs: $docsUrl
- OpenAPI URL: $openApiUrl
- Fetched at: $now
- OpenAPI SHA256: $openApiHash
"@ | Set-Content -LiteralPath $snapshotPath -Encoding UTF8

Write-Host "Saved: $docsPath"
Write-Host "Saved: $openApiPath"
Write-Host "Saved: $snapshotPath"
