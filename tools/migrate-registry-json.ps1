$ErrorActionPreference = 'Stop'
$effects = @(
    'mojibattle:neutral', 'mojibattle:stock', 'mojibattle:formula', 'mojibattle:speed',
    'mojibattle:fire', 'mojibattle:impact', 'mojibattle:wind', 'mojibattle:kana',
    'mojibattle:stun', 'mojibattle:unit', 'mojibattle:meteor', 'mojibattle:vehicle',
    'mojibattle:critical', 'mojibattle:cash', 'mojibattle:star', 'mojibattle:family'
)

function Add-StringProperty($object, [string]$name, [string]$value) {
    if ($null -eq $object.PSObject.Properties[$name]) {
        $object | Add-Member -NotePropertyName $name -NotePropertyValue $value
    } elseif ([string]::IsNullOrWhiteSpace($object.$name)) {
        $object.$name = $value
    }
}

$abilityDirectory = Join-Path $PSScriptRoot '..\assets\config\abilities'
Get-ChildItem -LiteralPath $abilityDirectory -Filter '*.json' | ForEach-Object {
    $definition = Get-Content -LiteralPath $_.FullName -Raw -Encoding utf8 | ConvertFrom-Json
    $characterId = $_.BaseName -replace '_(skill|ultimate|passive)$', ''
    $implementationId = if ([string]::IsNullOrWhiteSpace($definition.type) -and $_.BaseName.EndsWith('_skill')) {
        'projectile'
    } else {
        $definition.type
    }
    Add-StringProperty $definition 'implementationId' $implementationId
    if ($_.BaseName.EndsWith('_skill')) {
        Add-StringProperty $definition 'nameKey' "ability.$characterId.skill.name"
        Add-StringProperty $definition 'descriptionKey' "guide.character.$characterId"
        Add-StringProperty $definition 'activationEffectId' $effects[$definition.effectIndex]
        Add-StringProperty $definition 'projectileEffectId' $effects[$definition.effectIndex]
        Add-StringProperty $definition 'hitEffectId' $effects[$definition.effectIndex]
        $definition.PSObject.Properties.Remove('effectIndex')
    } elseif ($_.BaseName.EndsWith('_ultimate')) {
        Add-StringProperty $definition 'descriptionKey' "guide.character.$characterId"
        Add-StringProperty $definition 'warningEffectId' $effects[$definition.warningEffectIndex]
        Add-StringProperty $definition 'impactEffectId' $effects[$definition.impactEffectIndex]
        Add-StringProperty $definition 'renderEffectId' $effects[$definition.impactEffectIndex]
        $definition.PSObject.Properties.Remove('warningEffectIndex')
        $definition.PSObject.Properties.Remove('impactEffectIndex')
    } else {
        Add-StringProperty $definition 'nameKey' "ability.$characterId.passive.name"
        Add-StringProperty $definition 'descriptionKey' "guide.passive_effect.$characterId"
    }
    $definition | ConvertTo-Json -Depth 20 -Compress | Set-Content -LiteralPath $_.FullName -Encoding utf8
}

$supportDirectory = Join-Path $PSScriptRoot '..\assets\config\support'
Get-ChildItem -LiteralPath $supportDirectory -Filter '*.json' | ForEach-Object {
    $definition = Get-Content -LiteralPath $_.FullName -Raw -Encoding utf8 | ConvertFrom-Json
    Add-StringProperty $definition 'styleKey' "select.support_style.$($definition.id)"
    Add-StringProperty $definition.skill 'descriptionKey' "select.support_skill_detail.$($definition.id)"
    Add-StringProperty $definition.ultimate 'descriptionKey' "select.support_ultimate_detail.$($definition.id)"
    $definition | ConvertTo-Json -Depth 20 -Compress | Set-Content -LiteralPath $_.FullName -Encoding utf8
}
