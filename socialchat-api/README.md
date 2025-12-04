# SocialChat API

A robust RESTful API and real-time messaging backend built with Spring Boot. This service provides authentication, social networking features, real-time chat via WebSockets, and media management capabilities.

## Features

### Social Features
- User registration and authentication (JWT-based)
- User profiles with avatar and bio
- Posts with images, likes, and comments
- Friend requests and friend management
- User search functionality

### Real-Time Features
- Instant messaging with WebSocket
- Typing indicators
- Read receipts
- Online/offline presence status (friends only)
- Real-time notifications for likes, comments, and friend requests

## Tech Stack

### Backend
- **Java 17+** with **Spring Boot 3.x**
- **Spring Security 6** - Stateless JWT authentication
- **Spring WebSocket** - STOMP over SockJS for real-time features
- **Spring Data JPA** - Database access
- **MySQL** - Primary database
- **Flyway** - Database migrations
- **MapStruct** - DTO mapping
- **JJWT** - JWT token handling
- **Springdoc OpenAPI** - API documentation

## Project Structure

```
backend/
├── src/main/java/com/socialchat/
│   ├── config/          # Security, WebSocket, OpenAPI configs
│   ├── controller/      # REST API controllers
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA entities
│   ├── exception/       # Custom exceptions and handlers
│   ├── mapper/          # MapStruct mappers
│   ├── repository/      # Spring Data repositories
│   ├── security/        # JWT filter, service, utilities
│   ├── service/         # Business logic
│   └── websocket/       # WebSocket handlers
└── src/main/resources/
├── db/migration/    # Flyway migrations
└── application.yml  # Configuration
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd social-chat-application
   ```

2. **Configure database**
   ```bash
   mysql -u root -p
   CREATE DATABASE social_chat;
   ```

3. **Update application.yml or set environment variables**

4. **Run the application**
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

5. **Access API documentation**
   ```
   http://localhost:8080/swagger-ui.html
   ```

---

## Security Architecture

### JWT Payload Structure

```json
{
"jti": "unique-token-id",
"sub": "username",
"userId": 123,
"roles": ["ROLE_USER"],
"type": "access",
"iat": 1699999999,
"exp": 1700000000
}
```

| Claim | Description |
|-------|-------------|
| `jti` | Unique token ID (for revocation) |
| `sub` | Username |
| `userId` | User's database ID |
| `roles` | User roles list |
| `type` | `access` or `refresh` |

### Stateless Authentication
- **Zero database calls** per authenticated request
- All user info extracted from JWT claims
- Single JWT parsing per request via `JwtClaims` object
- Token revocation support via database blacklist

### WebSocket Security
- Conversation membership verified on SUBSCRIBE and SEND
- Rate limiting: 30 messages/minute per user per conversation
- Message validation: max 5000 characters
- Presence updates restricted to friends only

---

## API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | Login with credentials |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | Revoke tokens |
| POST | `/change-password` | Change password |

### Users (`/api/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/me` | Get current user profile |
| GET | `/{id}` | Get user by ID |
| GET | `/username/{username}` | Get user by username |
| PATCH | `/me` | Update profile |
| POST | `/me/photo` | Upload profile photo |
| DELETE | `/me` | Delete account |

### Friends (`/api/friends`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get friends list |
| POST | `/request/{userId}` | Send friend request |
| GET | `/requests/received` | Get received requests |
| GET | `/requests/sent` | Get sent requests |
| POST | `/requests/{id}/accept` | Accept request |
| POST | `/requests/{id}/reject` | Reject request |
| DELETE | `/{friendId}` | Remove friend |
| POST | `/block/{userId}` | Block user |
| DELETE | `/block/{userId}` | Unblock user |
| GET | `/relationship/{userId}` | Get relationship status |

### Chat (`/api/chat`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/conversations` | Create or get conversation |
| GET | `/conversations` | Get all conversations |
| GET | `/conversations/{id}` | Get conversation by ID |
| GET | `/conversations/{id}/messages` | Get messages (paginated) |
| POST | `/conversations/{id}/messages` | Send message (REST) |
| POST | `/conversations/{id}/read` | Mark as read |
| DELETE | `/conversations/{id}` | Leave conversation |

### Posts (`/api/posts`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create post (multipart) |
| GET | `/feed` | Get posts feed |
| GET | `/{id}` | Get post by ID |
| GET | `/user/{userId}` | Get user's posts |
| PATCH | `/{id}` | Update post |
| DELETE | `/{id}` | Delete post |
| POST | `/{id}/like` | Like post |
| DELETE | `/{id}/like` | Unlike post |
| POST | `/{id}/comments` | Add comment |
| GET | `/{id}/comments` | Get comments |
| DELETE | `/{postId}/comments/{commentId}` | Delete comment |

### Notifications (`/api/notifications`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get notifications |
| GET | `/unread-count` | Get unread count |
| POST | `/{id}/read` | Mark as read |
| POST | `/read-all` | Mark all as read |
| DELETE | `/{id}` | Delete notification |

### Presence (`/api/presence`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/friends` | Get online friends IDs |
| GET | `/user/{userId}` | Check user status (friends only) |

---

## WebSocket

### Connection

```
Endpoint: /ws (with SockJS fallback)
Protocol: STOMP
```

**Authentication**: Pass JWT in STOMP CONNECT headers:
```javascript
stompClient.connect({
'Authorization': 'Bearer <access_token>'
}, onConnected, onError);
```

### Subscribe Destinations (Client receives)

| Destination | Description |
|-------------|-------------|
| `/topic/conversations/{id}/messages` | New messages in conversation |
| `/topic/conversations/{id}/typing` | Typing indicators |
| `/topic/conversations/{id}/read-receipts` | Read receipts |
| `/user/queue/notifications` | Personal notifications |
| `/user/queue/presence` | Friends' presence updates |

### Send Destinations (Client sends)

| Destination | Payload | Description |
|-------------|---------|-------------|
| `/app/chat/{conversationId}/message` | `{ "content": "..." }` | Send message |
| `/app/chat/{conversationId}/typing` | `{ "typing": true }` | Typing indicator |
| `/app/chat/{conversationId}/read` | `{}` | Mark as read |

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | Database JDBC URL | - |
| `DB_USERNAME` | Database username | - |
| `DB_PASSWORD` | Database password | - |
| `JWT_SECRET` | Base64-encoded secret (256+ bits) | - |
| `JWT_ACCESS_EXPIRATION` | Access token TTL (ms) | 36000000 |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | 604800000 |
| `UPLOAD_DIR` | File upload directory | ./uploads |
| `CORS_ORIGINS` | Allowed CORS origins (prod) | - |

---

## Profiles

**Development** (`-Dspring-boot.run.profiles=dev`):
- HTTP on port 8080
- Debug logging
- CORS allows all origins

**Production** (`--spring.profiles.active=ssl`):
- HTTPS/TLS on port 8443
- Info logging
- Restricted CORS origins

---

## License

MIT License
