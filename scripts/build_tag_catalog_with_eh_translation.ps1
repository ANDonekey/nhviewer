param(
    [int]$MaxGalleryPages = 40,
    [int]$PerPage = 25,
    [int]$DelayMs = 2300,
    [string]$SeedPath = "app/src/main/assets/tag_catalog_seed.json",
    [string]$PlainTranslationPath = "app/src/main/assets/tag_translation_plain_zh_cn.json",
    [string]$EhTranslationUrl = "https://raw.githubusercontent.com/xiaojieonly/EhTagTranslation/main/tag-translations/tag-translations-zh-rCN.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Step1: build seed from nhentai API"
& powershell.exe -File "scripts/build_tag_catalog_seed.ps1" `
    -MaxGalleryPages $MaxGalleryPages `
    -PerPage $PerPage `
    -DelayMs $DelayMs `
    -OutputPath $SeedPath

Write-Host "Step2: apply EH Chinese translations to name_zh"
python "scripts/apply_eh_tag_translations.py" `
    --seed $SeedPath `
    --url $EhTranslationUrl `
    --plain-output $PlainTranslationPath

Write-Host "Step3: rebuild prebuilt sqlite"
python "scripts/build_prebuilt_db.py"

Write-Host "Done."
