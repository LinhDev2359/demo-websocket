# 📡 Portfolio WebSocket Client Guide

## 🚀 Overview

This guide shows how to connect to the Portfolio WebSocket service for real-time portfolio data streaming.

## 🔗 WebSocket Connection

### Connection URL
```
ws://localhost:8080/ws
```

### Authentication
Include JWT token in connection headers:
```javascript
const headers = {
    'Authorization': 'Bearer your-jwt-token-here'
};
```

## 📡 Available Destinations

### 🔔 Subscription Destinations (Client Subscribes To)

| Destination | Description | Response Type |
|-------------|-------------|---------------|
| `/user/queue/portfolio` | User-specific portfolio updates | `PortfolioResponse` |
| `/user/queue/errors` | User-specific error messages | `WebSocketErrorResponse` |

### 📤 Message Destinations (Client Sends To)

| Destination | Description | Payload Type |
|-------------|-------------|--------------|
| `/app/portfolio/subscribe/{userId}` | Subscribe to portfolio updates | None |
| `/app/portfolio/update` | Request portfolio update | `PortfolioUpdateRequest` |
| `/app/portfolio/refresh` | Force refresh portfolio data | `userId` (String) |
| `/app/portfolio/unsubscribe/{userId}` | Unsubscribe from updates | None |

## 💡 Usage Examples

### 1. JavaScript/SockJS Client

```html
<!DOCTYPE html>
<html>
<head>
    <title>Portfolio WebSocket Client</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <div id="portfolio-data"></div>
    <div id="error-messages"></div>

    <script>
        class PortfolioWebSocketClient {
            constructor(serverUrl, jwtToken, userId) {
                this.serverUrl = serverUrl;
                this.jwtToken = jwtToken;
                this.userId = userId;
                this.stompClient = null;
                this.connected = false;
            }

            connect() {
                const socket = new SockJS(this.serverUrl);
                this.stompClient = Stomp.over(socket);
                
                // Set JWT token in headers
                const headers = {
                    'Authorization': 'Bearer ' + this.jwtToken
                };

                this.stompClient.connect(headers, 
                    (frame) => this.onConnected(frame),
                    (error) => this.onError(error)
                );
            }

            onConnected(frame) {
                console.log('Connected to Portfolio WebSocket:', frame);
                this.connected = true;

                // Subscribe to user-specific portfolio updates
                this.stompClient.subscribe('/user/queue/portfolio', (message) => {
                    const portfolio = JSON.parse(message.body);
                    this.handlePortfolioUpdate(portfolio);
                });

                // Subscribe to error messages
                this.stompClient.subscribe('/user/queue/errors', (message) => {
                    const error = JSON.parse(message.body);
                    this.handleError(error);
                });

                // Subscribe to portfolio updates for this user
                this.subscribeToPortfolio();
            }

            subscribeToPortfolio() {
                if (this.connected) {
                    this.stompClient.send(`/app/portfolio/subscribe/${this.userId}`, {}, '');
                    console.log('Subscribed to portfolio updates for user:', this.userId);
                }
            }

            requestPortfolioUpdate(refreshCache = false) {
                if (this.connected) {
                    const request = {
                        user_id: this.userId,
                        refresh_cache: refreshCache,
                        subscription_type: 'real_time'
                    };
                    this.stompClient.send('/app/portfolio/update', {}, JSON.stringify(request));
                    console.log('Portfolio update requested');
                }
            }

            refreshPortfolio() {
                if (this.connected) {
                    this.stompClient.send('/app/portfolio/refresh', {}, this.userId);
                    console.log('Portfolio refresh requested');
                }
            }

            handlePortfolioUpdate(portfolio) {
                console.log('Portfolio Update Received:', portfolio);
                
                // Update UI with portfolio data
                const portfolioDiv = document.getElementById('portfolio-data');
                portfolioDiv.innerHTML = `
                    <h3>Portfolio for ${portfolio.username}</h3>
                    <p>Total Wallets: ${portfolio.total_wallets}</p>
                    <p>Total EOS: ${portfolio.total_balance.total_eos}</p>
                    <p>Total RAM: ${portfolio.total_balance.total_ram}</p>
                    <p>Last Updated: ${portfolio.last_updated}</p>
                    
                    <h4>Wallets:</h4>
                    <ul>
                        ${portfolio.wallets.map(wallet => `
                            <li>
                                <strong>${wallet.wallet_address}</strong> (${wallet.wallet_type})
                                ${wallet.is_primary ? ' [PRIMARY]' : ''}
                                <ul>
                                    ${wallet.balances.map(balance => `
                                        <li>${balance.token_type}: ${balance.balance}</li>
                                    `).join('')}
                                </ul>
                            </li>
                        `).join('')}
                    </ul>
                `;
            }

            handleError(error) {
                console.error('Portfolio WebSocket Error:', error);
                
                const errorDiv = document.getElementById('error-messages');
                errorDiv.innerHTML += `
                    <div style="color: red; border: 1px solid red; padding: 10px; margin: 5px;">
                        <strong>Error ${error.error_code}:</strong> ${error.error_message}
                        <br><small>${error.timestamp}</small>
                    </div>
                `;
            }

            onError(error) {
                console.error('WebSocket Connection Error:', error);
                this.connected = false;
            }

            disconnect() {
                if (this.stompClient !== null) {
                    this.stompClient.send(`/app/portfolio/unsubscribe/${this.userId}`, {}, '');
                    this.stompClient.disconnect();
                    this.connected = false;
                    console.log('Disconnected from Portfolio WebSocket');
                }
            }
        }

        // Usage Example
        const jwtToken = 'your-jwt-token-here';
        const userId = 'user123';
        const client = new PortfolioWebSocketClient('http://localhost:8080/ws', jwtToken, userId);

        // Connect to WebSocket
        client.connect();

        // Request updates periodically
        setInterval(() => {
            if (client.connected) {
                client.requestPortfolioUpdate();
            }
        }, 30000); // Every 30 seconds
    </script>
</body>
</html>
```

