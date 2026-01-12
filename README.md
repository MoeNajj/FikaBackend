# Frontend API Integration Guide

## 🔗 Base Configuration

### API Base URL
```
Development: http://localhost:8080
Production: https://your-production-domain.com
```

### Default Port
- **8080** (Spring Boot default)

### Content Type
- All POST/PUT requests: `application/json`
- All responses: `application/json`

---

## 🔐 Authentication Flow

### Overview
The API uses **JWT (JSON Web Tokens)** for authentication with access tokens and refresh tokens.

### Authentication Flow Diagram
```
1. Login → Get accessToken + refreshToken
2. Store tokens securely
3. Include accessToken in Authorization header for protected endpoints
4. When accessToken expires → Use refreshToken to get new tokens
5. Logout → Revoke refreshToken
```

---

## 📝 TypeScript Types

```typescript
// Auth Types
interface LoginRequest {
  username: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  expiresIn: number; // seconds
  user: {
    id: number;
    username: string;
    email: string;
    roles: string[];
  };
}

interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  createdAt: string; // ISO 8601
  lastLogin?: string; // ISO 8601
}

interface RefreshTokenRequest {
  refreshToken: string;
}

// Parking Types
interface ParkingSpotRequest {
  spotId: number;
  locationId: number;
  rfidHash: string;
  userUuid: string; // UUID format
}

interface ParkingSpot {
  spotId: number;
  locationId: number;
  rfidHash?: string;
  userUuid?: string;
  isOccupied: boolean;
  loginTime?: string; // ISO 8601
  lastUpdate: string; // ISO 8601
}

interface ToggleResponse {
  action: "PARKED" | "VACATED";
  message: string;
  parkingSpot: ParkingSpot;
}

interface ErrorResponse {
  status: number;
  message: string;
  timestamp?: string;
  path?: string;
}
```

---

## 🚀 API Endpoints

### 1. Authentication Endpoints

#### POST `/api/auth/login`
**Authenticate user and receive tokens**

**Request:**
```typescript
const login = async (username: string, password: string): Promise<AuthResponse> => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw new Error(error.message || 'Login failed');
  }

  return response.json();
};

// Usage
const authData = await login('user@example.com', 'SecurePassword123!');
// Store tokens securely
localStorage.setItem('accessToken', authData.accessToken);
localStorage.setItem('refreshToken', authData.refreshToken);
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "user@example.com",
    "email": "user@example.com",
    "roles": ["USER"]
  }
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid credentials
- `400 Bad Request` - Missing fields

---

#### POST `/api/auth/register`
**Register a new user account**

**Request:**
```typescript
const register = async (data: RegisterRequest): Promise<AuthResponse> => {
  const response = await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw new Error(error.message || 'Registration failed');
  }

  return response.json();
};

// Usage
const authData = await register({
  username: 'newuser',
  email: 'newuser@example.com',
  password: 'SecurePassword123!',
  firstName: 'John',
  lastName: 'Doe',
});
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 2,
    "username": "newuser",
    "email": "newuser@example.com",
    "roles": ["USER"]
  }
}
```

**Error Responses:**
- `400 Bad Request` - Validation errors
- `409 Conflict` - Username/email already exists

---

#### POST `/api/auth/refresh`
**Refresh access token using refresh token**

**Request:**
```typescript
const refreshToken = async (refreshToken: string): Promise<AuthResponse> => {
  const response = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw new Error(error.message || 'Token refresh failed');
  }

  const data = await response.json();
  // Update stored tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  return data;
};

// Usage
const newTokens = await refreshToken(storedRefreshToken);
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or expired refresh token

---

#### GET `/api/auth/me`
**Get current authenticated user information**

**Request:**
```typescript
const getCurrentUser = async (): Promise<UserResponse> => {
  const accessToken = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/auth/me', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      // Token expired, try to refresh
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await refreshToken(refreshToken);
        return getCurrentUser(); // Retry
      }
    }
    throw new Error('Failed to get user info');
  }

  return response.json();
};
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "user@example.com",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "createdAt": "2024-01-15T10:30:00",
  "lastLogin": "2024-01-20T14:22:00"
}
```

---

#### POST `/api/auth/logout`
**Logout user and invalidate refresh token**

**Request:**
```typescript
const logout = async (): Promise<void> => {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');

  await fetch('http://localhost:8080/api/auth/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  // Clear tokens from storage
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};
```

**Response (200 OK):**
```json
{
  "message": "Successfully logged out"
}
```

---

### 2. Parking Endpoints

#### POST `/api/parking/toggle`
**Toggle parking spot status (park or vacate)**

**Request:**
```typescript
const toggleParkingSpot = async (request: ParkingSpotRequest): Promise<ToggleResponse> => {
  const accessToken = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/parking/toggle', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw new Error(error.message || 'Failed to toggle parking spot');
  }

  return response.json();
};

// Usage
const result = await toggleParkingSpot({
  spotId: 5,
  locationId: 1,
  rfidHash: 'MAX-TEST-TAG-6666',
  userUuid: 'a1b2c3d4-e5f6-7890-5555-567890abcdef',
});

console.log(result.action); // "PARKED" or "VACATED"
```

