# JWT Token Validation Test

## Test JWT Token từ Log
```
Token: eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJ1c2VySWQiOiJ1c2VyX2RjOThiNzg5MGJlYiIsImVtYWlsIjoidHJ1b25nZ2lhbGluaDIzNTlAZ21haWwuY29tIiwiYXV0aG9yaXRpZXMiOlt7ImF1dGhvcml0eSI6IlJPTEVfVVNFUiJ9XSwic3ViIjoidHJ1b25nZ2lhbGluaDIzNTlAZ21haWwuY29tIiwiaWF0IjoxNzUyMDY5MDI3LCJleHAiOjE3NTIxNTU0Mjd9.E0qmE-4WjQ8n_FRiBRAppUiHmYoqIP7TOzO_JDh4g5ARhT2EBxbXERvqrEWT_SfN34HY1lnTDsaHkuc6h9wokA
```

## Payload Decoded
```json
{
  "tokenType": "ACCESS",
  "userId": "user_dc98b7890beb",
  "email": "truonggialinh2359@gmail.com",
  "authorities": [{"authority": "ROLE_USER"}],
  "sub": "truonggialinh2359@gmail.com",
  "iat": 1752069027,
  "exp": 1752155427
}
```

## Issues Found
1. **Token Expiration**: Token expires at 1752155427 (Unix timestamp)
2. **Current Time**: Check if current time is before expiration
3. **JWT Secret**: Make sure the same secret is used for validation and generation
4. **Key Padding**: Fixed the key padding issue in JwtTokenUtil

## Changes Made
1. **Fixed JwtTokenUtil.getSigningKey()**: Removed padding logic
2. **Enhanced JWT secret**: Made it 64+ bytes for HS512
3. **Improved error logging**: Better debugging info
4. **Fixed WebSocket interceptor**: Proper token validation flow

## Test Instructions
1. Restart the application
2. Login to get fresh JWT token
3. Connect via WebSocket
4. Check logs for detailed validation info
5. Verify Portfolio Operations buttons are enabled

## Expected Behavior
- JWT token should be extracted correctly during handshake
- Token validation should pass with proper secret
- WebSocket connection should stay connected
- User should be authenticated with proper userId
- Portfolio Operations buttons should be enabled