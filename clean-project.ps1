# Script de Limpieza - PackedGo
# Ejecutar antes de compartir el proyecto

Write-Host "🧹 Iniciando limpieza del proyecto PackedGo..." -ForegroundColor Cyan

$projectRoot = Get-Location

# Eliminar carpetas node_modules
Write-Host "`n📦 Eliminando node_modules..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter "node_modules" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Eliminar carpetas target (Maven)
Write-Host "`n🎯 Eliminando carpetas target..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter "target" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Eliminar carpetas .angular (cache)
Write-Host "`n⚡ Eliminando cache de Angular..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter ".angular" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Eliminar carpetas dist
Write-Host "`n📂 Eliminando carpetas dist..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter "dist" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Eliminar archivos .log
Write-Host "`n📝 Eliminando archivos .log..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -File -Filter "*.log" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Force -ErrorAction SilentlyContinue
}

# Eliminar archivos temporales
Write-Host "`n🗑️  Eliminando archivos temporales..." -ForegroundColor Yellow
$tempExtensions = @("*.tmp", "*.bak", "*.swp", "*~")
foreach ($ext in $tempExtensions) {
    Get-ChildItem -Path $projectRoot -Recurse -File -Filter $ext -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
        Remove-Item -Path $_.FullName -Force -ErrorAction SilentlyContinue
    }
}

# Eliminar carpetas .idea (IntelliJ)
Write-Host "`n💡 Eliminando carpetas .idea..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter ".idea" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Eliminar carpetas .vscode
Write-Host "`n🔧 Eliminando carpetas .vscode..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter ".vscode" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Eliminar carpetas .claude
Write-Host "`n🤖 Eliminando carpetas .claude..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter ".claude" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Eliminando: $($_.FullName)" -ForegroundColor Gray
    Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

# Info sobre archivos .env
Write-Host "`n📋 Archivos .env incluidos en el repositorio" -ForegroundColor Cyan
Write-Host "Los archivos .env están incluidos para facilitar el despliegue." -ForegroundColor White
Write-Host "`nArchivos .env encontrados:" -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Recurse -File -Filter ".env" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  ✓ $($_.FullName)" -ForegroundColor Green
}

Write-Host "`n✅ Limpieza completada!" -ForegroundColor Green
Write-Host "`n📋 Próximos pasos:" -ForegroundColor Cyan
Write-Host "  1. Verifica que los archivos .env.example estén actualizados" -ForegroundColor White
Write-Host "  2. Asegúrate de que .gitignore incluya todos los archivos sensibles" -ForegroundColor White
Write-Host "  3. Revisa INSTALACION.md para instrucciones completas" -ForegroundColor White
Write-Host "  4. Ejecuta 'git status' para verificar qué archivos se van a commitear" -ForegroundColor White
