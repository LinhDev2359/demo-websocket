# 🛡️ Security Guide - EOS Wallet System

## 🔐 Environment Variables & Secrets Management

### Quick Setup (Recommended)
```bash
# Generate secure environment with random passwords
./setup-env.sh

# Check security configuration
./check-security.sh
```

### Manual Setup
```bash
# 1. Copy template
cp .env.example .env

# 2. Edit with your secure values
nano .env

# 3. Set proper permissions
chmod 600 .env
```

## 📋 Security Checklist

### ✅ Before Development
- [ ] Run `./setup-env.sh` to generate secure credentials
- [ ] Verify `.env` file is in `.gitignore`
- [ ] Check no hardcoded secrets with `./check-security.sh`
- [ ] Use strong passwords (12+ characters)
- [ ] Set proper file permissions: `chmod 600 .env`

### ✅ Before Committing Code
- [ ] Run `./check-security.sh`
- [ ] Verify no `.env*` files are staged: `git status`
- [ ] Check for accidental secrets: `git diff --cached`
- [ ] Remove any hardcoded credentials
- [ ] Use environment variables for all sensitive data

### ✅ Before Production Deployment
- [ ] Generate new production secrets
- [ ] Use proper secret management (AWS Secrets Manager, HashiCorp Vault, etc.)
- [ ] Enable SSL/TLS encryption
- [ ] Set up proper backup encryption
- [ ] Configure monitoring and alerting
- [ ] Rotate JWT secrets regularly

## 🔒 Sensitive Information Categories

### 🚫 NEVER Commit These:
- Database passwords
- JWT secrets
- API keys
- Private keys (.key, .pem files)
- SSL certificates
- Configuration files with real credentials
- `.env` files
- Database dumps with real data

### ✅ Safe to Commit:
- `.env.example` (template with placeholder values)
- `docker-compose.yml` (using environment variables)
- Configuration templates
- Public keys
- Documentation
- Code without hardcoded secrets

## 🛠️ Security Tools & Scripts

### `./setup-env.sh`
Generates secure random passwords and creates `.env` file:
- 25-character database passwords
- 64-character JWT secrets
- Secure Redis passwords
- Environment-specific configuration

### `./check-security.sh`
Comprehensive security audit:
- Scans for hardcoded secrets
- Verifies `.gitignore` coverage
- Checks file permissions
- Validates password strength
- Ensures no secrets in Git history

## 🌍 Environment-Specific Security

### Development
- Use `.env.dev` for development defaults
- Generate unique passwords (don't use defaults)
- Enable debug logging for security events
- Use localhost connections only

### Staging
- Mirror production security settings
- Use separate database and credentials
- Test security configurations
- Verify SSL/TLS setup

### Production
- Use external secret management
- Enable audit logging
- Set up monitoring and alerting
- Implement proper backup encryption
- Use read-only database replicas
- Enable rate limiting and DDoS protection

## 🔧 Configuration Security

### Database Security
```bash
# Strong passwords
MYSQL_ROOT_PASSWORD=<32-character-random>
MYSQL_PASSWORD=<25-character-random>

# Network security
- Bind to localhost only in development
- Use private networks in production
- Enable SSL for database connections
```

### Redis Security
```bash
# Authentication
REDIS_PASSWORD=<25-character-random>

# Disable dangerous commands
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command DEBUG ""
```

### JWT Security
```bash
# Strong secret (minimum 32 characters)
JWT_SECRET=<64-character-random>

# Proper expiration
jwt.expiration=86400  # 24 hours
jwt.refresh-expiration=604800  # 7 days
```

## 🚨 Incident Response

### If Secrets Are Accidentally Committed:
1. **Immediate Action:**
   ```bash
   # Remove from staging
   git reset --soft HEAD~1
   git reset HEAD .env
   
   # If already pushed
   git revert <commit-hash>
   git push
   ```

2. **Rotate All Exposed Secrets:**
   ```bash
   # Generate new credentials
   ./setup-env.sh
   
   # Update all environments
   # Change database passwords
   # Rotate JWT secrets
   # Update API keys
   ```

3. **Clean Git History:**
   ```bash
   # Remove from entire git history (destructive!)
   git filter-branch --force --index-filter \
   'git rm --cached --ignore-unmatch .env' \
   --prune-empty --tag-name-filter cat -- --all
   
   # Force push (coordinate with team!)
   git push --force --all
   git push --force --tags
   ```

### If Database is Compromised:
1. Change all database passwords immediately
2. Rotate JWT secrets
3. Audit access logs
4. Check for data exfiltration
5. Notify security team/users if required

## 📚 Security Resources

### OWASP Guidelines
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security](https://owasp.org/www-project-api-security/)
- [OWASP Docker Security](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)

### Spring Security
- [Spring Security Architecture](https://spring.io/guides/topicals/spring-security-architecture)
- [Spring Boot Security](https://spring.io/guides/gs/securing-web/)
- [JWT Best Practices](https://auth0.com/blog/a-look-at-the-latest-draft-for-jwt-bcp/)

### Docker Security
- [Docker Security Guide](https://docs.docker.com/engine/security/)
- [Container Security](https://kubernetes.io/docs/concepts/security/)

---

## 🎯 Quick Commands

```bash
# Setup secure environment
./setup-env.sh

# Security audit
./check-security.sh

# Start with secure config
./reset-dev.sh

# Test security
grep -r "password" . --exclude-dir=.git | grep -v ".env"
```

**Remember: Security is everyone's responsibility! 🛡️**