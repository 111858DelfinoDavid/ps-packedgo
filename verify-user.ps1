# 🔧 Script para verificar usuarios manualmente en la base de datos

param(
    [Parameter(Mandatory=$false)]
    [string]$Email,
    
    [Parameter(Mandatory=$false)]
    [switch]$VerifyAll
)

Write-Host "`n🔧 VERIFICACIÓN MANUAL DE USUARIOS" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""

if ($VerifyAll) {
    Write-Host "📝 Verificando TODOS los usuarios..." -ForegroundColor Yellow
    Write-Host ""
    
    # Verificar todos los usuarios
    $sql = "UPDATE auth_users SET email_verified = true, updated_at = CURRENT_TIMESTAMP WHERE email_verified = false;"
    $listSql = "SELECT id, username, email, role, email_verified, created_at FROM auth_users ORDER BY id;"
    
    docker exec -it back-auth-db-1 psql -U auth_user -d auth_db -c "$sql"
    
    Write-Host ""
    Write-Host "✅ Todos los usuarios verificados" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Lista de usuarios:" -ForegroundColor Cyan
    docker exec -it back-auth-db-1 psql -U auth_user -d auth_db -c "$listSql"
    
} elseif ($Email) {
    Write-Host "📝 Verificando usuario con email: $Email" -ForegroundColor Yellow
    Write-Host ""
    
    # Verificar usuario específico
    $sql = "UPDATE auth_users SET email_verified = true, updated_at = CURRENT_TIMESTAMP WHERE email = '$Email';"
    $checkSql = "SELECT id, username, email, role, email_verified FROM auth_users WHERE email = '$Email';"
    
    docker exec -it back-auth-db-1 psql -U auth_user -d auth_db -c "$sql"
    
    Write-Host ""
    Write-Host "✅ Usuario verificado" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Datos del usuario:" -ForegroundColor Cyan
    docker exec -it back-auth-db-1 psql -U auth_user -d auth_db -c "$checkSql"
    
} else {
    # Mostrar lista de usuarios sin verificar
    Write-Host "📋 Usuarios SIN VERIFICAR:" -ForegroundColor Yellow
    Write-Host ""
    
    $listSql = "SELECT id, username, email, role, email_verified, created_at FROM auth_users WHERE email_verified = false ORDER BY id;"
    docker exec -it back-auth-db-1 psql -U auth_user -d auth_db -c "$listSql"
    
    Write-Host ""
    Write-Host "💡 USO DEL SCRIPT:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Verificar un usuario específico:" -ForegroundColor White
    Write-Host "  .\verify-user.ps1 -Email 'admin@packedgo.com'" -ForegroundColor Green
    Write-Host ""
    Write-Host "Verificar TODOS los usuarios:" -ForegroundColor White
    Write-Host "  .\verify-user.ps1 -VerifyAll" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ver usuarios sin verificar (este comando):" -ForegroundColor White
    Write-Host "  .\verify-user.ps1" -ForegroundColor Green
    Write-Host ""
}

Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""
