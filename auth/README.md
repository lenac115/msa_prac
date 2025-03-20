# 🛒 Order & Payment System (MSA 기반)

이 프로젝트는 **마이크로서비스 아키텍처(MSA)** 를 기반으로 한 주문 및 결제 시스템입니다.  
Kafka를 활용한 **비동기 이벤트 처리** 와 **Spring Security (JWT 인증)** 을 적용하여 확장성과 보안성을 고려한 구조로 설계되었습니다.  
또한, **실시간 채팅 기능(WebSocket + Kafka)** 을 추가하여 고객과 판매자 간의 원활한 소통을 지원합니다.

---

## 📌 아키텍처 개요 (MSA 구조)
이 프로젝트는 다음과 같은 **마이크로서비스** 로 구성됩니다:

1️⃣ **Auth Service (인증 서비스)**
- JWT 기반 인증 및 사용자 관리
- 역할(Role) 기반 접근 제어 (`BUYER`, `SELLER`)
- Kafka를 이용한 `SELLER` 및 `BUYER` 정보 전송 (Event-Driven)

2️⃣ **Order Service (주문 서비스)**
- 상품 주문 관리
- 구매자(`BUYER`) 및 판매자(`SELLER`) 정보 캐싱
- 주문 데이터 저장 및 상태 관리

3️⃣ **Payment Service (결제 서비스)**
- 결제 요청 및 승인 처리
- 주문 상태 변경 (`ORDERED` → `PAID`)
- Kafka를 이용한 결제 이벤트 처리

4️⃣ **Chat Service (실시간 채팅 서비스)**
- WebSocket을 이용한 실시간 채팅 기능
- Kafka를 Message Broker로 활용하여 메시지 전송

5️⃣ **API Gateway (게이트웨이 서비스)**
- Spring Cloud Gateway를 사용하여 API 요청을 라우팅
- JWT 인증 및 보안 필터 적용

---

## ⚙️ 사용 기술 스택
| 분야            | 기술 스택 |
|---------------|-----------------|
| **Language**  | Java 17, Spring Boot |
| **Frameworks**  | Spring Security, Spring WebFlux, Spring Kafka |
| **Database**   | MySQL, Redis (캐싱) |
| **Message Queue** | Apache Kafka |
| **API Gateway** | Spring Cloud Gateway |
| **Real-time**   | WebSocket, Kafka Streams |
| **Authentication** | JWT (JSON Web Token) |
| **DevOps**      | Docker, Kubernetes, Prometheus, Grafana |

---

## 🏗️ 서비스 간 데이터 흐름
### 🔹 Kafka 기반 이벤트 흐름
- **사용자 정보 전송 (Auth → Order, Payment)**
    - `auth-service` → `order-service` 로 `SELLER`, `BUYER` 정보 전달
    - `auth-service` → `payment-service` 로 `BUYER` 정보 전달

- **주문 및 결제 처리**
    - `order-service` → `payment-service` 로 결제 요청 이벤트 발행
    - `payment-service` → `order-service` 로 결제 완료 이벤트 발행

- **실시간 채팅**
    - `chat-service` 가 Kafka Topic을 구독하여 메시지 전달
    - `BUYER` 와 `SELLER` 간 WebSocket을 통해 실시간 채팅 지원

---

## 🔑 보안 및 인증 (Auth Service)
- JWT 기반 **토큰 인증** 적용
- Spring Security + API Gateway에서 **권한(Role) 기반 접근 제어**
- `SELLER` 와 `BUYER` 의 권한을 Enum 클래스로 정의하여 관리
- `BUYER` 정보는 구매한 `SELLER` 에 대해서만 조회 가능하도록 제한

---

## 🚀 API 명세 예시
### 🔹 Auth Service
#### 🔐 로그인 API
```http
POST /auth/login
```
```json
{
  "email": "user@example.com",
  "password": "securepassword"
}
```
✅ **Response**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 👤 사용자 정보 조회 (구매자 정보 제한)
```http
GET /auth/user/{userId}
Authorization: Bearer {accessToken}
```
✅ **Response**
```json
{
  "id": 1,
  "role": "BUYER",
  "email": "buyer@example.com"
}
```

---

## 🛠️ 구현 예정 기능
✅ **1. Kafka 기반 주문 이벤트 연동**  
✅ **2. WebSocket을 통한 실시간 채팅 구축**  
✅ **3. Redis를 활용한 사용자 정보 캐싱**  
✅ **4. 결제 서비스 연동 (PG사 모듈 추가 가능)**  
✅ **5. Prometheus + Grafana 기반 모니터링 구축**

---

## 📜 프로젝트 실행 방법
### 1️⃣ Kafka 및 MySQL 실행
```bash
docker-compose up -d
```

### 2️⃣ Auth 서비스 실행
```bash
cd auth-service
./mvnw spring-boot:run
```

### 3️⃣ Order, Payment, Chat 서비스 실행
```bash
cd order-service && ./mvnw spring-boot:run
cd payment-service && ./mvnw spring-boot:run
cd chat-service && ./mvnw spring-boot:run
```

---

## 📌 기여 가이드
1. 코드 스타일은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) 준수

---

## 💬 문의 및 피드백
- **GitHub Issues** 에 피드백 남겨주세요!
- **Email:** lenac115@naver.com  