### 2. Node.js Client

```javascript
const StompJs = require('@stomp/stompjs');
const WebSocket = require('ws');

// Assign ws to global for StompJS
Object.assign(global, { WebSocket: WebSocket });

class NodePortfolioWebSocketClient {
    constructor(serverUrl, jwtToken, userId) {
        this.serverUrl = serverUrl;
        this.jwtToken = jwtToken;
        this.userId = userId;
        this.client = null;
    }

    connect() {
        this.client = new StompJs.Client({
            brokerURL: this.serverUrl,
            connectHeaders: {
                'Authorization': 'Bearer ' + this.jwtToken
            },
            debug: function (str) {
                console.log('STOMP: ' + str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = (frame) => {
            console.log('Connected to Portfolio WebSocket');

            // Subscribe to portfolio updates
            this.client.subscribe('/user/queue/portfolio', (message) => {
                const portfolio = JSON.parse(message.body);
                console.log('Portfolio Update:', JSON.stringify(portfolio, null, 2));
            });

            // Subscribe to errors
            this.client.subscribe('/user/queue/errors', (message) => {
                const error = JSON.parse(message.body);
                console.error('Portfolio Error:', error);
            });

            // Subscribe to portfolio for this user
            this.subscribeToPortfolio();
        };

        this.client.onStompError = (frame) => {
            console.error('STOMP Error:', frame.headers['message']);
            console.error('Details:', frame.body);
        };

        this.client.activate();
    }

    subscribeToPortfolio() {
        this.client.publish({
            destination: `/app/portfolio/subscribe/${this.userId}`,
            body: ''
        });
    }

    requestPortfolioUpdate(refreshCache = false) {
        const request = {
            user_id: this.userId,
            refresh_cache: refreshCache,
            subscription_type: 'real_time'
        };

        this.client.publish({
            destination: '/app/portfolio/update',
            body: JSON.stringify(request)
        });
    }

    disconnect() {
        if (this.client) {
            this.client.publish({
                destination: `/app/portfolio/unsubscribe/${this.userId}`,
                body: ''
            });
            this.client.deactivate();
        }
    }
}

// Usage
const client = new NodePortfolioWebSocketClient(
    'ws://localhost:8080/ws',
    'your-jwt-token-here',
    'user123'
);

client.connect();

// Request updates every 30 seconds
setInterval(() => {
    client.requestPortfolioUpdate();
}, 30000);
```

### 3. Python Client

