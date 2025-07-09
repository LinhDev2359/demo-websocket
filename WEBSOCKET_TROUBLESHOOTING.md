# 🔧 WEBSOCKET TROUBLESHOOTING GUIDE

## ✅ **Lỗi đã được fix!**

### 🐛 **Lỗi đã gặp phải:**

#### **1. ClassNotFoundException Security Issue:**
```
java.lang.ClassNotFoundException: 
org.springframework.security.messaging.context.SecurityContextChannelInterceptor
```

#### **2. TaskScheduler Issue:**
```
java.lang.IllegalArgumentException: Heartbeat values configured but no TaskScheduler provided
org.springframework.context.ApplicationContextException: Failed to start bean 'simpleBrokerMessageHandler'
```

### 🔧 **Nguyên nhân và giải pháp:**

#### **Issue 1: Security Messaging Dependency**
**Nguyên nhân:** Thiếu dependency `spring-security-messaging` cho WebSocket security integration.

#### **Issue 2: TaskScheduler Missing**
**Nguyên nhân:** SimpleBrokerMessageHandler yêu cầu TaskScheduler khi có heartbeat configuration.

**Giải pháp đã thực hiện:**

#### 1. ✅ **Thêm Spring Security Messaging dependency:**
```xml
<!-- Spring Security Messaging for WebSocket -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-messaging</artifactId>
</dependency>
```

#### 2. ✅ **Simplified WebSocket Security Configuration:**
- Thay thế `AbstractSecurityWebSocketMessageBrokerConfigurer` (deprecated)
- Sử dụng `@EnableWebSocketSecurity` annotation
- Implement `AuthorizationManager` approach (Spring Security 6+)

#### 3. ✅ **Added TaskScheduler Configuration:**
```java
@Bean
public TaskScheduler messageBrokerTaskScheduler() {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(10);
    taskScheduler.setThreadNamePrefix("websocket-heartbeat-");
    taskScheduler.initialize();
    return taskScheduler;
}
```

#### 4. ✅ **Smart Heartbeat Configuration:**
```java
// Conditional heartbeat based on TaskScheduler availability
if (messageBrokerTaskScheduler != null) {
    registry.enableSimpleBroker("/topic", "/user")
            .setHeartbeatValue(new long[]{10000, 10000})
            .setTaskScheduler(messageBrokerTaskScheduler);
} else {
    registry.enableSimpleBroker("/topic", "/user");
}
```

#### 5. ✅ **Updated Files:**
- **pom.xml**: Added spring-security-messaging dependency
- **WebSocketSecurityConfig.java**: Simplified security configuration
- **WebSocketTaskSchedulerConfig.java**: TaskScheduler configuration
- **WebSocketConfig.java**: Smart heartbeat configuration
- **Backup files**: Lưu phiên bản cũ để tham khảo

## 🚀 **Cách test fix:**

### **Bước 1: Clean và rebuild project**
```bash
mvn clean install
```

### **Bước 2: Start application**
```bash
mvn spring-boot:run
```

### **Bước 3: Check logs để confirm WebSocket configuration**
Tìm logs như:
```
✅ STOMP endpoint '/ws' configured with SockJS fallback
✅ Message broker configured: Simple broker: /topic, /user
✅ Inbound channel configured with JWT interceptor
```

### **Bước 4: Test WebSocket connection**
```javascript
const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',
    connectHeaders: {
        'Authorization': 'Bearer YOUR_JWT_TOKEN'
    }
});
client.activate();
```

## 🔍 **Debugging WebSocket Issues:**

### **1. Connection Issues:**
```bash
# Check if WebSocket endpoint is available
curl -i -N -H "Connection: Upgrade" \
     -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Version: 13" \
     -H "Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==" \
     http://localhost:8080/ws
```

### **2. JWT Authentication Issues:**
- Kiểm tra JWT token có valid không
- Verify token format: `Bearer <token>`
- Check JWT expiration date
- Validate JWT secret key trong application.yml

### **3. CORS Issues:**
- Verify CORS configuration trong WebSocketConfig
- Check browser console cho CORS errors
- Test với same-origin requests trước

### **4. Subscription Issues:**
- Check destination paths: `/topic/*`, `/user/*`
- Verify user permissions cho specific destinations
- Monitor server logs cho authorization failures

## 📊 **Application Logs để monitor:**

### **Successful Connection:**
```
🟢 WebSocket CONNECTED: User username (ID: 123) - Session: abc123
✅ WebSocket connection authenticated for user: username (ID: 123)
📋 WebSocket SUBSCRIBED: User username (ID: 123) to destination: /topic/portfolio
```

### **Authentication Failures:**
```
❌ Invalid or missing JWT token in WebSocket connection
❌ WebSocket authentication failed: JWT token expired
❌ User 123 not authorized for destination: /user/456/topic/private
```

### **Connection Issues:**
```
🔴 WebSocket DISCONNECTED: User username (ID: 123) - Session: abc123
❌ STOMP Error: Authentication required for subscription
```

## 🛠 **Common Fixes:**

### **Fix 1: JWT Token Issues**
```java
// Verify JWT token extraction
String authHeader = accessor.getFirstNativeHeader("Authorization");
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    String token = authHeader.substring(7);
    // Validate token...
}
```

### **Fix 2: CORS Configuration**
```java
// In WebSocketConfig.java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // For development
        .withSockJS();
```

### **Fix 3: Security Configuration**
```java
// Ensure proper message authorization
messages
    .simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
    .simpDestMatchers("/app/**").authenticated()
    .anyMessage().authenticated();
```

## 🔐 **Security Best Practices:**

### **Production Considerations:**
1. **CORS**: Restrict allowed origins trong production
2. **JWT**: Use secure secret keys và proper expiration
3. **Rate Limiting**: Implement message rate limiting
4. **Monitoring**: Log all authentication attempts
5. **Encryption**: Use WSS (WebSocket over TLS) trong production

### **Development vs Production:**
```java
// Development - Allow all origins
.setAllowedOriginPatterns("*")

// Production - Specific origins only
.setAllowedOrigins("https://yourdomain.com", "https://app.yourdomain.com")
```

## ✅ **Status Check:**

- ✅ **Dependencies**: spring-security-messaging added
- ✅ **Configuration**: WebSocket security simplified
- ✅ **JWT Auth**: Working với interceptor
- ✅ **CORS**: Configured cho development
- ✅ **Testing**: Test endpoints available
- ✅ **Documentation**: Client guide provided

**WebSocket configuration đã được fix và sẵn sàng sử dụng!** 🎉

## 📞 **Need Help?**

Nếu vẫn gặp issues:
1. Check application logs chi tiết
2. Verify JWT token validity
3. Test với simple WebSocket client trước
4. Review WEBSOCKET_CLIENT_GUIDE.md cho examples
5. Check dependency conflicts với `mvn dependency:tree`