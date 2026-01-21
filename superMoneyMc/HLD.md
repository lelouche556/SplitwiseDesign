# High-Level Design (HLD) Document
## Expense Sharing Application

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Component Design](#component-design)
4. [Data Flow Diagrams](#data-flow-diagrams)
5. [Database Design](#database-design)
6. [API Design](#api-design)
7. [Simplify Debt Algorithm Flow](#simplify-debt-algorithm-flow)
8. [Scale-Up Scenarios](#scale-up-scenarios)
9. [Deployment Architecture](#deployment-architecture)

---

## System Overview

### Purpose
An expense sharing application that allows users to track expenses, split them among multiple people, and manage balances between users.

### Key Features
- User management
- Expense creation with multiple split types (EQUAL, EXACT, PERCENT, SHARE)
- Balance tracking and netting
- Expense simplification
- Payment/settlement tracking
- Transaction history (passbook)

### Non-Functional Requirements
- In-memory storage (current)
- Fast response times
- Scalable architecture
- Extensible design

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   CLI App    │  │  REST API    │  │  Web UI      │     │
│  │ (Current)    │  │ (Scale-up)   │  │ (Scale-up)   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          ExpenseSharingApp (Command Processor)       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ UserService  │  │ExpenseService│  │BalanceService│     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │PassbookService│ │PaymentService│                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Strategy Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ EqualSplit   │  │ ExactSplit   │  │ PercentSplit │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐                                          │
│  │ ShareSplit    │                                          │
│  └──────────────┘                                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ In-Memory    │  │  Repository  │  │   Database   │     │
│  │ (Current)    │  │  (Scale-up)  │  │  (Scale-up)  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Design

### 1. Presentation Layer

**Current: CLI (Command Line Interface)**
- `ExpenseSharingApp`: Parses commands and delegates to services
- Commands: EXPENSE, SHOW, PASSBOOK, UPDATE_EXPENSE, SIMPLIFY, SETTLE, PAYMENTS

**Scale-Up: REST API**
- RESTful endpoints for all operations
- JSON request/response format
- Authentication & authorization middleware

**Scale-Up: Web UI**
- React/Vue.js frontend
- Real-time updates via WebSockets
- Responsive design

### 2. Application Layer

**ExpenseSharingApp**
- Command parsing and validation
- Routes commands to appropriate services
- Error handling and response formatting

### 3. Service Layer

#### UserService
- **Responsibilities:**
  - User CRUD operations
  - User validation
  - User lookup

#### ExpenseService
- **Responsibilities:**
  - Expense creation and management
  - Balance calculation and updates
  - Expense simplification
  - Payment/settlement processing

#### BalanceService
- **Responsibilities:**
  - Balance retrieval
  - Balance formatting for display
  - Filtering balances by user

#### PassbookService
- **Responsibilities:**
  - Transaction history retrieval
  - Passbook formatting

#### PaymentService (Scale-up)
- **Responsibilities:**
  - Payment processing
  - Payment history management
  - Settlement tracking

### 4. Strategy Layer

**SplitStrategy Interface**
- `calculateSplit(Expense)`: Returns Map<User, Double>

**Implementations:**
- `EqualSplitStrategy`: Equal division
- `ExactSplitStrategy`: Exact amounts
- `PercentSplitStrategy`: Percentage-based
- `ShareSplitStrategy`: Proportional shares

### 5. Data Layer

**Current: In-Memory**
- `Map<String, User>`: User storage
- `Map<String, Expense>`: Expense storage
- `Map<String, Map<String, Transaction>>`: Balance storage
- `List<Expense>`: Expense history
- `List<Payment>`: Payment history

**Scale-Up: Repository Pattern**
- `UserRepository`: Database operations for users
- `ExpenseRepository`: Database operations for expenses
- `TransactionRepository`: Database operations for transactions
- `PaymentRepository`: Database operations for payments

---

## Data Flow Diagrams

### 1. Expense Creation Flow

```
User Input: EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL
    │
    ▼
ExpenseSharingApp.processCommand()
    │
    ▼
Parse command → Validate parameters
    │
    ▼
UserService.getUser() → Validate users exist
    │
    ▼
ExpenseService.addExpense()
    │
    ├─→ Create Expense object
    │
    ├─→ SplitStrategyFactory.createStrategy(EQUAL)
    │       │
    │       └─→ EqualSplitStrategy.calculateSplit()
    │               │
    │               └─→ Returns: {u2: 250, u3: 250, u4: 250}
    │
    └─→ updateBalances(paidBy, splitMap)
            │
            ├─→ For each participant:
            │   ├─→ Check if reverse transaction exists
            │   ├─→ Net out amounts if needed
            │   └─→ Update balances map
            │
            └─→ If simplifyExpenses enabled:
                └─→ simplifyBalances()
```

### 2. Balance Display Flow

```
User Input: SHOW u1
    │
    ▼
ExpenseSharingApp.processCommand()
    │
    ▼
BalanceService.showUserBalances(u1)
    │
    ▼
ExpenseService.getUserBalances(u1)
    │
    ├─→ Get balances where u1 owes others
    │   └─→ balances[u1][otherUserId]
    │
    └─→ Get balances where others owe u1
        └─→ balances[otherUserId][u1]
            └─→ Create reverse transactions for display
    │
    ▼
Filter transactions with pendingAmount > 0
    │
    ▼
Format and display:
    "u2 owes u1: 250.00"
    "u1 owes u4: 230.00 (Settled: 250.00)"
```

### 3. Simplify Debt Flow

```
User Input: SIMPLIFY true
    │
    ▼
ExpenseService.setSimplifyExpenses(true)
    │
    ▼
simplifyBalances()
    │
    ├─→ Step 1: Calculate Net Balances
    │   └─→ For each user pair, calculate net amount
    │       Example: A owes B 250, B owes A 100
    │       Net: A owes B 150
    │
    ├─→ Step 2: Build Debt Graph
    │   └─→ Create graph: A → B (150), B → C (200)
    │
    └─→ Step 3: Transitive Simplification
        │
        ├─→ For each chain A → B → C:
        │   ├─→ Check if A can pay C directly
        │   ├─→ Calculate: min(A→B, B→C)
        │   └─→ Simplify: A → C (amount), A → B (remaining)
        │
        └─→ Repeat until no more simplifications possible
    │
    ▼
Update balances map with simplified transactions
```

**Detailed Simplify Debt Algorithm:**

```
Algorithm: simplifyBalancesGraph()

1. NETTING PHASE:
   For each user pair (A, B):
     - If A owes B and B owes A:
       - Calculate net = |A→B - B→A|
       - If A→B > B→A: A owes B net amount
       - Else: B owes A net amount
       - Remove smaller debt

2. GRAPH BUILDING:
   Build directed graph G where:
     - Nodes = Users
     - Edges = Debts (A → B: amount)

3. TRANSITIVE SIMPLIFICATION:
   For each node A:
     For each outgoing edge A → B:
       For each outgoing edge B → C:
         If A ≠ C:
           minAmount = min(A→B.amount, B→C.amount)
           If minAmount > threshold:
             - Add A → C: minAmount
             - Reduce A → B by minAmount
             - Reduce B → C by minAmount
             - If A → B becomes 0, remove edge
             - If B → C becomes 0, remove edge

4. ITERATION:
   Repeat step 3 until no changes occur

5. RESULT:
   Return simplified graph with minimum transactions
```

**Example:**
```
Initial State:
  A owes B: 250
  B owes C: 200

After Simplification:
  A owes B: 50
  A owes C: 200

Result: Reduced from 2 transactions to 2 transactions,
        but simplified the chain (A can pay C directly)
```

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│    User     │         │   Expense    │         │ Transaction │
├─────────────┤         ├──────────────┤         ├─────────────┤
│ userId (PK) │◄──┐     │ expenseId(PK)│     ┌─►│ fromUserId  │
│ name        │   │     │ paidByUserId │     │  │ toUserId    │
│ email       │   │     │ amount       │     │  │ amount      │
│ mobile      │   │     │ expenseName │     │  │ paidAmount  │
└─────────────┘   │     │ splitType    │     │  │ createdAt   │
                 │     │ createdAt    │     │  └─────────────┘
                 │     └──────────────┘     │
                 │            │              │
                 │            │              │
                 │     ┌──────▼──────┐      │
                 │     │Participant  │      │
                 │     ├─────────────┤      │
                 │     │expenseId(FK)│      │
                 │     │userId (FK)  │      │
                 │     │splitValue   │      │
                 │     └─────────────┘      │
                 │                          │
                 │     ┌──────────────┐     │
                 │     │   Payment    │     │
                 │     ├──────────────┤     │
                 │     │paymentId (PK)│     │
                 │     │fromUserId(FK)├─────┘
                 │     │toUserId (FK) │
                 │     │amount        │
                 │     │paymentDate   │
                 │     │notes         │
                 │     └──────────────┘
                 │
                 │     ┌──────────────┐
                 │     │    Group     │
                 │     ├──────────────┤
                 │     │groupId (PK)  │
                 │     │name          │
                 │     │adminUserId   │
                 │     └──────────────┘
                 │            │
                 │     ┌──────▼──────┐
                 │     │GroupMember   │
                 │     ├──────────────┤
                 │     │groupId (FK)  │
                 │     │userId (FK)   │
                 │     └──────────────┘
                 │
                 └─────────────────────┘
```

### Database Schema

#### Users Table
```sql
CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    mobile VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

#### Expenses Table
```sql
CREATE TABLE expenses (
    expense_id VARCHAR(50) PRIMARY KEY,
    paid_by_user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    expense_name VARCHAR(200),
    notes TEXT,
    split_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (paid_by_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_expenses_paid_by ON expenses(paid_by_user_id);
CREATE INDEX idx_expenses_created_at ON expenses(created_at);
```

#### Expense_Participants Table
```sql
CREATE TABLE expense_participants (
    expense_id VARCHAR(50),
    user_id VARCHAR(50),
    split_value DECIMAL(10, 2),
    PRIMARY KEY (expense_id, user_id),
    FOREIGN KEY (expense_id) REFERENCES expenses(expense_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

#### Transactions Table (Balances)
```sql
CREATE TABLE transactions (
    from_user_id VARCHAR(50),
    to_user_id VARCHAR(50),
    total_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2) DEFAULT 0,
    pending_amount DECIMAL(10, 2) GENERATED ALWAYS AS (total_amount - paid_amount),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (from_user_id, to_user_id),
    FOREIGN KEY (from_user_id) REFERENCES users(user_id),
    FOREIGN KEY (to_user_id) REFERENCES users(user_id),
    CHECK (paid_amount <= total_amount)
);

CREATE INDEX idx_transactions_from ON transactions(from_user_id);
CREATE INDEX idx_transactions_to ON transactions(to_user_id);
CREATE INDEX idx_transactions_pending ON transactions(pending_amount);
```

#### Payments Table
```sql
CREATE TABLE payments (
    payment_id VARCHAR(50) PRIMARY KEY,
    from_user_id VARCHAR(50) NOT NULL,
    to_user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    notes TEXT,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_user_id) REFERENCES users(user_id),
    FOREIGN KEY (to_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_payments_from ON payments(from_user_id);
CREATE INDEX idx_payments_to ON payments(to_user_id);
CREATE INDEX idx_payments_date ON payments(payment_date);
```

#### Groups Table (Scale-up)
```sql
CREATE TABLE groups (
    group_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    admin_user_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_user_id) REFERENCES users(user_id)
);

CREATE TABLE group_members (
    group_id VARCHAR(50),
    user_id VARCHAR(50),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups(group_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

---

## API Design

### REST API Endpoints

#### User Management
```
POST   /api/v1/users
GET    /api/v1/users/{userId}
GET    /api/v1/users
PUT    /api/v1/users/{userId}
DELETE /api/v1/users/{userId}
```

#### Expense Management
```
POST   /api/v1/expenses
GET    /api/v1/expenses/{expenseId}
GET    /api/v1/expenses?userId={userId}&groupId={groupId}
PUT    /api/v1/expenses/{expenseId}
DELETE /api/v1/expenses/{expenseId}
```

#### Balance Management
```
GET    /api/v1/balances
GET    /api/v1/balances/{userId}
GET    /api/v1/balances/{userId}/summary
```

#### Payment/Settlement
```
POST   /api/v1/payments/settle
GET    /api/v1/payments
GET    /api/v1/payments/{userId}
GET    /api/v1/payments/{paymentId}
```

#### Passbook
```
GET    /api/v1/passbook/{userId}
GET    /api/v1/passbook/{userId}?from={date}&to={date}
```

#### Simplification
```
POST   /api/v1/simplify/enable
POST   /api/v1/simplify/disable
GET    /api/v1/simplify/status
```

### Request/Response Examples

#### Create Expense
```json
POST /api/v1/expenses
Request:
{
  "paidByUserId": "u1",
  "amount": 1000,
  "expenseName": "Electricity Bill",
  "splitType": "EQUAL",
  "participants": ["u1", "u2", "u3", "u4"],
  "notes": "Monthly bill",
  "imageUrls": ["http://example.com/bill.jpg"]
}

Response:
{
  "expenseId": "exp-123",
  "status": "success",
  "message": "Expense created successfully"
}
```

#### Get Balances
```json
GET /api/v1/balances/u1
Response:
{
  "userId": "u1",
  "balances": [
    {
      "fromUserId": "u2",
      "toUserId": "u1",
      "totalAmount": 620.00,
      "paidAmount": 200.00,
      "pendingAmount": 420.00,
      "isFullySettled": false
    },
    {
      "fromUserId": "u1",
      "toUserId": "u4",
      "totalAmount": 230.00,
      "paidAmount": 0.00,
      "pendingAmount": 230.00,
      "isFullySettled": false
    }
  ]
}
```

#### Settle Debt
```json
POST /api/v1/payments/settle
Request:
{
  "fromUserId": "u2",
  "toUserId": "u1",
  "amount": 200.00,
  "notes": "Partial payment"
}

Response:
{
  "paymentId": "pay-456",
  "status": "success",
  "message": "Settlement successful",
  "remainingAmount": 420.00
}
```

---

## Simplify Debt Algorithm Flow

### Detailed Algorithm

```
FUNCTION simplifyBalancesGraph():
    
    // PHASE 1: Calculate Net Balances
    netBalances = Map<UserId, Map<UserId, Double>>
    
    FOR each transaction in balances:
        fromUser = transaction.fromUser
        toUser = transaction.toUser
        amount = transaction.amount
        
        // Net out reverse transactions
        IF netBalances[toUser][fromUser] exists:
            reverseAmount = netBalances[toUser][fromUser]
            IF amount >= reverseAmount:
                netBalances[toUser].remove(fromUser)
                IF amount > reverseAmount:
                    netBalances[fromUser][toUser] = amount - reverseAmount
            ELSE:
                netBalances[toUser][fromUser] = reverseAmount - amount
                netBalances[fromUser].remove(toUser)
        ELSE:
            netBalances[fromUser][toUser] += amount
    
    // PHASE 2: Transitive Simplification
    simplified = copy(netBalances)
    changed = true
    
    WHILE changed:
        changed = false
        
        FOR each user A in simplified:
            FOR each user B that A owes:
                FOR each user C that B owes:
                    IF A ≠ C:
                        abAmount = simplified[A][B]
                        bcAmount = simplified[B][C]
                        minAmount = min(abAmount, bcAmount)
                        
                        IF minAmount > threshold:
                            // Simplify: A → C directly
                            simplified[A][C] += minAmount
                            
                            // Reduce A → B
                            abAmount -= minAmount
                            IF abAmount <= threshold:
                                simplified[A].remove(B)
                            ELSE:
                                simplified[A][B] = abAmount
                            
                            // Reduce B → C
                            bcAmount -= minAmount
                            IF bcAmount <= threshold:
                                simplified[B].remove(C)
                            ELSE:
                                simplified[B][C] = bcAmount
                            
                            changed = true
    
    // PHASE 3: Convert to Transaction objects
    result = Map<UserId, Map<UserId, Transaction>>
    FOR each entry in simplified:
        IF amount > threshold:
            result[fromUserId][toUserId] = new Transaction(...)
    
    RETURN result
```

### Example Walkthrough

**Initial State:**
```
A owes B: 250
B owes C: 200
C owes D: 150
```

**After Netting (if applicable):**
```
A owes B: 250
B owes C: 200
C owes D: 150
```

**After Transitive Simplification:**
```
Iteration 1:
  A → B → C: min(250, 200) = 200
  Result: A → C: 200, A → B: 50, B → C: 0 (removed)
  
Iteration 2:
  A → C → D: min(200, 150) = 150
  Result: A → D: 150, A → C: 50, C → D: 0 (removed)

Final:
  A owes B: 50
  A owes C: 50
  A owes D: 150
```

**Benefits:**
- Reduced from 3 transactions to 3 transactions
- But simplified chain: A can pay D directly instead of A→B→C→D

---

## Scale-Up Scenarios

### 1. Database Migration

**Current:** In-Memory Maps
**Scale-Up:** PostgreSQL/MySQL

**Migration Strategy:**
1. Add Repository layer abstraction
2. Implement InMemoryRepository (current)
3. Implement DatabaseRepository (new)
4. Use dependency injection to switch implementations
5. Add database connection pooling
6. Add transaction management

**Repository Pattern:**
```
UserRepository (Interface)
├── InMemoryUserRepository (current)
└── DatabaseUserRepository (scale-up)

ExpenseRepository (Interface)
├── InMemoryExpenseRepository (current)
└── DatabaseExpenseRepository (scale-up)
```

### 2. Caching Layer

**Redis Cache Strategy:**
- Cache user balances (TTL: 5 minutes)
- Cache user passbook (TTL: 10 minutes)
- Cache user details (TTL: 1 hour)
- Invalidate cache on expense/payment creation

**Cache Keys:**
```
balances:{userId}
passbook:{userId}
user:{userId}
```

### 3. Microservices Architecture

**Service Breakdown:**
```
┌─────────────────┐
│  API Gateway    │
└────────┬─────────┘
         │
    ┌────┴────┬──────────┬──────────┐
    │         │          │          │
┌───▼───┐ ┌───▼───┐ ┌───▼───┐ ┌───▼───┐
│ User  │ │Expense│ │Balance│ │Payment│
│Service│ │Service│ │Service│ │Service│
└───────┘ └───────┘ └───────┘ └───────┘
    │         │          │          │
    └─────────┴──────────┴──────────┘
              │
         ┌────▼────┐
         │Database │
         └─────────┘
```

**Service Responsibilities:**
- **User Service**: User CRUD, authentication
- **Expense Service**: Expense management, split calculations
- **Balance Service**: Balance calculations, queries
- **Payment Service**: Payment processing, settlement

**Communication:**
- Synchronous: REST API calls
- Asynchronous: Message queue (RabbitMQ/Kafka) for events

### 4. Load Balancing & High Availability

**Architecture:**
```
                    ┌─────────────┐
                    │ Load Balancer│
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌─────▼─────┐      ┌─────▼─────┐
   │ App     │       │ App       │      │ App       │
   │ Server 1 │       │ Server 2  │      │ Server 3  │
   └────┬────┘       └─────┬─────┘      └─────┬─────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    ┌──────▼───────┐
                    │   Database   │
                    │  (Primary)   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │   Database   │
                    │  (Replica)   │
                    └──────────────┘
```

### 5. Event-Driven Architecture

**Events:**
- `ExpenseCreated`: When expense is added
- `PaymentReceived`: When payment is made
- `BalanceUpdated`: When balance changes
- `DebtSettled`: When debt is fully paid

**Event Flow:**
```
ExpenseService.addExpense()
    │
    ├─→ Save to database
    │
    └─→ Publish ExpenseCreated event
            │
            ├─→ BalanceService (update balances)
            ├─→ NotificationService (send notifications)
            └─→ AnalyticsService (update metrics)
```

### 6. Search & Filtering

**Elasticsearch Integration:**
- Index expenses for full-text search
- Search by expense name, notes, participants
- Filter by date range, amount range, split type
- Faceted search for analytics

### 7. Real-time Updates

**WebSocket Implementation:**
- Real-time balance updates
- Live expense notifications
- Collaborative expense editing
- Push notifications for payments

### 8. Batch Processing

**For Large Operations:**
- Batch expense imports
- Bulk balance calculations
- Scheduled simplification runs
- Daily balance summaries

**Implementation:**
- Use job queue (Celery/Bull)
- Process in batches of 1000
- Progress tracking
- Error handling and retries

### 9. Analytics & Reporting

**Features:**
- Spending trends
- User spending patterns
- Group spending analysis
- Debt settlement rates
- Expense categories

**Implementation:**
- Data warehouse (Redshift/BigQuery)
- ETL pipeline
- Dashboard (Grafana/Tableau)

### 10. Security & Authentication

**Authentication:**
- JWT tokens
- OAuth 2.0 integration
- Multi-factor authentication

**Authorization:**
- Role-based access control (RBAC)
- Group permissions
- Expense visibility controls

**Data Security:**
- Encryption at rest
- Encryption in transit (HTTPS)
- PII data masking
- Audit logs

---

## Deployment Architecture

### Current: Single Server
```
┌─────────────────────┐
│   Single JVM        │
│  - All Services     │
│  - In-Memory Data   │
└─────────────────────┘
```

### Scale-Up: Containerized Microservices
```
┌─────────────────────────────────────────┐
│         Kubernetes Cluster               │
│                                         │
│  ┌──────────┐  ┌──────────┐           │
│  │   User   │  │ Expense  │           │
│  │ Service  │  │ Service  │           │
│  │ (Pod)    │  │ (Pod)    │           │
│  └──────────┘  └──────────┘           │
│                                         │
│  ┌──────────┐  ┌──────────┐           │
│  │ Balance  │  │ Payment  │           │
│  │ Service  │  │ Service  │           │
│  │ (Pod)    │  │ (Pod)    │           │
│  └──────────┘  └──────────┘           │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │      PostgreSQL (StatefulSet)    │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │      Redis (StatefulSet)          │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Monitoring & Observability

**Components:**
- **Metrics**: Prometheus + Grafana
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing**: Jaeger/Zipkin
- **Alerting**: PagerDuty/AlertManager

**Key Metrics:**
- Request rate (RPS)
- Response time (p50, p95, p99)
- Error rate
- Database connection pool usage
- Cache hit rate
- Balance calculation time

---

## Performance Considerations

### Database Optimization

**Indexes:**
- User lookups: `user_id` (primary key)
- Expense queries: `paid_by_user_id`, `created_at`
- Balance queries: `from_user_id`, `to_user_id`, `pending_amount`
- Payment queries: `from_user_id`, `to_user_id`, `payment_date`

**Query Optimization:**
- Use prepared statements
- Batch inserts for bulk operations
- Pagination for large result sets
- Read replicas for read-heavy operations

### Caching Strategy

**Cache Levels:**
1. **L1 Cache**: In-memory (current balances)
2. **L2 Cache**: Redis (user data, recent expenses)
3. **L3 Cache**: CDN (static assets, API responses)

**Cache Invalidation:**
- On expense creation: Invalidate user balances
- On payment: Invalidate both users' balances
- TTL-based expiration for stale data

### Scalability Patterns

**Horizontal Scaling:**
- Stateless services (can scale independently)
- Database sharding by user_id
- Read replicas for read scaling

**Vertical Scaling:**
- Increase server resources
- Database connection pooling
- JVM tuning

---

## Data Consistency

### Transaction Management

**ACID Properties:**
- **Atomicity**: Expense creation and balance updates in single transaction
- **Consistency**: Balance invariants maintained
- **Isolation**: Concurrent expense creation handled
- **Durability**: Database persistence

**Distributed Transactions:**
- Use Saga pattern for microservices
- Two-phase commit for critical operations
- Eventual consistency for non-critical data

### Concurrency Control

**Optimistic Locking:**
- Version field in transactions table
- Check version before update
- Retry on conflict

**Pessimistic Locking:**
- Row-level locks for balance updates
- Prevent concurrent modifications
- Use for critical balance operations

---

## Error Handling & Resilience

### Error Types

1. **Validation Errors**: Invalid input (400)
2. **Not Found Errors**: User/expense not found (404)
3. **Business Logic Errors**: Invalid split values (422)
4. **System Errors**: Database failures (500)

### Resilience Patterns

**Circuit Breaker:**
- Prevent cascading failures
- Fail fast when service is down
- Automatic recovery

**Retry Logic:**
- Exponential backoff
- Maximum retry attempts
- Idempotent operations

**Fallback Mechanisms:**
- Cache fallback when DB is down
- Default values for missing data
- Graceful degradation

---

## Summary

This HLD document outlines:
1. **Current Architecture**: In-memory, monolithic design
2. **Scale-Up Path**: Database, microservices, caching
3. **Simplify Debt Algorithm**: Graph-based transitive simplification
4. **Database Design**: Normalized schema with proper indexes
5. **API Design**: RESTful endpoints for all operations
6. **Deployment**: Containerized, scalable architecture

The design maintains extensibility while providing clear paths for scaling to handle millions of users and transactions.


