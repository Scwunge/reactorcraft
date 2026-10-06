# Runs gradlew with BelowNormal priority (the PC is shared with gaming). Usage: scripts\gradle-low.ps1 build
param([Parameter(ValueFromRemainingArguments = $true)] [string[]] $Tasks)
# lower ourselves first so gradlew, its JVM and the forked daemon all inherit BelowNormal
[System.Diagnostics.Process]::GetCurrentProcess().PriorityClass = 'BelowNormal'
$root = Split-Path $PSScriptRoot -Parent
$log = Join-Path $root 'build\gradle-low.log'
New-Item -ItemType Directory -Force (Split-Path $log) | Out-Null
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = Join-Path $root 'gradlew.bat'
$psi.Arguments = (($Tasks + '--no-daemon', '--console=plain') -join ' ')
$psi.WorkingDirectory = $root
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$p = [System.Diagnostics.Process]::Start($psi)
try { $p.PriorityClass = 'BelowNormal' } catch {}
$out = $p.StandardOutput.ReadToEndAsync()
$err = $p.StandardError.ReadToEndAsync()
$p.WaitForExit()
($out.Result + $err.Result) | Set-Content -Encoding utf8 $log
Get-Content $log -Tail 60
"EXIT $($p.ExitCode)"
exit $p.ExitCode
