# SocialChat API

A robust RESTful API and real-time messaging backend built with Spring Boot. This service provides authentication, social networking features, real-time chat via WebSockets, and media management capabilities.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Project](#running-the-project)
- [API Documentation](#api-documentation)
- [WebSocket Events](#websocket-events)
- [Database Schema](#database-schema)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Contribution Guide](#contribution-guide)
- [License](#license)

---

## Features

### Authentication & Security
- JWT-based authentication with access and refresh tokens
- Password encryption using BCrypt
- Token revocation on logout
- Role-based access control
- CORS configuration for cross-origin requests

### User Management
- User registration and login
- Profile management with photo uploads
- Account deletion
- User search functionality

### Social Features
- Friend requests (send, accept, reject)
- Friend list management
- User blocking/unblocking
- Relationship status tracking

### Posts & Content
- Create, read, update, delete posts
- Image attachments support
- Like/unlike posts
- Comments on posts
- Paginated feeds

### Real-time Chat
- One-on-one conversations
- Group conversations
- Real-time messaging via WebSocket/STOMP
- Typing indicators
- Read receipts
- Message history with pagination

### Notifications
- Real-time notifications
- Notification types: friend requests, likes, comments, messages
- Mark as read functionality
- Unread count tracking

### Media Management
- Profile photo uploads
- Post image uploads
- Secure file storage
- Image serving endpoints

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                                        │
│              (Web App / Mobile App / Third-party)                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          │                               │
          ▼                               ▼
┌─────────────────────┐       ┌─────────────────────┐
│        REST API         │       │   WebSocket (STOMP)     │
│      (HTTP/HTTPS)       │       │   Real-time Comms       │
│    Port: (8080/8443)    │       │    Endpoint: /ws        │
└─────────┬───────────┘       └─────────┬───────────┘
            │                                 │
            └───────────────┬─────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SocialChat API                                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐    │
│  │    Auth        │ │    User       │ │    Chat        │ │   Post      │    │
│  │  Service       │ │  Service      │ │  Service       │ │  Service    │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐    │
│  │   Friend       │ │  Notification │ │   Search       │ │   Media     │    │
│  │  Service       │ │  Service      │ │  Service       │ │  Service    │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘    │
└─────────────────────────┬───────────────────────────────────────┘
                               │
          ┌───────────────┴───────────────┐
          ▼                                    ▼
┌─────────────────────┐       ┌─────────────────────┐
│       MySQL             │       │    File Storage         │
│     Database            │       │   (Local/Cloud)         │
└─────────────────────┘       └─────────────────────┘
```

---

## Tech Stack

| Category | Technology |
|----------|------------|
| **Framework** | Spring Boot 3.2.0 |
| **Language** | Java 17 |
| **Database** | MySQL 8.0 |
| **ORM** | Spring Data JPA / Hibernate |
| **Migrations** | Flyway |
| **Security** | Spring Security + JWT (jjwt 0.12.3) |
| **WebSocket** | Spring WebSocket + STOMP |
| **API Docs** | SpringDoc OpenAPI (Swagger) |
| **Validation** | Jakarta Validation |
| **Mapping** | MapStruct 1.5.5 |
| **Utilities** | Lombok |
| **Build Tool** | Maven |

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **MySQL 8.0+**
- **Git**

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/socialchat-api.git
cd socialchat-api
```

### 2. Configure Database

Create a MySQL database:

```sql
CREATE DATABASE socialchat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'socialchat'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON socialchat_db.* TO 'socialchat'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Environment Variables

Set the following environment variables or update `application.properties`:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `root` |
| `JWT_SECRET` | Base64-encoded secret key (min 32 chars) | Dev key provided |
| `UPLOAD_DIR` | Directory for file uploads | `./uploads` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |

### 4. Install Dependencies

```bash
cd backend
mvn clean install
```

---

## Running the Project

### Development Mode

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Development with Debug Logging

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Mode

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Build and Run JAR

```bash
mvn clean package -DskipTests
java -jar target/social-chat-backend-1.0.0.jar
```

---

## API Documentation

### Interactive Documentation

Once running, access the API docs at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### Authentication

All protected endpoints require a Bearer token:

```
Authorization: Bearer <access_token>
```

### API Endpoints Reference

#### Authentication (`/api/auth`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/register` | Register new user | No |
| POST | `/login` | Login and get tokens | No |
| POST | `/refresh` | Refresh access token | No |
| POST | `/logout` | Logout and revoke token | Yes |
| POST | `/change-password` | Change password | Yes |

**Register Request:**
```json
{
"username": "johndoe",
"email": "john@example.com",
"password": "SecurePass123!",
"displayName": "John Doe"
}
```

**Login Response:**
```json
{
"accessToken": "eyJhbGciOiJIUzI1NiIs...",
"refreshToken": "eyJhbGciOiJIUzI1NiIs...",
"tokenType": "Bearer",
"expiresIn": 900,
"user": {
"id": 1,
"username": "johndoe",
"email": "john@example.com",
"displayName": "John Doe"
}
}
```

#### Users (`/api/users`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/me` | Get current user profile | Yes |
| GET | `/{id}` | Get user by ID | Yes |
| GET | `/username/{username}` | Get user by username | Yes |
| PATCH | `/me` | Update current user profile | Yes |
| POST | `/me/photo` | Upload profile photo | Yes |
| DELETE | `/me` | Delete account | Yes |

#### Posts (`/api/posts`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create new post | Yes |
| GET | `/feed` | Get paginated feed | Yes |
| GET | `/{id}` | Get post by ID | Yes |
| GET | `/user/{userId}` | Get user's posts | Yes |
| PATCH | `/{id}` | Update post | Yes |
| DELETE | `/{id}` | Delete post | Yes |
| POST | `/{id}/like` | Like a post | Yes |
| DELETE | `/{id}/like` | Unlike a post | Yes |
| POST | `/{id}/comments` | Add comment | Yes |
| GET | `/{id}/comments` | Get post comments | Yes |
| DELETE | `/{postId}/comments/{commentId}` | Delete comment | Yes |

**Create Post (multipart/form-data):**
```
content: "Hello world!"
image: [file] (optional)
```

#### Chat (`/api/chat`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/conversations` | Create or get conversation | Yes |
| GET | `/conversations` | List user's conversations | Yes |
| GET | `/conversations/{id}` | Get conversation details | Yes |
| GET | `/conversations/{id}/messages` | Get messages (paginated) | Yes |
| POST | `/conversations/{id}/messages` | Send message | Yes |
| POST | `/conversations/{id}/read` | Mark as read | Yes |
| DELETE | `/conversations/{id}` | Leave conversation | Yes |

**Create Conversation:**
```json
{
"participantIds": [2, 3],
"name": "Group Chat"  // optional, for groups
}
```

**Send Message:**
```json
{
"content": "Hello!",
"messageType": "TEXT"
}
```

#### Friends (`/api/friends`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | Get friends list | Yes |
| POST | `/request/{userId}` | Send friend request | Yes |
| GET | `/requests/received` | Get received requests | Yes |
| GET | `/requests/sent` | Get sent requests | Yes |
| POST | `/requests/{id}/accept` | Accept friend request | Yes |
| POST | `/requests/{id}/reject` | Reject friend request | Yes |
| DELETE | `/{friendId}` | Remove friend | Yes |
| POST | `/block/{userId}` | Block user | Yes |
| DELETE | `/block/{userId}` | Unblock user | Yes |
| GET | `/relationship/{userId}` | Get relationship status | Yes |

#### Notifications (`/api/notifications`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | Get notifications (paginated) | Yes |
| GET | `/unread-count` | Get unread count | Yes |
| POST | `/{id}/read` | Mark as read | Yes |
| POST | `/read-all` | Mark all as read | Yes |
| DELETE | `/{id}` | Delete notification | Yes |

#### Search (`/api/search`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/users?q={query}` | Search users by username/name | Yes |

#### Media (`/api/media`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/{filename}` | Get uploaded file | No |

#### Health (`/api/health`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | Health check | No |

---

## WebSocket Events

### Connection

Connect to WebSocket endpoint with authentication:

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
{ Authorization: `Bearer ${accessToken}` },
onConnected,
onError
);
```

### Subscribe Destinations

| Destination | Description |
|-------------|-------------|
| `/topic/conversations/{id}/messages` | New messages in conversation |
| `/topic/conversations/{id}/typing` | Typing indicators |
| `/topic/conversations/{id}/read-receipts` | Read receipts |
| `/topic/presence` | User online/offline status |
| `/user/queue/notifications` | Personal notifications |

### Send Destinations

| Destination | Payload | Description |
|-------------|---------|-------------|
| `/app/chat/{conversationId}/message` | `{ content, messageType }` | Send message |
| `/app/chat/{conversationId}/typing` | `{ isTyping }` | Send typing status |
| `/app/chat/{conversationId}/read` | `{ messageId }` | Send read receipt |

### Event Payloads

**New Message:**
```json
{
"id": 123,
"conversationId": 1,
"senderId": 2,
"senderName": "John",
"content": "Hello!",
"messageType": "TEXT",
"createdAt": "2024-01-15T10:30:00Z"
}
```

**Typing Indicator:**
```json
{
"userId": 2,
"username": "johndoe",
"isTyping": true
}
```

**Read Receipt:**
```json
{
"userId": 2,
"messageId": 123,
"readAt": "2024-01-15T10:31:00Z"
}
```

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────────────┐       ┌─────────────┐
│   users     │       │   friend_requests   │       │    posts    │
├─────────────┤       ├─────────────────────┤       ├─────────────┤
│ id (PK)     │◄──┬───│ sender_id (FK)      │   ┌──►│ id (PK)     │
│ username    │   │   │ receiver_id (FK)    │───┘   │ user_id(FK) │◄─┐
│ email       │   │   │ status              │       │ content     │  │
│ password    │   │   │ created_at          │       │ image_url   │  │
│ display_name│   │   └─────────────────────┘       │ created_at  │  │
│ bio         │   │                                 └──────┬──────┘  │
│ profile_pic │   │                                        │         │
│ created_at  │   │   ┌─────────────────────┐              │         │
│ updated_at  │   │   │    post_likes       │              │         │
└──────┬──────┘   │   ├─────────────────────┤              │         │
       │          ├───│ user_id (FK)        │              │         │
       │          │   │ post_id (FK)        │──────────────┘         │
       │          │   │ created_at          │                        │
       │          │   └─────────────────────┘                        │
       │          │                                                  │
       │          │   ┌─────────────────────┐                        │
       │          │   │     comments        │                        │
       │          │   ├─────────────────────┤                        │
       │          ├───│ user_id (FK)        │                        │
       │          │   │ post_id (FK)        │────────────────────────┘
       │          │   │ content             │
       │          │   │ created_at          │
       │          │   └─────────────────────┘
       │          │
       │          │   ┌─────────────────────┐       ┌─────────────┐
       │          │   │ conversation_       │       │conversations│
       │          │   │ participants        │       ├─────────────┤
       │          │   ├─────────────────────┤   ┌──►│ id (PK)     │
       │          ├───│ user_id (FK)        │   │   │ name        │
       │          │   │ conversation_id(FK) │───┘   │ is_group    │
       │          │   │ joined_at           │       │ created_at  │
       │          │   └─────────────────────┘       └──────┬──────┘
       │          │                                        │
       │          │   ┌─────────────────────┐              │
       │          │   │     messages        │              │
       │          │   ├─────────────────────┤              │
       │          ├───│ sender_id (FK)      │              │
       │              │ conversation_id(FK) │──────────────┘
       │              │ content             │
       │              │ message_type        │
       │              │ created_at          │
       │              └─────────────────────┘
       │
       │          ┌─────────────────────┐
       │          │   notifications     │
       │          ├─────────────────────┤
       └──────────│ user_id (FK)        │
                  │ type                │
                  │ content             │
                  │ reference_id        │
                  │ is_read             │
                  │ created_at          │
                  └─────────────────────┘
```

### Tables Overview

| Table | Description |
|-------|-------------|
| `users` | User accounts and profiles |
| `revoked_tokens` | JWT tokens revoked on logout |
| `friend_requests` | Friend request tracking with status |
| `posts` | User posts with optional images |
| `post_likes` | Post like relationships |
| `comments` | Comments on posts |
| `conversations` | Chat conversations (1:1 or group) |
| `conversation_participants` | Conversation membership |
| `messages` | Chat messages |
| `notifications` | User notifications |

---

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/socialchat/
│   │   │   ├── config/                 # Configuration classes
│   │   │   │   ├── FileStorageConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   └── WebSocketSecurityConfig.java
│   │   │   │
│   │   │   ├── controller/             # REST API controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── FriendController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── MediaController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   ├── PostController.java
│   │   │   │   ├── SearchController.java
│   │   │   │   └── UserController.java
│   │   │   │
│   │   │   ├── dto/                    # Data Transfer Objects
│   │   │   │   ├── auth/               # Auth DTOs
│   │   │   │   ├── chat/               # Chat DTOs
│   │   │   │   ├── common/             # Shared DTOs
│   │   │   │   ├── friend/             # Friend DTOs
│   │   │   │   ├── notification/       # Notification DTOs
│   │   │   │   ├── post/               # Post DTOs
│   │   │   │   ├── user/               # User DTOs
│   │   │   │   └── websocket/          # WebSocket DTOs
│   │   │   │
│   │   │   ├── entity/                 # JPA Entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Post.java
│   │   │   │   ├── Comment.java
│   │   │   │   ├── PostLike.java
│   │   │   │   ├── FriendRequest.java
│   │   │   │   ├── Conversation.java
│   │   │   │   ├── ConversationParticipant.java
│   │   │   │   ├── Message.java
│   │   │   │   ├── Notification.java
│   │   │   │   └── RevokedToken.java
│   │   │   │
│   │   │   ├── exception/              # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── custom exceptions...
│   │   │   │
│   │   │   ├── mapper/                 # MapStruct mappers
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── PostMapper.java
│   │   │   │   ├── ChatMapper.java
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── repository/             # Spring Data JPA repos
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── PostRepository.java
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── security/               # Security components
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │
│   │   │   ├── service/                # Business logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── PostService.java
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── FriendService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   └── WebSocketService.java
│   │   │   │
│   │   │   ├── websocket/              # WebSocket handlers
│   │   │   │   └── ChatWebSocketHandler.java
│   │   │   │
│   │   │   └── SocialChatApplication.java
│   │   │
│   │   └── resources/
│   │       ├── db/migration/           # Flyway migrations
│   │       │   └── V1__initial_schema.sql
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── application-ssl.properties
│   │
│   └── test/                           # Test classes
│
└── pom.xml                             # Maven configuration
```

---

## Configuration

### Application Properties

#### Core Settings (`application.properties`)

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/socialchat_db
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
jwt.secret=${JWT_SECRET:your-secret-key}
jwt.access-token.expiration=36000000      # 1 hour
jwt.refresh-token.expiration=604800000  # 7 days

# File Upload
file.upload-dir=${UPLOAD_DIR:./uploads}
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```
