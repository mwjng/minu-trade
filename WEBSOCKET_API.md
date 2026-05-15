# WebSocket API (STOMP over WebSocket)

minu-trade 의 실시간 데이터 채널 명세. 클라이언트가 구독해야 할 destination 과 메시지 페이로드 형식을 정의한다.

> **Single source of truth**: 서버 측 destination 상수는 `com.minupay.trade.broadcast.application.StompDestinations` 에 정의되어 있다. 이 문서는 그 상수의 외부 공개 명세이며, 변경 시 두 곳을 함께 갱신해야 한다.

---

## 1. 연결 (Handshake & CONNECT)

### Endpoint
```
ws://{host}/ws        (개발)
wss://{host}/ws       (운영)
```

### 인증
STOMP `CONNECT` 프레임의 `Authorization` 헤더에 JWT 를 실어 보낸다. minu-pay 와 같은 secret 으로 서명된 토큰을 사용한다.

```
CONNECT
Authorization: Bearer {jwt}
accept-version:1.2
heart-beat:10000,10000
```

서버 (`JwtChannelInterceptor`) 가 CONNECT 프레임에서만 토큰을 검증하고 `Principal` 로 `userId` 를 바인딩한다. 이후 SUBSCRIBE/SEND 프레임은 별도 인증을 거치지 않는다(연결 자체가 인증된 세션이므로).

토큰이 없거나 위조된 경우 CONNECT 가 거부되어 ERROR 프레임으로 응답한다.

### 클라이언트 예시 (StompJS)
```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'wss://api.minu-trade.com/ws',
  connectHeaders: { Authorization: `Bearer ${jwt}` },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});
client.activate();
```

---

## 2. Destination 종류

| Destination | 종류 | 발행 트리거 | 수신자 |
|---|---|---|---|
| `/topic/quotes/{stockCode}` | public topic | KIS 시세 수신 → `quote.updated` Kafka | 해당 종목을 구독한 모두 |
| `/user/queue/orders` | user queue | `order.accepted` / `.filled` / `.cancelled` / `.rejected` | 해당 주문의 소유자 1명 |
| `/user/queue/trades` | user queue | `trade.executed` | 매수자 + 매도자 (각자 1명) |
| `/user/queue/holdings` | user queue | `holding.updated` | 해당 보유의 소유자 1명 |

### topic vs user queue
- **`/topic/...`** — 같은 destination 을 구독한 모든 세션이 동일 메시지를 받는다 (fan-out).
- **`/user/queue/...`** — 클라이언트는 `/user/queue/orders` 로 SUBSCRIBE 하지만, 서버가 `/user/{userId}/queue/orders` 로 변환해 해당 사용자의 세션에만 전달한다. 다른 사용자에게는 절대 새지 않는다.

---

## 3. SUBSCRIBE 예시

### 종목 시세 (public)
```ts
client.subscribe('/topic/quotes/005930', (message) => {
  const event = JSON.parse(message.body);
  // event.payload.currentPrice 등
});
```
종목 코드를 동적으로 조립해 보내면 된다. 와일드카드/사전 등록 불필요. SimpleBroker 가 prefix(`/topic`) 만 보고 destination 별로 구독자 목록을 관리한다.

### 본인 주문/체결/보유 (private)
```ts
client.subscribe('/user/queue/orders',   (m) => { /* 주문 상태 변경 */ });
client.subscribe('/user/queue/trades',   (m) => { /* 체결 발생 */ });
client.subscribe('/user/queue/holdings', (m) => { /* 보유 변동 */ });
```

`/user` prefix 는 클라이언트가 그대로 적어 보내야 한다. 서버가 `/user/{userId}/queue/orders` 로 라우팅한다.

---

## 4. 메시지 envelope

모든 메시지는 Outbox 가 발행한 Kafka envelope 를 그대로 STOMP body 로 전달한다. 형식은 다음과 같다.

```json
{
  "eventId": "01J9X8...",
  "eventType": "order.filled",
  "occurredAt": "2026-05-07T12:34:56.789Z",
  "payload": { /* 이벤트별 페이로드 */ }
}
```

페이로드 스키마는 `DESIGN.md` 의 도메인 이벤트 섹션과 일치한다(주요 필드만 발췌).

| eventType | payload 핵심 필드 |
|---|---|
| `quote.updated` | `stockCode`, `currentPrice`, `volume`, `timestamp` |
| `order.accepted` / `.filled` / `.cancelled` / `.rejected` | `orderId`, `accountId`, `userId`, `stockCode`, `side`, `status`, `filledQuantity` |
| `trade.executed` | `tradeId`, `stockCode`, `price`, `quantity`, `buyerUserId`, `sellerUserId`, `buyOrderId`, `sellOrderId` |
| `holding.updated` | `holdingId`, `userId`, `stockCode`, `quantity`, `avgPrice` |

---

## 5. 클라이언트 → 서버 메시지

현재 시점에는 **없다**. 클라이언트가 `SEND` 로 보낼 destination(`/app/...`) 은 정의되어 있지 않으며, 모든 명령 (주문/취소 등) 은 REST API 로 처리한다. WebSocket 은 read-only push 채널이다.

따라서 클라이언트는 사실상 다음만 한다:
- `CONNECT` (인증)
- `SUBSCRIBE` (관심 destination 등록)
- `UNSUBSCRIBE` / `DISCONNECT` (정리)
- 서버 PING 에 PONG 응답 (heart-beat, StompJS 자동 처리)

---

## 6. 재연결 / 멱등성 / 끊김

- **재연결**: 클라이언트는 `reconnectDelay` 로 재연결을 시도한다. 재연결 시 SUBSCRIBE 를 다시 걸어야 한다 (StompJS 의 `onConnect` 콜백에서 처리).
- **메시지 유실**: SimpleBroker 는 in-memory 라 끊겨 있는 동안 발행된 메시지는 받지 못한다. 정확한 상태가 필요한 화면(주문 목록, 보유 목록)은 재연결 직후 REST 로 한 번 fetch 해 동기화하는 것을 권장한다.
- **중복**: Kafka consumer 가 at-least-once 라 동일 `eventId` 가 두 번 도착할 수 있다. 클라이언트는 `eventId` 기준 중복 제거를 구현해야 한다.

---

## 7. 운영 / 디버깅

- 로컬 개발: `docker-compose up` 으로 Redis(분산 fanout)·Kafka 띄우고, `ws://localhost:8080/ws` 로 접속.
- 인스턴스 간 메시지 전파: `RedisStompFanoutPublisher` → Redis Pub/Sub → 각 인스턴스의 `RedisStompFanoutListener` → 로컬 SimpleBroker. 즉, 같은 사용자가 어느 인스턴스에 붙어 있든 메시지를 받는다.
- 한 인스턴스에서 `KafkaListener` 가 메시지를 받아 `RedisStompFanoutPublisher` 로 publish 하는 게 아니라, **각 broadcast consumer 가 자신만의 group 으로 Kafka 를 소비** → 메시지를 Redis 로 fanout → 모든 인스턴스가 자기 세션에 push.

---

## 8. 변경 절차

1. 새 destination / payload 추가 시: `StompDestinations.java` 와 본 문서를 함께 PR 에 포함.
2. payload 스키마 변경 시: 하위 호환을 깨지 않는 방향(필드 추가만) 우선. 깨야 한다면 별도 eventType 으로 versioning.
3. 클라이언트 SDK 가 있다면 destination 상수를 codegen 하여 양쪽이 같은 값을 쓰게 한다.
