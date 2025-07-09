# 📋 Portfolio WebSocket API Guide

## 🎯 **Tổng Quan**

WebSocket API mới cho phép client lấy danh sách portfolio với pagination và real-time updates.

## 🔧 **Endpoints**

### **1. Get Portfolio List (MỚI)**
```
Destination: /app/portfolio/list
Response: /user/queue/portfolio-list
```

**Request Format:**
```json
{
  "user_id": "user123",
  "page": 0,
  "size": 20,
  "refresh_cache": false,
  "include_balances": true,
  "include_inactive": false,
  "min_balance_usd": 0.0,
  "token_filter": "EOS",
  "wallet_type_filter": "EOS"
}
```

**Response Format:**
```json
{
  "message_type": "PORTFOLIO_LIST",
  "user_id": "user123",
  "username": "john_doe",
  "portfolios": [
    {
      "wallet_id": 1,
      "wallet_address": "testwallet123",
      "wallet_name": "My Main Wallet",
      "wallet_type": "EOS",
      "is_primary": true,
      "status": "ACTIVE",
      "balances": [
        {
          "balance_id": 1,
          "token_symbol": "EOS",
          "token_name": "EOS",
          "token_contract": "eosio.token",
          "balance": 100.5000,
          "available_balance": 90.5000,
          "locked_balance": 10.0000,
          "staked_balance": 0.0000,
          "usd_value": 150.75,
          "token_price_usd": 1.50,
          "price_change_24h": 2.5,
          "is_native_token": true,
          "decimal_places": 4,
          "last_updated": "2024-01-20 10:30:00"
        }
      ],
      "total_balance_usd": 150.75,
      "total_tokens": 1,
      "last_updated": "2024-01-20 10:30:00",
      "created_at": "2024-01-15 08:00:00"
    }
  ],
  "pagination": {
    "current_page": 0,
    "page_size": 20,
    "total_pages": 1,
    "total_elements": 1,
    "has_next": false,
    "has_previous": false,
    "is_first": true,
    "is_last": true
  },
  "summary": {
    "total_wallets": 1,
    "active_wallets": 1,
    "total_balance_usd": 150.75,
    "total_tokens": 1,
    "primary_wallet_address": "testwallet123",
    "last_sync_time": "2024-01-20 10:30:00",
    "next_sync_time": "2024-01-20 10:35:00"
  },
  "timestamp": "2024-01-20 10:30:15",
  "cache_hit": false
}
```

### **2. Existing Endpoints**

#### Subscribe to Updates
```
Destination: /app/portfolio/subscribe/{userId}
Response: /user/queue/portfolio
```

#### Force Refresh
```
Destination: /app/portfolio/refresh
Payload: "user123"
Response: /user/queue/portfolio
```

#### Update Request
```
Destination: /app/portfolio/update
Payload: {"user_id": "user123", "refresh_cache": true}
Response: /user/queue/portfolio
```

## 🔄 **Luồng Sử Dụng**

### **1. Kết nối WebSocket**
```javascript
const socket = new WebSocket('ws://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// Connect với JWT
stompClient.connect(
  { 'Authorization': 'Bearer ' + jwtToken },
  function (frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to portfolio list responses
    stompClient.subscribe('/user/queue/portfolio-list', function (message) {
      const portfolioList = JSON.parse(message.body);
      console.log('Portfolio List:', portfolioList);
    });
    
    // Subscribe to errors
    stompClient.subscribe('/user/queue/errors', function (message) {
      const error = JSON.parse(message.body);
      console.log('Error:', error);
    });
  }
);
```

### **2. Request Portfolio List**
```javascript
// Basic request
stompClient.send('/app/portfolio/list', {}, JSON.stringify({
  "user_id": "user123"
}));

// Request with pagination
stompClient.send('/app/portfolio/list', {}, JSON.stringify({
  "user_id": "user123",
  "page": 0,
  "size": 10,
  "include_balances": true,
  "refresh_cache": false
}));

// Request with filters
stompClient.send('/app/portfolio/list', {}, JSON.stringify({
  "user_id": "user123",
  "page": 0,
  "size": 20,
  "include_balances": true,
  "min_balance_usd": 1.0,
  "token_filter": "EOS"
}));
```

### **3. Handle Responses**
```javascript
// Portfolio list response handler
stompClient.subscribe('/user/queue/portfolio-list', function (message) {
  const response = JSON.parse(message.body);
  
  if (response.message_type === 'PORTFOLIO_LIST') {
    // Update UI với portfolio data
    updatePortfolioTable(response.portfolios);
    updatePagination(response.pagination);
    updateSummary(response.summary);
  }
});

// Error handler
stompClient.subscribe('/user/queue/errors', function (message) {
  const error = JSON.parse(message.body);
  console.error('Portfolio API Error:', error.error_message);
  showErrorMessage(error.error_message);
});
```

## 📊 **Use Cases**

### **1. Portfolio Dashboard**
```javascript
// Load initial portfolio list
function loadPortfolio(page = 0) {
  stompClient.send('/app/portfolio/list', {}, JSON.stringify({
    "user_id": getCurrentUserId(),
    "page": page,
    "size": 20,
    "include_balances": true,
    "refresh_cache": false
  }));
}

// Refresh data
function refreshPortfolio() {
  stompClient.send('/app/portfolio/list', {}, JSON.stringify({
    "user_id": getCurrentUserId(),
    "refresh_cache": true
  }));
}
```

### **2. Pagination**
```javascript
function loadPage(pageNumber) {
  stompClient.send('/app/portfolio/list', {}, JSON.stringify({
    "user_id": getCurrentUserId(),
    "page": pageNumber,
    "size": 20,
    "include_balances": true
  }));
}
```

### **3. Filtering**
```javascript
function filterByToken(tokenSymbol) {
  stompClient.send('/app/portfolio/list', {}, JSON.stringify({
    "user_id": getCurrentUserId(),
    "token_filter": tokenSymbol,
    "min_balance_usd": 0.01
  }));
}
```

## 🔐 **Security**

- ✅ JWT Authentication required
- ✅ User authorization - chỉ được xem portfolio của mình
- ✅ Input validation
- ✅ Error handling với proper error codes

## ⚡ **Performance**

- ✅ Redis caching (5 phút cache)
- ✅ Pagination để tránh large responses
- ✅ Async processing
- ✅ Connection pooling
- ✅ Có thể filter để giảm data transfer

## 🆕 **Cải Tiến Mới**

### **1. WalletBalance Sync Logic**
- ✅ Tự động lưu vào `wallet_balances` table
- ✅ Track balance changes với previous/current amounts
- ✅ Sync status và timestamps
- ✅ Support cho balance history

### **2. Comprehensive Portfolio API**
- ✅ Pagination support
- ✅ Filtering options
- ✅ Summary information
- ✅ Rich response format
- ✅ Cache optimization

### **3. Real-time Updates**
- ✅ WebSocket cho instant updates
- ✅ Balance change notifications
- ✅ Error handling
- ✅ Connection monitoring

## 🎯 **Kết Luận**

API mới cung cấp:
- **Complete portfolio list** với pagination
- **Real-time updates** qua WebSocket
- **Proper WalletBalance sync** với database
- **Rich filtering** và search options
- **Production-ready** với caching và error handling

Hoàn toàn ready để client sử dụng! 🚀