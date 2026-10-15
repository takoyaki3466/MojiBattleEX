param(
    [string] $ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

Add-Type -AssemblyName System.Drawing

$fontPath = Join-Path $ProjectRoot "assets/fonts/NotoSansJP-VF.ttf"
$glyphDirectory = Join-Path $ProjectRoot "assets/glyphs"
$privateFonts = [System.Drawing.Text.PrivateFontCollection]::new()
$privateFonts.AddFontFile($fontPath)
$fontFamily = $privateFonts.Families[0]

function New-Canvas([int] $Width, [int] $Height) {
    $bitmap = [System.Drawing.Bitmap]::new($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bitmap.SetResolution(96, 96)
    return $bitmap
}

function Initialize-Graphics([System.Drawing.Bitmap] $Bitmap) {
    $graphics = [System.Drawing.Graphics]::FromImage($Bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear([System.Drawing.Color]::Transparent)
    return $graphics
}

function Save-Png([System.Drawing.Bitmap] $Bitmap, [string] $Path) {
    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Bitmap.Dispose()
}

function New-Glyph([string] $Text, [string] $Path, [System.Drawing.Color] $Fill, [System.Drawing.Color] $Outline) {
    $bitmap = New-Canvas 512 512
    $graphics = Initialize-Graphics $bitmap
    $font = [System.Drawing.Font]::new($fontFamily, 350, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $pathShape = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $emSize = $graphics.DpiY * $font.Size / 96.0
    $pathShape.AddString($Text, $fontFamily, [int][System.Drawing.FontStyle]::Bold, $emSize,
        [System.Drawing.RectangleF]::new(8, -12, 496, 520), $format)
    $outlinePen = [System.Drawing.Pen]::new($Outline, 18)
    $outlinePen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $fillBrush = [System.Drawing.SolidBrush]::new($Fill)
    $graphics.DrawPath($outlinePen, $pathShape)
    $graphics.FillPath($fillBrush, $pathShape)
    $fillBrush.Dispose()
    $outlinePen.Dispose()
    $pathShape.Dispose()
    $format.Dispose()
    $font.Dispose()
    $graphics.Dispose()
    Save-Png $bitmap $Path
}

New-Glyph "㍻" (Join-Path $glyphDirectory "heisei.png") ([System.Drawing.Color]::White) ([System.Drawing.Color]::FromArgb(255, 20, 118, 255))
New-Glyph "薪" (Join-Path $glyphDirectory "maki.png") ([System.Drawing.Color]::White) ([System.Drawing.Color]::FromArgb(255, 255, 96, 36))

$privateFonts.Dispose()
