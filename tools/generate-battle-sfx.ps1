param(
    [string] $ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$sampleRate = 44100
$outputDirectory = Join-Path $ProjectRoot "assets/audio/sfx"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

function Limit-Sample([double] $Value) {
    return [Math]::Max(-1.0, [Math]::Min(1.0, $Value))
}

function Write-GeneratedWave([string] $Name, [double] $Duration, [string] $Kind, [int] $Seed) {
    $sampleCount = [int]($sampleRate * $Duration)
    $dataSize = $sampleCount * 2
    $path = Join-Path $outputDirectory $Name
    $stream = [System.IO.File]::Create($path)
    $writer = [System.IO.BinaryWriter]::new($stream)
    $random = [System.Random]::new($Seed)
    try {
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
        $writer.Write(36 + $dataSize)
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
        $writer.Write(16)
        $writer.Write([int16]1)
        $writer.Write([int16]1)
        $writer.Write($sampleRate)
        $writer.Write($sampleRate * 2)
        $writer.Write([int16]2)
        $writer.Write([int16]16)
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
        $writer.Write($dataSize)

        for ($index = 0; $index -lt $sampleCount; $index++) {
            $time = $index / [double]$sampleRate
            $progress = $time / $Duration
            $noise = $random.NextDouble() * 2.0 - 1.0
            $sample = switch ($Kind) {
                "projectile" {
                    $envelope = [Math]::Pow(1.0 - $progress, 2.2)
                    $frequency = 1500.0 - 1180.0 * $progress
                    $envelope * (0.52 * [Math]::Sin(2 * [Math]::PI * $frequency * $time) + 0.32 * $noise)
                }
                "impact" {
                    $envelope = [Math]::Exp(-12.0 * $progress)
                    $envelope * (0.68 * $noise + 0.62 * [Math]::Sin(2 * [Math]::PI * 92 * $time))
                }
                "slash" {
                    $envelope = [Math]::Sin([Math]::PI * [Math]::Min(1.0, $progress)) * [Math]::Pow(1.0 - $progress, 0.8)
                    $frequency = 2100.0 - 1650.0 * $progress
                    $envelope * (0.48 * [Math]::Sin(2 * [Math]::PI * $frequency * $time) + 0.42 * $noise)
                }
                "explosion" {
                    $envelope = [Math]::Exp(-4.4 * $progress)
                    $rumble = [Math]::Sin(2 * [Math]::PI * (58 - 18 * $progress) * $time)
                    $envelope * (0.54 * $noise + 0.68 * $rumble)
                }
                "vehicle" {
                    $attack = [Math]::Min(1.0, $progress * 8.0)
                    $release = [Math]::Min(1.0, (1.0 - $progress) * 5.0)
                    $envelope = $attack * $release
                    $engine = [Math]::Sin(2 * [Math]::PI * (86 + 48 * $progress) * $time)
                    $horn = [Math]::Sin(2 * [Math]::PI * 310 * $time) + 0.55 * [Math]::Sin(2 * [Math]::PI * 465 * $time)
                    $envelope * (0.42 * $engine + 0.34 * $horn + 0.12 * $noise)
                }
                "skill" {
                    $envelope = [Math]::Sin([Math]::PI * $progress) * [Math]::Pow(1.0 - $progress, 0.35)
                    $frequency = 280.0 + 1250.0 * $progress * $progress
                    $sparkle = [Math]::Sin(2 * [Math]::PI * 1960 * $time) * [Math]::Pow($progress, 2)
                    $envelope * (0.68 * [Math]::Sin(2 * [Math]::PI * $frequency * $time) + 0.22 * $sparkle)
                }
                "ultimate" {
                    $attack = [Math]::Min(1.0, $progress * 5.0)
                    $release = [Math]::Min(1.0, (1.0 - $progress) * 3.5)
                    $envelope = $attack * $release
                    $low = [Math]::Sin(2 * [Math]::PI * (62 + 74 * $progress) * $time)
                    $rise = [Math]::Sin(2 * [Math]::PI * (330 + 1550 * $progress * $progress) * $time)
                    $envelope * (0.55 * $low + 0.48 * $rise + 0.12 * $noise)
                }
                "support" {
                    $envelope = [Math]::Exp(-3.2 * $progress)
                    $chime = [Math]::Sin(2 * [Math]::PI * 660 * $time) + 0.7 * [Math]::Sin(2 * [Math]::PI * 990 * $time)
                    $envelope * (0.54 * $chime + 0.16 * $noise)
                }
            }
            $writer.Write([int16]((Limit-Sample $sample) * 30000))
        }
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
}

Write-GeneratedWave "projectile.wav" 0.28 "projectile" 101
Write-GeneratedWave "impact.wav" 0.30 "impact" 202
Write-GeneratedWave "slash.wav" 0.42 "slash" 303
Write-GeneratedWave "explosion.wav" 0.95 "explosion" 404
Write-GeneratedWave "vehicle.wav" 0.85 "vehicle" 505
Write-GeneratedWave "skill.wav" 0.55 "skill" 606
Write-GeneratedWave "ultimate.wav" 1.15 "ultimate" 707
Write-GeneratedWave "support.wav" 0.62 "support" 808
