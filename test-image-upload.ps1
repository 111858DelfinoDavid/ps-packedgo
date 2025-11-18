# Script para probar la funcionalidad de carga de imágenes en eventos
# Requiere: Un evento existente y un token JWT válido

Write-Host "=== Test de Carga de Imágenes para Eventos ===" -ForegroundColor Cyan

# Variables de configuración
$baseUrl = "http://localhost:8086/api"
$eventId = Read-Host "Ingrese el ID del evento al que desea subir una imagen"

# Solicitar credenciales para obtener token
Write-Host "`n1. Obteniendo token de autenticación..." -ForegroundColor Yellow
$username = Read-Host "Usuario (email)"
$password = Read-Host "Contraseña" -AsSecureString
$passwordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
)

$loginBody = @{
    username = $username
    password = $passwordPlain
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.token
    Write-Host "✓ Token obtenido correctamente" -ForegroundColor Green
} catch {
    Write-Host "✗ Error al obtener token: $_" -ForegroundColor Red
    exit 1
}

# Solicitar ruta de la imagen
Write-Host "`n2. Selección de imagen..." -ForegroundColor Yellow
$imagePath = Read-Host "Ingrese la ruta completa de la imagen (PNG, JPG o JPEG)"

if (-not (Test-Path $imagePath)) {
    Write-Host "✗ El archivo no existe: $imagePath" -ForegroundColor Red
    exit 1
}

$fileInfo = Get-Item $imagePath
$extension = $fileInfo.Extension.ToLower()

if ($extension -notin @('.png', '.jpg', '.jpeg')) {
    Write-Host "✗ Formato de archivo no válido. Use PNG, JPG o JPEG" -ForegroundColor Red
    exit 1
}

$fileSize = $fileInfo.Length
$maxSize = 5MB

if ($fileSize -gt $maxSize) {
    Write-Host "✗ El archivo excede el tamaño máximo de 5MB (Tamaño: $([math]::Round($fileSize/1MB, 2)) MB)" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Archivo válido: $($fileInfo.Name) ($([math]::Round($fileSize/1KB, 2)) KB)" -ForegroundColor Green

# Cargar la imagen
Write-Host "`n3. Cargando imagen al evento..." -ForegroundColor Yellow

$contentType = switch ($extension) {
    '.png' { 'image/png' }
    '.jpg' { 'image/jpeg' }
    '.jpeg' { 'image/jpeg' }
}

try {
    # Leer el archivo como bytes
    $fileBytes = [System.IO.File]::ReadAllBytes($imagePath)
    
    # Crear el boundary para multipart
    $boundary = [System.Guid]::NewGuid().ToString()
    
    # Construir el cuerpo multipart manualmente
    $LF = "`r`n"
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"$($fileInfo.Name)`"",
        "Content-Type: $contentType$LF",
        [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes),
        "--$boundary--$LF"
    ) -join $LF
    
    # Hacer la petición
    $uploadUrl = "$baseUrl/events/$eventId/image"
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    
    $response = Invoke-RestMethod -Uri $uploadUrl -Method POST -Body $bodyLines -ContentType "multipart/form-data; boundary=$boundary" -Headers $headers
    
    Write-Host "✓ Imagen cargada correctamente" -ForegroundColor Green
    Write-Host "Respuesta del servidor:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 3 | Write-Host
    
} catch {
    Write-Host "✗ Error al cargar la imagen: $_" -ForegroundColor Red
    Write-Host "Detalles: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verificar que la imagen se puede descargar
Write-Host "`n4. Verificando descarga de la imagen..." -ForegroundColor Yellow

try {
    $downloadUrl = "$baseUrl/events/$eventId/image"
    $outputPath = Join-Path $env:TEMP "downloaded_event_image$extension"
    
    Invoke-RestMethod -Uri $downloadUrl -Method GET -Headers $headers -OutFile $outputPath
    
    $downloadedFile = Get-Item $outputPath
    Write-Host "✓ Imagen descargada correctamente: $outputPath" -ForegroundColor Green
    Write-Host "  Tamaño: $([math]::Round($downloadedFile.Length/1KB, 2)) KB" -ForegroundColor Cyan
    
    # Preguntar si desea abrir la imagen
    $openImage = Read-Host "`n¿Desea abrir la imagen descargada? (S/N)"
    if ($openImage -eq 'S' -or $openImage -eq 's') {
        Start-Process $outputPath
    }
    
} catch {
    Write-Host "✗ Error al descargar la imagen: $_" -ForegroundColor Red
}

Write-Host "`n=== Test completado ===" -ForegroundColor Cyan
