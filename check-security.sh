#!/bin/bash

echo "🔍 Security Check for EOS Wallet System"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ISSUES=0

echo "================================"
echo "1. Checking for hardcoded secrets..."

# Check docker-compose files for hardcoded passwords
echo "📋 Scanning docker-compose files..."

if grep -r "password.*:" docker-compose*.yml | grep -v "\${" | grep -v "#"; then
    echo -e "${RED}❌ Found hardcoded passwords in docker-compose files!${NC}"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}✅ No hardcoded passwords found in docker-compose files${NC}"
fi

# Check application.yml for secrets
echo "📋 Scanning application.yml..."
if grep -E "(password|secret|key).*:" src/main/resources/application.yml | grep -v "\${" | grep -v "#" | grep -v "secret: false"; then
    echo -e "${RED}❌ Found potential hardcoded secrets in application.yml!${NC}"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}✅ No hardcoded secrets found in application.yml${NC}"
fi

echo ""
echo "================================"
echo "2. Checking .gitignore coverage..."

REQUIRED_GITIGNORE_ENTRIES=(
    ".env"
    ".env.local"
    ".env.production"
    "*.secret"
    "mysql/init-dev.sql"
)

for entry in "${REQUIRED_GITIGNORE_ENTRIES[@]}"; do
    if grep -q "^${entry}$" .gitignore; then
        echo -e "${GREEN}✅ .gitignore includes: ${entry}${NC}"
    else
        echo -e "${RED}❌ .gitignore missing: ${entry}${NC}"
        ISSUES=$((ISSUES + 1))
    fi
done

echo ""
echo "================================"
echo "3. Checking environment files..."

# Check if .env exists but is properly protected
if [ -f .env ]; then
    echo -e "${GREEN}✅ .env file exists${NC}"
    
    # Check .env file permissions
    PERM=$(stat -c "%a" .env)
    if [ "$PERM" = "600" ] || [ "$PERM" = "644" ]; then
        echo -e "${GREEN}✅ .env file permissions are secure (${PERM})${NC}"
    else
        echo -e "${YELLOW}⚠️  .env file permissions could be more secure (current: ${PERM}, recommended: 600)${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  .env file not found. Run ./setup-env.sh to create it${NC}"
fi

# Check if .env.example exists
if [ -f .env.example ]; then
    echo -e "${GREEN}✅ .env.example file exists${NC}"
else
    echo -e "${RED}❌ .env.example file missing${NC}"
    ISSUES=$((ISSUES + 1))
fi

echo ""
echo "================================"
echo "4. Checking for committed secrets..."

# Check if any .env files are tracked by git
if git ls-files | grep -E "\.env$|\.env\."; then
    echo -e "${RED}❌ Environment files are tracked by Git!${NC}"
    echo "Run: git rm --cached .env* && git commit -m 'Remove env files'"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}✅ No environment files are tracked by Git${NC}"
fi

# Check for large files that might contain secrets
echo "📋 Checking for potentially sensitive files..."
SENSITIVE_PATTERNS=(
    "*.key"
    "*.pem"
    "*.p12"
    "*.jks"
    "*password*"
    "*secret*"
    "*credential*"
)

for pattern in "${SENSITIVE_PATTERNS[@]}"; do
    if find . -name "$pattern" -not -path "./.git/*" | head -1 | grep -q .; then
        echo -e "${YELLOW}⚠️  Found files matching pattern: ${pattern}${NC}"
        find . -name "$pattern" -not -path "./.git/*"
        echo "   Make sure these are in .gitignore!"
    fi
done

echo ""
echo "================================"
echo "5. Password strength check..."

if [ -f .env ]; then
    # Check JWT secret length
    JWT_SECRET=$(grep "JWT_SECRET=" .env | cut -d'=' -f2)
    if [ ${#JWT_SECRET} -lt 32 ]; then
        echo -e "${RED}❌ JWT_SECRET is too short (${#JWT_SECRET} chars, minimum 32)${NC}"
        ISSUES=$((ISSUES + 1))
    else
        echo -e "${GREEN}✅ JWT_SECRET length is adequate (${#JWT_SECRET} chars)${NC}"
    fi
    
    # Check password complexity
    MYSQL_PASS=$(grep "MYSQL_PASSWORD=" .env | cut -d'=' -f2)
    if [ ${#MYSQL_PASS} -lt 12 ]; then
        echo -e "${YELLOW}⚠️  MySQL password is short (${#MYSQL_PASS} chars, recommended 12+)${NC}"
    else
        echo -e "${GREEN}✅ MySQL password length is good (${#MYSQL_PASS} chars)${NC}"
    fi
fi

echo ""
echo "================================"
echo "📊 SECURITY SUMMARY"
echo "================================"

if [ $ISSUES -eq 0 ]; then
    echo -e "${GREEN}🎉 Security check passed! No issues found.${NC}"
    echo ""
    echo "🛡️  Security best practices in place:"
    echo "   ✅ Environment variables used for secrets"
    echo "   ✅ Sensitive files in .gitignore"
    echo "   ✅ No hardcoded credentials"
    echo "   ✅ Proper file structure"
else
    echo -e "${RED}⚠️  Found ${ISSUES} security issue(s) that need attention!${NC}"
    echo ""
    echo "🔧 Recommended actions:"
    echo "   1. Run ./setup-env.sh to generate secure environment"
    echo "   2. Review and fix any hardcoded secrets"
    echo "   3. Ensure .gitignore covers all sensitive files"
    echo "   4. Use strong, unique passwords"
fi

echo ""
echo "📚 Security resources:"
echo "   - OWASP Top 10: https://owasp.org/www-project-top-ten/"
echo "   - Spring Security Guide: https://spring.io/guides/topicals/spring-security-architecture"
echo "   - Docker Security: https://docs.docker.com/engine/security/"