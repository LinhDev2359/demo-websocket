# 🔌 WEBSOCKET CLIENT CONNECTION GUIDE

## WebSocket Configuration đã hoàn thành!

### ✅ **Server-side Components:**
- ✅ **WebSocketConfig**: STOMP configuration với message broker
- ✅ **WebSocketJwtAuthInterceptor**: JWT authentication cho WebSocket
- ✅ **WebSocketUserPrincipal**: Custom principal với user info
- ✅ **WebSocketSecurityConfig**: Security policies cho messages
- ✅ **WebSocketEventListener**: Event monitoring và logging
- ✅ **WebSocketTestController**: Test endpoints cho validation

## 🚀 **Client Connection Examples:**

### **JavaScript/TypeScript Client:**

```javascript
// 1. Include SockJS và STOMP libraries
// <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
// <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>

// 2. Create WebSocket connection với JWT authentication
const jwtToken = "your-jwt-token-here";

const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',
    connectHeaders: {
        'Authorization': 'Bearer ' + jwtToken
    },
    debug: function (str) {
        console.log('STOMP: ' + str);
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
});

// 3. Handle connection events
client.onConnect = function (frame) {
    console.log('✅ WebSocket Connected:', frame);
    
    // Subscribe to portfolio updates
    client.subscribe('/topic/portfolio/updates', function (message) {
        const portfolioData = JSON.parse(message.body);
        console.log('📊 Portfolio update:', portfolioData);
    });
    
    // Subscribe to user-specific notifications
    client.subscribe('/user/topic/notifications', function (message) {
        const notification = JSON.parse(message.body);
        console.log('🔔 Personal notification:', notification);
    });
    
    // Test connection
    client.publish({
        destination: '/app/test.ping',
        body: JSON.stringify({ message: 'Hello from client!' })
    });
};

client.onDisconnect = function () {
    console.log('🔴 WebSocket Disconnected');
};

client.onStompError = function (frame) {
    console.error('❌ STOMP Error:', frame);
};

// 4. Activate connection
client.activate();
```

### **React Hook Example:**

```typescript
import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface UseWebSocketProps {
    jwtToken: string;
    onPortfolioUpdate?: (data: any) => void;
    onNotification?: (data: any) => void;
}

export const useWebSocket = ({ 
    jwtToken, 
    onPortfolioUpdate, 
    onNotification 
}: UseWebSocketProps) => {
    const [client, setClient] = useState<Client | null>(null);
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        const stompClient = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
            connectHeaders: {
                'Authorization': `Bearer ${jwtToken}`
            },
            debug: (str) => console.log('STOMP:', str),
            onConnect: () => {
                console.log('✅ WebSocket Connected');
                setConnected(true);

                // Subscribe to topics
                if (onPortfolioUpdate) {
                    stompClient.subscribe('/topic/portfolio/updates', (message) => {
                        onPortfolioUpdate(JSON.parse(message.body));
                    });
                }

                if (onNotification) {
                    stompClient.subscribe('/user/topic/notifications', (message) => {
                        onNotification(JSON.parse(message.body));
                    });
                }
            },
            onDisconnect: () => {
                console.log('🔴 WebSocket Disconnected');
                setConnected(false);
            },
            onStompError: (frame) => {
                console.error('❌ STOMP Error:', frame);
                setConnected(false);
            }
        });

        stompClient.activate();
        setClient(stompClient);

        return () => {
            if (stompClient.active) {
                stompClient.deactivate();
            }
        };
    }, [jwtToken]);

    const sendMessage = (destination: string, body: any) => {
        if (client && connected) {
            client.publish({
                destination,
                body: JSON.stringify(body)
            });
        }
    };

    return { client, connected, sendMessage };
};
```

## 📋 **Available Endpoints:**

### **Subscription Destinations:**
- `/topic/portfolio/updates` - Portfolio real-time updates
- `/topic/notifications` - Global notifications
- `/user/topic/notifications` - Personal notifications
- `/user/topic/private` - Private messages

### **Message Destinations:**
- `/app/test.message` - Test broadcast message
- `/app/test.private` - Test private message
- `/app/test.ping` - Health check ping

### **Future Endpoints (TASK 4.2):**
- `/app/portfolio.subscribe` - Subscribe to portfolio updates
- `/app/portfolio.unsubscribe` - Unsubscribe from updates
- `/topic/portfolio/{userId}` - User-specific portfolio stream

## 🔧 **Testing WebSocket:**

### **1. Browser Console Test:**
```javascript
// Open browser console và run:
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);

client.connect({
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Test ping
    client.send('/app/test.ping', {}, JSON.stringify({
        message: 'ping from browser'
    }));
});
```

### **2. Postman/Insomnia WebSocket Test:**
- URL: `ws://localhost:8080/ws`
- Protocol: `sockjs`
- Headers: `Authorization: Bearer YOUR_JWT_TOKEN`

### **3. Command Line Test với wscat:**
```bash
# Install wscat
npm install -g wscat

# Connect to WebSocket
wscat -c ws://localhost:8080/ws -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔐 **Authentication Flow:**

1. **Get JWT Token:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"your-username","password":"your-password"}'
   ```

2. **Use Token in WebSocket:**
   ```javascript
   connectHeaders: {
       'Authorization': 'Bearer ' + jwtToken
   }
   ```

3. **Server validates token và creates WebSocketUserPrincipal**

4. **User-specific routing dựa trên user ID từ JWT**

## 🚨 **Security Features:**

- ✅ **JWT Authentication**: Required cho tất cả WebSocket operations
- ✅ **User Authorization**: Users chỉ access được own data
- ✅ **Destination Validation**: Server validate permissions
- ✅ **CORS Configuration**: Proper cross-origin handling
- ✅ **Session Management**: Automatic cleanup on disconnect

## 📊 **Monitoring:**

Server logs sẽ show:
- 🟢 Connection events
- 🔴 Disconnection events  
- 📋 Subscription activities
- 📨 Message routing
- ❌ Authentication failures

**WebSocket Configuration hoàn thành và sẵn sàng cho real-time communication!** 🎉