**Response (200 OK):**
```json
{
  "action": "PARKED",
  "message": "Bike successfully parked.",
  "parkingSpot": {
    "spotId": 5,
    "locationId": 1,
    "rfidHash": "MAX-TEST-TAG-6666",
    "userUuid": "a1b2c3d4-e5f6-7890-5555-567890abcdef",
    "isOccupied": true,
    "loginTime": "2024-01-20T14:30:00",
    "lastUpdate": "2024-01-20T14:30:00"
  }
}
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - Parking spot not found
- `409 Conflict` - Spot occupied by different user

---

#### POST `/api/parking/toggle/nfc`
**Toggle parking spot status via NFC (rfidHash auto-generated)**

**Request:**
```typescript
interface NfcParkingRequest {
  spotId: number;
  locationId: number;
  userUuid: string; // UUID format
}

const toggleParkingSpotNfc = async (request: NfcParkingRequest): Promise<ToggleResponse> => {
  const accessToken = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/parking/toggle/nfc', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: ErrorResponse = await response.json();
    throw new Error(error.message || 'Failed to toggle parking spot');
  }

  return response.json();
};

// Usage
const result = await toggleParkingSpotNfc({
  spotId: 5,
  locationId: 1,
  userUuid: 'a1b2c3d4-e5f6-7890-5555-567890abcdef',
});

console.log(result.statusMessage); // "Bike successfully parked." or "Bike successfully vacated..."
```

**Response (200 OK):**
```json
{
  "statusMessage": "Bike successfully parked.",
  "isOccupied": true,
  "spotDetails": {
    "spotId": 5,
    "locationId": 1,
    "rfidHash": "NFC-a1b2c3d4-e5f6-7890-5555-567890abcdef-20240120143000",
    "userUuid": "a1b2c3d4-e5f6-7890-5555-567890abcdef",
    "isOccupied": true,
    "loginTime": "2024-01-20T14:30:00",
    "lastUpdate": "2024-01-20T14:30:00"
  }
}
```

**Key Differences from Regular Toggle:**
- ✅ **No `rfidHash` required** in request body
- ✅ **rfidHash auto-generated** by backend in format: `NFC-{userUuid}-{timestamp}`
- ✅ **Simpler request** - only needs `spotId`, `locationId`, and `userUuid`
- ✅ **Same response format** as regular toggle endpoint

**NFC Tag Format:**
NFC tags can contain JSON data in NDEF text format:
```json
{
  "locationId": 1,
  "spotId": 5,
  "type": "parking"
}
```

Or simpler comma-separated format:
```
locationId:1,spotId:5
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - Parking spot not found
- `409 Conflict` - Spot occupied by different user

---

#### GET `/api/parking/status`
**Get parking spot status**

**Request:**
```typescript
const getParkingSpots = async (
  locationId?: number,
  spotIds?: number[]
): Promise<ParkingSpot[]> => {
  const accessToken = localStorage.getItem('accessToken');
  
  let url = 'http://localhost:8080/api/parking/status?';
  const params = new URLSearchParams();
  
  if (locationId) params.append('locationId', locationId.toString());
  if (spotIds) {
    spotIds.forEach(id => params.append('spotIds', id.toString()));
  }
  
  url += params.toString();

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    if (response.status === 404) {
      return []; // No spots found
    }
    throw new Error('Failed to get parking spots');
  }

  return response.json();
};

// Usage examples:
// Get all spots
const allSpots = await getParkingSpots();

// Get spots at location 1
const locationSpots = await getParkingSpots(1);

// Get specific spots
const specificSpots = await getParkingSpots(1, [1, 2, 5]);
```

**Response (200 OK):**
```json
[
  {
    "spotId": 1,
    "locationId": 1,
    "rfidHash": "ABC-123-TAG",
    "userUuid": "123e4567-e89b-12d3-a456-426614174000",
    "isOccupied": true,
    "loginTime": "2024-01-20T10:00:00",
    "lastUpdate": "2024-01-20T10:00:00"
  },
  {
    "spotId": 2,
    "locationId": 1,
    "rfidHash": null,
    "userUuid": null,
    "isOccupied": false,
    "loginTime": null,
    "lastUpdate": "2024-01-20T09:30:00"
  }
]
```

**Query Parameters:**
- `locationId` (optional) - Filter by location
- `spotIds` (optional) - Filter specific spots (can be multiple: `?spotIds=1&spotIds=2`)

---

## 🛠️ Complete API Client Example

