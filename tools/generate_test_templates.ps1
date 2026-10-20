$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$target = Join-Path $taskRoot 'src/gametest/resources/data/alone_mechanical/structures'
[IO.Directory]::CreateDirectory($target) | Out-Null
function Write-BigInt($stream, [int]$n) { $bytes = [BitConverter]::GetBytes($n); [Array]::Reverse($bytes); $stream.Write($bytes, 0, 4) }
function Write-Name($stream, [string]$name) { $bytes = [Text.Encoding]::UTF8.GetBytes($name); $stream.WriteByte([byte]($bytes.Length -shr 8)); $stream.WriteByte([byte]($bytes.Length -band 255)); $stream.Write($bytes, 0, $bytes.Length) }
foreach ($template in @(@{ name = 'empty'; width = 12; depth = 12 }, @{ name = 'grid'; width = 96; depth = 12 }, @{name='reactor_safety';width=96;depth=96})) {
    $raw = [IO.MemoryStream]::new()
    $raw.WriteByte(10); Write-Name $raw ''
    $raw.WriteByte(3); Write-Name $raw 'DataVersion'; Write-BigInt $raw 3465
    $raw.WriteByte(9); Write-Name $raw 'size'; $raw.WriteByte(3); Write-BigInt $raw 3
    Write-BigInt $raw $template.width; Write-BigInt $raw 12; Write-BigInt $raw $template.depth
    foreach ($name in @('blocks','entities')) { $raw.WriteByte(9); Write-Name $raw $name; $raw.WriteByte(10); Write-BigInt $raw 0 }
    $raw.WriteByte(9); Write-Name $raw 'palette'; $raw.WriteByte(10); Write-BigInt $raw 1
    $raw.WriteByte(8); Write-Name $raw 'Name'; Write-Name $raw 'minecraft:air'; $raw.WriteByte(0); $raw.WriteByte(0)
    $file = [IO.File]::Create((Join-Path $target ($template.name + '.nbt')))
    $gzip = [IO.Compression.GZipStream]::new($file, [IO.Compression.CompressionMode]::Compress)
    $raw.Position = 0; $raw.CopyTo($gzip); $gzip.Dispose(); $file.Dispose(); $raw.Dispose()
}
Write-Output 'Generated GameTest structure templates.'
