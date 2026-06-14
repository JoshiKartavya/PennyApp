param(
    [string]$sourcePath = "C:\Users\joshi\.gemini\antigravity\brain\009ca194-418c-42a8-9c99-e8f0917d23ff\media__1779906252736.png",
    [string]$resDir = "c:\Users\joshi\AndroidStudioProjects\Penny\androidApp\src\main\res"
)

Add-Type -AssemblyName System.Drawing

$densities = @(
    @{ Name = "mdpi"; LegacySize = 48; ForegroundSize = 108; SafeSize = 72 },
    @{ Name = "hdpi"; LegacySize = 72; ForegroundSize = 162; SafeSize = 108 },
    @{ Name = "xhdpi"; LegacySize = 96; ForegroundSize = 216; SafeSize = 144 },
    @{ Name = "xxhdpi"; LegacySize = 144; ForegroundSize = 324; SafeSize = 216 },
    @{ Name = "xxxhdpi"; LegacySize = 192; ForegroundSize = 432; SafeSize = 288 }
)

Write-Host "Loading source image from $sourcePath..."
$srcImage = [System.Drawing.Image]::FromFile($sourcePath)

foreach ($d in $densities) {
    $dirName = "mipmap-" + $d.Name
    $targetDir = Join-Path $resDir $dirName
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }

    # 1. Generate Legacy ic_launcher.png (Centered logo on solid #0C0C0C background)
    $legacySize = $d.LegacySize
    $legacyBmp = New-Object System.Drawing.Bitmap $legacySize, $legacySize
    $g = [System.Drawing.Graphics]::FromImage($legacyBmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::FromArgb(255, 12, 12, 12)) # #0C0C0C
    
    # Center logo at 80% of boundary
    $logoSize = [int]($legacySize * 0.8)
    $offset = [int](($legacySize - $logoSize) / 2)
    $g.DrawImage($srcImage, $offset, $offset, $logoSize, $logoSize)
    
    $outputPath = Join-Path $targetDir "ic_launcher.png"
    if (Test-Path $outputPath) { Remove-Item $outputPath -Force }
    $legacyBmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $legacyBmp.Dispose()
    Write-Host "Generated legacy icon: $outputPath"

    # 2. Generate Legacy ic_launcher_round.png (Circular background circle #0C0C0C with centered logo)
    $roundBmp = New-Object System.Drawing.Bitmap $legacySize, $legacySize
    $g = [System.Drawing.Graphics]::FromImage($roundBmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 12, 12, 12))
    $g.FillEllipse($brush, 0, 0, $legacySize, $legacySize)
    $brush.Dispose()
    
    # Center logo at 70% boundary for round safety
    $logoSizeRound = [int]($legacySize * 0.7)
    $offsetRound = [int](($legacySize - $logoSizeRound) / 2)
    $g.DrawImage($srcImage, $offsetRound, $offsetRound, $logoSizeRound, $logoSizeRound)
    
    $outputPathRound = Join-Path $targetDir "ic_launcher_round.png"
    if (Test-Path $outputPathRound) { Remove-Item $outputPathRound -Force }
    $roundBmp.Save($outputPathRound, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $roundBmp.Dispose()
    Write-Host "Generated legacy round icon: $outputPathRound"

    # 3. Generate Adaptive ic_launcher_foreground.png (Centered logo on transparent background)
    $fgSize = $d.ForegroundSize
    $safeSize = $d.SafeSize
    $fgBmp = New-Object System.Drawing.Bitmap $fgSize, $fgSize
    $g = [System.Drawing.Graphics]::FromImage($fgBmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    
    # Center logo in 108dp canvas
    $offsetFg = [int](($fgSize - $safeSize) / 2)
    $g.DrawImage($srcImage, $offsetFg, $offsetFg, $safeSize, $safeSize)
    
    $outputPathFg = Join-Path $targetDir "ic_launcher_foreground.png"
    if (Test-Path $outputPathFg) { Remove-Item $outputPathFg -Force }
    $fgBmp.Save($outputPathFg, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $fgBmp.Dispose()
    Write-Host "Generated adaptive foreground: $outputPathFg"
}

$srcImage.Dispose()
Write-Host "All launcher icons generated successfully!"