```typescript
class BikeParkingAPI {
  private baseURL: string;

  constructor(baseURL: string = 'http://localhost:8080') {
    this.baseURL = baseURL;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = localStorage.getItem('accessToken');
    
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${this.baseURL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      if (response.status === 401) {
        // Try to refresh token
        await this.refreshAccessToken();
        // Retry request
        return this.request<T>(endpoint, options);
      }
      
      const error: ErrorResponse = await response.json().catch(() => ({
        status: response.status,
        message: response.statusText,
      }));
      throw new Error(error.message);
    }

    return response.json();
  }

  // Auth methods
  async login(username: string, password: string): Promise<AuthResponse> {
    const data = await this.request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data;
  }

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await this.request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    return response;
  }

  async refreshAccessToken(): Promise<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const data = await this.request<AuthResponse>('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
    
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data;
  }

  async getCurrentUser(): Promise<UserResponse> {
    return this.request<UserResponse>('/api/auth/me');
  }

  async logout(): Promise<void> {
    const refreshToken = localStorage.getItem('refreshToken');
    await this.request('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
    
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }

  // Parking methods
  async toggleParkingSpot(request: ParkingSpotRequest): Promise<ToggleResponse> {
    return this.request<ToggleResponse>('/api/parking/toggle', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async toggleParkingSpotNfc(request: { spotId: number; locationId: number; userUuid: string }): Promise<ToggleResponse> {
    return this.request<ToggleResponse>('/api/parking/toggle/nfc', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getParkingSpots(
    locationId?: number,
    spotIds?: number[]
  ): Promise<ParkingSpot[]> {
    const params = new URLSearchParams();
    if (locationId) params.append('locationId', locationId.toString());
    if (spotIds) {
      spotIds.forEach(id => params.append('spotIds', id.toString()));
    }
    
    const query = params.toString();
    return this.request<ParkingSpot[]>(
      `/api/parking/status${query ? `?${query}` : ''}`
    );
  }
}

// Usage
const api = new BikeParkingAPI('http://localhost:8080');

// Login
await api.login('user@example.com', 'password123');

// Get user info
const user = await api.getCurrentUser();

// Get parking spots
const spots = await api.getParkingSpots(1);

// Toggle spot (regular)
await api.toggleParkingSpot({
  spotId: 5,
  locationId: 1,
  rfidHash: 'TAG-123',
  userUuid: user.id.toString(),
});

// Toggle spot (NFC - rfidHash auto-generated)
await api.toggleParkingSpotNfc({
  spotId: 5,
  locationId: 1,
  userUuid: user.id.toString(),
});
```

---

## 🔒 Token Management Best Practices

### Storage
```typescript
// ✅ Good: Use secure storage
// For web apps: httpOnly cookies (server-side) or memory
// For mobile: Secure storage (Keychain/Keystore)

// ⚠️ Acceptable for development: localStorage
localStorage.setItem('accessToken', token);

// ❌ Bad: Never store in plain text files or unencrypted storage
```

### Automatic Token Refresh
```typescript
// Intercept 401 responses and refresh token
const apiCall = async () => {
  try {
    return await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  } catch (error) {
    if (error.status === 401) {
      await refreshToken();
      // Retry original request
      return await fetch(url, { headers: { Authorization: `Bearer ${newToken}` } });
    }
    throw error;
  }
};
```

---

## ⚠️ Error Handling

### Standard Error Response Format
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2024-01-20T14:30:00",
  "path": "/api/auth/login"
}
```

### HTTP Status Codes
- `200 OK` - Success
- `201 Created` - Resource created (registration)
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Invalid/missing token
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., user exists)

### Error Handling Example
```typescript
try {
  const result = await api.toggleParkingSpot(request);
} catch (error) {
  if (error.message.includes('401')) {
    // Token expired, redirect to login
    router.push('/login');
  } else if (error.message.includes('409')) {
    // Conflict - spot occupied
    alert('This spot is already occupied by another user');
  } else {
    // Generic error
    alert('An error occurred: ' + error.message);
  }
}
```

---

## 📱 React Hook Example

```typescript
import { useState, useEffect } from 'react';

const useAuth = () => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const userData = await api.getCurrentUser();
        setUser(userData);
      } catch (error) {
        // Not authenticated
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    loadUser();
  }, []);

  const login = async (username: string, password: string) => {
    const authData = await api.login(username, password);
    const userData = await api.getCurrentUser();
    setUser(userData);
    return authData;
  };

  const logout = async () => {
    await api.logout();
    setUser(null);
  };

  return { user, loading, login, logout };
};
```

---

## 🌐 Axios Example

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor - handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            'http://localhost:8080/api/auth/refresh',
            { refreshToken }
          );
          localStorage.setItem('accessToken', data.accessToken);
          localStorage.setItem('refreshToken', data.refreshToken);
          // Retry original request
          return apiClient.request(error.config);
        } catch {
          // Refresh failed, redirect to login
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

// Usage
const spots = await apiClient.get('/api/parking/status', {
  params: { locationId: 1 }
});
```

---

## 🔗 Quick Reference

### Public Endpoints (No Auth Required)
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`

### Protected Endpoints (Auth Required)
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `POST /api/parking/toggle`
- `POST /api/parking/toggle/nfc`
- `GET /api/parking/status`

### Headers Required
```typescript
// All requests
'Content-Type': 'application/json'

// Protected requests
'Authorization': 'Bearer <accessToken>'
```

---

## 📞 Support

For issues or questions:
1. Check error response messages
2. Verify token is valid and not expired
3. Ensure base URL is correct
4. Check network connectivity

---

**Last Updated:** 2024-01-20

