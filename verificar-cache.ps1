# ========================================
# Script de Verificación de Caché
# ========================================

Write-Host "`n╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  🔍 VERIFICACIÓN DE ACTUALIZACIÓN DE CÓDIGO                 ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# 1. Verificar que el archivo tiene la versión correcta
Write-Host "📂 Verificando archivo consumer-dashboard.html..." -ForegroundColor Yellow
$filePath = ".\packedgo\front\consumer-dashboard.html"

if (Test-Path $filePath) {
    $content = Get-Content $filePath -Raw
    
    if ($content -match "VERSION 2\.0") {
        Write-Host "   ✅ El archivo contiene VERSION 2.0" -ForegroundColor Green
    } else {
        Write-Host "   ❌ El archivo NO contiene VERSION 2.0" -ForegroundColor Red
    }
    
    if ($content -match "Cache-Control") {
        Write-Host "   ✅ El archivo tiene meta tags de no-cache" -ForegroundColor Green
    } else {
        Write-Host "   ❌ El archivo NO tiene meta tags de no-cache" -ForegroundColor Red
    }
    
    if ($content -match "document\.getElementById\('name'\)\.value") {
        Write-Host "   ✅ El archivo usa document.getElementById" -ForegroundColor Green
    } else {
        Write-Host "   ❌ El archivo NO usa document.getElementById" -ForegroundColor Red
    }
} else {
    Write-Host "   ❌ No se encuentra el archivo" -ForegroundColor Red
}

Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

# 2. Instrucciones para el usuario
Write-Host "`n🎯 PASOS PARA VERIFICAR EN EL NAVEGADOR:`n" -ForegroundColor Cyan

Write-Host "1️⃣  CIERRA COMPLETAMENTE el navegador" -ForegroundColor Yellow
Write-Host "    - Cierra TODAS las pestañas" -ForegroundColor White
Write-Host "    - Si usas Chrome/Edge: Verifica que no haya ningún ícono en la barra de tareas`n" -ForegroundColor White

Write-Host "2️⃣  ABRE el navegador nuevamente" -ForegroundColor Yellow
Write-Host "    - Presiona Ctrl + Shift + Del" -ForegroundColor White
Write-Host "    - Selecciona:" -ForegroundColor White
Write-Host "      ☑ Imágenes y archivos almacenados en caché" -ForegroundColor Green
Write-Host "      ☑ Cookies y otros datos de sitios" -ForegroundColor Green
Write-Host "    - Rango: 'Última hora'" -ForegroundColor White
Write-Host "    - Haz clic en 'Borrar datos'`n" -ForegroundColor White

Write-Host "3️⃣  ACCEDE A LA APLICACIÓN" -ForegroundColor Yellow
Write-Host "    - Ve a: http://localhost:3000/consumer-login.html" -ForegroundColor White
Write-Host "    - Inicia sesión con DNI: 33333333`n" -ForegroundColor White

Write-Host "4️⃣  ABRE LA CONSOLA DEL NAVEGADOR" -ForegroundColor Yellow
Write-Host "    - Presiona F12" -ForegroundColor White
Write-Host "    - Ve a la pestaña 'Console'" -ForegroundColor White
Write-Host "    - BUSCA este mensaje:" -ForegroundColor White
Write-Host "      🔧 VERSION 2.0 - Función updatePersonalProfile cargada correctamente" -ForegroundColor Green
Write-Host "    - Si LO VES → El archivo se actualizó correctamente ✅" -ForegroundColor Green
Write-Host "    - Si NO lo ves → Todavía hay caché antiguo ❌`n" -ForegroundColor Red

Write-Host "5️⃣  PRUEBA ACTUALIZAR EL PERFIL" -ForegroundColor Yellow
Write-Host "    - Ve a 'Mi Perfil'" -ForegroundColor White
Write-Host "    - Haz clic en 'Editar' (Información Personal)" -ForegroundColor White
Write-Host "    - Cambia tu nombre a: 'Davincha'" -ForegroundColor White
Write-Host "    - Haz clic en 'Guardar Cambios'" -ForegroundColor White
Write-Host "    - Observa la consola para ver los logs`n" -ForegroundColor White

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

Write-Host "`n📊 QUÉ DEBERÍAS VER EN LA CONSOLA:`n" -ForegroundColor Cyan
Write-Host "✅ CORRECTO (VERSION 2.0):" -ForegroundColor Green
Write-Host "   🔧 VERSION 2.0 - Función updatePersonalProfile cargada correctamente" -ForegroundColor White
Write-Host "   📤 Enviando datos de actualización: { name: 'Davincha', ... }" -ForegroundColor White
Write-Host "   🔍 Valores individuales:" -ForegroundColor White
Write-Host "      - name: Davincha" -ForegroundColor White
Write-Host "      - document: 33333333" -ForegroundColor White
Write-Host "      - gender: Masculino" -ForegroundColor White
Write-Host "      - bornDate: 1990-01-01" -ForegroundColor White
Write-Host "   📥 Respuesta del servidor: 200`n" -ForegroundColor White

Write-Host "❌ INCORRECTO (versión antigua en caché):" -ForegroundColor Red
Write-Host "   No aparece el mensaje de VERSION 2.0" -ForegroundColor White
Write-Host "   Los valores muestran: null`n" -ForegroundColor White

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

Write-Host "`n🆘 SI AÚN NO FUNCIONA:`n" -ForegroundColor Magenta
Write-Host "Opción A: Usar navegador diferente" -ForegroundColor Yellow
Write-Host "   - Si usas Chrome, prueba con Edge" -ForegroundColor White
Write-Host "   - Si usas Edge, prueba con Chrome" -ForegroundColor White
Write-Host "   - O prueba con Firefox`n" -ForegroundColor White

Write-Host "Opción B: Modo Incógnito" -ForegroundColor Yellow
Write-Host "   - Ctrl + Shift + N (Chrome/Edge)" -ForegroundColor White
Write-Host "   - Ctrl + Shift + P (Firefox)`n" -ForegroundColor White

Write-Host "Opción C: Dime qué ves en la consola" -ForegroundColor Yellow
Write-Host "   - Copia y pégame TODO lo que aparezca en Console" -ForegroundColor White
Write-Host "   - Especialmente los mensajes que empiecen con 📤 o ❌`n" -ForegroundColor White

Write-Host "╚══════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan
