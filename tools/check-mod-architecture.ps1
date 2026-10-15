param()

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$modules = @('api', 'core', 'mod-loader', 'item', 'lwjgl3')
$errors = [System.Collections.Generic.List[string]]::new()
$types = @{}

foreach ($module in $modules) {
    $sourceRoot = Join-Path $projectRoot "$module/src/main/java"
    if (-not (Test-Path $sourceRoot)) {
        $errors.Add("Missing Java source root: $sourceRoot")
        continue
    }
    foreach ($file in Get-ChildItem $sourceRoot -Recurse -Filter '*.java') {
        $source = Get-Content $file.FullName -Raw
        if ($source -notmatch '(?m)^package\s+([A-Za-z0-9_.]+);') {
            $errors.Add("Missing package declaration: $($file.FullName)")
            continue
        }
        $fqcn = "$($Matches[1]).$($file.BaseName)"
        $relative = $file.FullName.Substring($sourceRoot.Length + 1).Replace('\', '.').Replace('.java', '')
        if ($fqcn -ne $relative) {
            $errors.Add("Package/path mismatch: $fqcn != $relative")
        }
        if ($types.ContainsKey($fqcn)) {
            $errors.Add("Duplicate FQCN: $fqcn")
        } else {
            $types[$fqcn] = $file.FullName
        }
    }
}

$apiRoot = Join-Path $projectRoot 'api/src/main/java'
$forbiddenApiImports = Get-ChildItem $apiRoot -Recurse -Filter '*.java' |
    Select-String -Pattern '^import\s+(com\.badlogic|jp\.takoa\.mojibattle\.(config|domain|item|modloader|render|screen|ui|audio|runtime|data))'
foreach ($match in $forbiddenApiImports) {
    $errors.Add("Forbidden API dependency: $($match.Path):$($match.LineNumber)")
}

$jsonRoots = @(
    (Join-Path $projectRoot 'assets'),
    (Join-Path $projectRoot 'item/src/main/resources'),
    (Join-Path $projectRoot 'examples/example-mod/src/main/resources')
)
foreach ($root in $jsonRoots) {
    foreach ($file in Get-ChildItem $root -Recurse -Filter '*.json' -ErrorAction SilentlyContinue) {
        try {
            Get-Content $file.FullName -Raw | ConvertFrom-Json | Out-Null
        } catch {
            $errors.Add("Invalid JSON: $($file.FullName): $($_.Exception.Message)")
        }
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Static MOD architecture checks passed: $($types.Count) Java types."
