/**
 * ============================================
 * PERFIL UPDATE HANDLER - VERSION 2.0
 * Fecha: 2025-11-07 20:20
 * ============================================
 * Este archivo maneja la actualización del perfil
 * usando acceso directo al DOM (sin FormData)
 */

console.log('✅ profile-update-v2.js cargado correctamente');

window.updatePersonalProfileV2 = async function() {
    console.log('🔧 VERSION 2.0 - Ejecutando updatePersonalProfileV2');
    
    try {
        // Obtener userId del token JWT
        const token = localStorage.getItem('userToken');
        if (!token) {
            alert('Error: No hay token de sesión');
            return false;
        }
        
        // Decodificar JWT para obtener userId
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        const payload = JSON.parse(jsonPayload);
        const userId = payload.userId;
        
        console.log('👤 userId extraído del token:', userId);
        
        // ⚠️ CRÍTICO: Obtener valores DIRECTAMENTE del DOM
        const nameValue = document.getElementById('name').value;
        const lastNameValue = document.getElementById('lastName').value;
        const documentValue = document.getElementById('document').value;
        const genderValue = document.getElementById('gender').value;
        const bornDateValue = document.getElementById('bornDate').value;
        const telephoneValue = document.getElementById('telephone').value;
        const profileImageUrlValue = document.getElementById('profileImageUrl').value;
        
        console.log('🔍 Valores extraídos del DOM:');
        console.log('   - name:', nameValue);
        console.log('   - lastName:', lastNameValue);
        console.log('   - document:', documentValue);
        console.log('   - gender:', genderValue);
        console.log('   - bornDate:', bornDateValue);
        console.log('   - telephone:', telephoneValue);
        console.log('   - profileImageUrl:', profileImageUrlValue);
        
        // Construir objeto de request
        const requestData = {
            name: nameValue,
            lastName: lastNameValue,
            document: parseInt(documentValue),
            gender: genderValue,
            bornDate: bornDateValue,
            telephone: parseInt(telephoneValue),
            profileImageUrl: profileImageUrlValue || ''
        };

        console.log('📤 Request completo a enviar:', JSON.stringify(requestData, null, 2));

        // Hacer la petición PUT
        const response = await fetch(`http://localhost:8082/api/user-profiles/by-auth-user/${userId}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });

        console.log('📥 Respuesta del servidor:', response.status, response.statusText);

        if (response.ok) {
            const result = await response.json();
            console.log('✅ Perfil actualizado exitosamente:', result);
            alert('✅ Información personal actualizada correctamente');
            
            // Deshabilitar modo edición
            document.getElementById('name').disabled = true;
            document.getElementById('lastName').disabled = true;
            document.getElementById('gender').disabled = true;
            document.getElementById('bornDate').disabled = true;
            document.getElementById('telephone').disabled = true;
            document.getElementById('profileImageUrl').disabled = true;
            
            document.getElementById('edit-personal-btn').style.display = 'inline-block';
            document.getElementById('save-personal-btn').style.display = 'none';
            document.getElementById('cancel-personal-btn').style.display = 'none';
            
            return true;
        } else {
            const errorText = await response.text();
            console.error('❌ Error del servidor:', errorText);
            
            try {
                const errorJson = JSON.parse(errorText);
                alert(`❌ Error: ${errorJson.message || 'No se pudo actualizar el perfil'}`);
            } catch {
                alert(`❌ Error al actualizar el perfil (código ${response.status})`);
            }
            return false;
        }

    } catch (error) {
        console.error('❌ Error en updatePersonalProfileV2:', error);
        alert('❌ Error de conexión al actualizar la información personal');
        return false;
    }
};

console.log('✅ Función updatePersonalProfileV2 definida globalmente');