```python
import asyncio
import json
import websockets
from stomper import stompws

class PortfolioWebSocketClient:
    def __init__(self, url, jwt_token, user_id):
        self.url = url
        self.jwt_token = jwt_token
        self.user_id = user_id
        self.websocket = None
        self.stomp = stompws.StompWebSocket()

    async def connect(self):
        headers = {'Authorization': f'Bearer {self.jwt_token}'}
        
        try:
            self.websocket = await websockets.connect(
                self.url.replace('http', 'ws'),
                extra_headers=headers
            )
            
            # Send CONNECT frame
            connect_frame = self.stomp.connect(
                host='localhost',
                headers={'Authorization': f'Bearer {self.jwt_token}'}
            )
            await self.websocket.send(connect_frame)
            
            # Start listening for messages
            await self.listen_for_messages()
            
        except Exception as e:
            print(f"Connection error: {e}")

    async def listen_for_messages(self):
        async for message in self.websocket:
            try:
                frame = self.stomp.parse_frame(message)
                
                if frame['command'] == 'CONNECTED':
                    print("Connected to Portfolio WebSocket")
                    await self.subscribe_to_portfolio()
                    
                elif frame['command'] == 'MESSAGE':
                    await self.handle_message(frame)
                    
            except Exception as e:
                print(f"Message handling error: {e}")

    async def subscribe_to_portfolio(self):
        # Subscribe to portfolio updates
        subscribe_frame = self.stomp.subscribe(
            destination='/user/queue/portfolio',
            subscription_id='portfolio-updates'
        )
        await self.websocket.send(subscribe_frame)
        
        # Subscribe to errors
        error_subscribe_frame = self.stomp.subscribe(
            destination='/user/queue/errors',
            subscription_id='error-messages'
        )
        await self.websocket.send(error_subscribe_frame)
        
        # Request initial subscription
        send_frame = self.stomp.send(
            destination=f'/app/portfolio/subscribe/{self.user_id}',
            body=''
        )
        await self.websocket.send(send_frame)

    async def handle_message(self, frame):
        destination = frame['headers'].get('destination', '')
        body = frame.get('body', '')
        
        if '/user/queue/portfolio' in destination:
            portfolio = json.loads(body)
            print(f"Portfolio Update: {json.dumps(portfolio, indent=2)}")
            
        elif '/user/queue/errors' in destination:
            error = json.loads(body)
            print(f"Portfolio Error: {error}")

    async def request_portfolio_update(self, refresh_cache=False):
        request = {
            'user_id': self.user_id,
            'refresh_cache': refresh_cache,
            'subscription_type': 'real_time'
        }
        
        send_frame = self.stomp.send(
            destination='/app/portfolio/update',
            body=json.dumps(request)
        )
        await self.websocket.send(send_frame)

    async def disconnect(self):
        if self.websocket:
            # Unsubscribe
            send_frame = self.stomp.send(
                destination=f'/app/portfolio/unsubscribe/{self.user_id}',
                body=''
            )
            await self.websocket.send(send_frame)
            
            # Disconnect
            disconnect_frame = self.stomp.disconnect()
            await self.websocket.send(disconnect_frame)
            await self.websocket.close()

# Usage
async def main():
    client = PortfolioWebSocketClient(
        'ws://localhost:8080/ws',
        'your-jwt-token-here',
        'user123'
    )
    
    await client.connect()

# Run the client
asyncio.run(main())
```

## 📋 Message Formats

### PortfolioUpdateRequest
```json
{
    "user_id": "user123",
    "refresh_cache": false,
    "include_inactive_wallets": false,
    "wallet_address_filter": null,
    "subscription_type": "real_time"
}
```

### PortfolioResponse
```json
{
    "user_id": "user123",
    "username": "john_doe",
    "total_wallets": 2,
    "wallets": [
        {
            "wallet_id": 1,
            "wallet_address": "eoswalletabc",
            "wallet_type": "EOS",
            "balances": [
                {
                    "token_type": "A",
                    "balance": "100.50000000",
                    "last_updated": "2024-01-15 10:30:00"
                }
            ],
            "is_primary": true,
            "created_at": "2024-01-10 09:00:00"
        }
    ],
    "total_balance": {
        "total_eos": "150.75000000",
        "total_ram": "1024.00000000",
        "total_rams": "512.00000000",
        "total_wram": "256.00000000"
    },
    "last_updated": "2024-01-15 10:30:00",
    "message_type": "PORTFOLIO_UPDATE"
}
```

### WebSocketErrorResponse
```json
{
    "message_type": "ERROR",
    "error_code": "UNAUTHORIZED",
    "error_message": "Authentication failed",
    "error_details": null,
    "timestamp": "2024-01-15 10:30:00",
    "user_id": "user123",
    "request_id": null
}
```

## 🚨 Error Handling

### Common Error Codes

| Error Code | Description | Solution |
|------------|-------------|----------|
| `UNAUTHORIZED` | Authentication failed | Check JWT token validity |
| `ACCESS_DENIED` | Access denied to resource | Verify user permissions |
| `INVALID_REQUEST` | Invalid request format | Check request payload |
| `SERVICE_UNAVAILABLE` | Service temporarily unavailable | Retry after delay |
| `PORTFOLIO_NOT_FOUND` | Portfolio not found for user | Check if user has wallets |
| `RATE_LIMIT_EXCEEDED` | Rate limit exceeded | Reduce request frequency |

## 📊 Best Practices

### 1. Connection Management
- Always include JWT token in connection headers
- Implement reconnection logic for dropped connections
- Handle connection state properly

### 2. Subscription Management
- Subscribe only to needed destinations
- Unsubscribe when no longer needed
- Track subscription state

### 3. Error Handling
- Subscribe to error queue for user-specific errors
- Implement proper error handling for all scenarios
- Log errors for debugging

### 4. Performance
- Don't request updates too frequently
- Use `refresh_cache: false` for regular updates
- Use `refresh_cache: true` only when necessary

### 5. Security
- Keep JWT tokens secure
- Validate user permissions
- Don't expose sensitive data in logs

## 🔄 Real-time Updates

The service automatically sends portfolio updates:
- Every 30 seconds for active subscriptions
- When wallet balances change
- When new wallets are added
- When portfolio data is refreshed

## 📞 Support

For issues or questions about the Portfolio WebSocket service:
1. Check server logs for error details
2. Verify JWT token is valid and not expired
3. Ensure user has proper permissions
4. Check network connectivity
5. Review this guide for proper usage