#!/usr/bin/env python3
"""
Script para generar JWT tokens para testing de PackedGo
Basado en la configuración de auth-service
"""

import jwt
import datetime
import base64

# JWT Secret del .env de auth-service
JWT_SECRET_STRING = "mySecretKey123456789PackedGoAuth2025VerySecureKey"

# Java hace: Decoders.BASE64.decode(jwtSecret)
# Esto significa que el secret string está en BASE64 y lo decodifica a bytes
# Entonces nosotros también debemos encodear a BASE64 y luego decodificar
# O simplemente usar el string tal cual como bytes
JWT_SECRET = JWT_SECRET_STRING.encode('utf-8')

# Algoritmo usado por Spring Security
ALGORITHM = "HS256"

# Expiración: 1 hora (como en el .env: JWT_EXPIRATION=3600000 ms)
EXPIRATION_HOURS = 1

def generate_token(user_id, username, role):
    """
    Genera un JWT token compatible con auth-service de PackedGo

    Args:
        user_id: ID del usuario en auth_users
        username: Username del usuario
        role: CUSTOMER, ADMIN, o SUPER_ADMIN

    Returns:
        String del JWT token
    """
    now = datetime.datetime.utcnow()
    expiration = now + datetime.timedelta(hours=EXPIRATION_HOURS)

    # Payload según JwtTokenGenerator de auth-service
    payload = {
        "sub": username,  # Subject: username
        "userId": user_id,
        "role": role,
        "authorities": [],  # Lista vacía según el código
        "iat": int(now.timestamp()),  # Issued at
        "exp": int(expiration.timestamp())  # Expiration
    }

    token = jwt.encode(payload, JWT_SECRET, algorithm=ALGORITHM)

    return token

def main():
    print("=" * 80)
    print("GENERADOR DE JWT TOKENS - PACKEDGO")
    print("=" * 80)
    print()

    # Usuarios de prueba basados en la BD
    users = [
        {"id": 1, "username": "agustin", "role": "CUSTOMER", "desc": "Customer real (Agustín)"},
        {"id": 4, "username": "test_customer_001", "role": "CUSTOMER", "desc": "Test Customer 001"},
        {"id": 6, "username": "test_customer_002", "role": "CUSTOMER", "desc": "Test Customer 002"},
        {"id": 5, "username": "test_admin_001", "role": "ADMIN", "desc": "Test Admin 001"},
        {"id": 7, "username": "test_admin_002", "role": "ADMIN", "desc": "Test Admin 002"},
    ]

    for user in users:
        token = generate_token(user["id"], user["username"], user["role"])

        print(f">> {user['desc']}")
        print(f"   User ID: {user['id']}")
        print(f"   Username: {user['username']}")
        print(f"   Role: {user['role']}")
        print(f"   Token: {token}")
        print()
        print(f"   # Para usar en curl:")
        print(f"   export TOKEN_{user['role']}_{user['id']}=\"{token}\"")
        print()
        print("-" * 80)
        print()

    # Generar también las variables de entorno para copiar/pegar
    print()
    print("=" * 80)
    print("VARIABLES DE ENTORNO PARA COPIAR/PEGAR:")
    print("=" * 80)
    print()

    for user in users:
        token = generate_token(user["id"], user["username"], user["role"])
        var_name = f"TOKEN_{user['role']}_{user['id']}"
        print(f'export {var_name}="{token}"')

    print()
    print("=" * 80)
    print("EJEMPLO DE USO:")
    print("=" * 80)
    print()
    print('# Test como customer:')
    print('curl -X GET "http://localhost:8082/api/user-profiles/4" \\')
    print('  -H "Authorization: Bearer $TOKEN_CUSTOMER_4"')
    print()
    print('# Test como admin:')
    print('curl -X GET "http://localhost:8086/api/event-service/event/1" \\')
    print('  -H "Authorization: Bearer $TOKEN_ADMIN_5"')
    print()

if __name__ == "__main__":
    try:
        main()
    except ImportError:
        print("ERROR: PyJWT no está instalado")
        print("Instálalo con: pip install pyjwt")
        exit(1)
