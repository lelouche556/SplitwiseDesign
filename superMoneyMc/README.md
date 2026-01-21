# Expense Sharing Application

An expense sharing application built in Java that allows users to track expenses and split them among multiple people. The application keeps track of balances between users, showing who owes how much to whom.

## Features

### Core Features

- **User Management**: Create and manage users with userId, name, email, and mobile number
- **Multiple Split Types**:
  - **EQUAL**: Split expense equally among all participants
  - **EXACT**: Split expense with exact amounts specified for each participant
  - **PERCENT**: Split expense based on percentage shares
  - **SHARE**: Split expense based on proportional share values
- **Balance Tracking**: Tracks and updates balances between users
- **Balance Netting**: Nets out mutual debts (e.g., if A owes B and B owes A)
- **Validation**: 
  - Validates that PERCENT splits sum to 100%
  - Validates that EXACT splits sum to the total amount
- **Round-off Handling**: Handles rounding to 2 decimal places, ensuring totals match exactly
- **Show Balances**: View balances for a single user or all users

### Bonus Features

- **Expense Metadata**: Add expense name, notes, and image URLs
- **Passbook**: View transaction history for any user
- **Expense Simplification**: Minimize number of transactions using graph algorithms
- **Update Expenses**: Edit expense name, notes, and images after creation

## Architecture

The application uses separation of concerns:

### Design Patterns Used

1. **Strategy Pattern**: Used for different expense split types
   - `SplitStrategy` interface
   - Implementations: `EqualSplitStrategy`, `ExactSplitStrategy`, `PercentSplitStrategy`, `ShareSplitStrategy`

2. **Factory Pattern**: Used for creating users and split strategies
   - `UserFactory`: Creates user instances with validation
   - `SplitStrategyFactory`: Creates appropriate split strategy based on split type

### Package Structure

```
src/
├── model/          # Domain models (User, Expense, Transaction)
├── strategy/       # Split strategy implementations
├── factory/        # Factory classes for object creation
├── service/        # Business logic services
│   ├── UserService.java
│   ├── ExpenseService.java
│   ├── BalanceService.java
│   └── PassbookService.java
└── app/           # Application layer (command processing)
    └── ExpenseSharingApp.java
```

### Key Components

- **UserService**: Manages user CRUD operations
- **ExpenseService**: Handles expense creation, balance updates, and simplification
- **BalanceService**: Displays balances in formatted output
- **PassbookService**: Shows transaction history for users

## Getting Started

### Prerequisites

- Java 8 or higher
- JUnit 4 (for running tests)

### Compilation

```bash
# Compile all source files
javac -d out src/model/*.java src/strategy/*.java src/factory/*.java src/service/*.java src/app/*.java src/Main.java

# Run the application
java -cp out Main
```

### Running Tests

The test file includes a built-in test runner that works without JUnit:

```bash
# Compile test files
javac -d out src/model/*.java src/strategy/*.java src/factory/*.java src/service/*.java src/app/*.java src/test/*.java

# Run tests
java -cp out test.ExpenseSharingAppTest
```

**With JUnit 4 (optional):**

```bash
# Download JUnit 4 and Hamcrest, then:
javac -cp ".:junit-4.13.2.jar:hamcrest-core-1.3.jar" -d out src/**/*.java src/test/*.java
java -cp ".:out:junit-4.13.2.jar:hamcrest-core-1.3.jar" org.junit.runner.JUnitCore test.ExpenseSharingAppTest
```

The test suite includes tests covering:
- User management (add, get, exists)
- All split types (EQUAL, EXACT, PERCENT, SHARE)
- Balance tracking and netting
- Passbook functionality
- Expense updates (name, notes, images)
- Expense simplification
- Edge cases and error handling
- Integration scenarios

## Usage

### Creating Users

Users are created programmatically using `UserFactory`:

```java
User user = UserFactory.createUser("u1", "User1", "user1@example.com", "1234567890");
app.addUser(user);
```

### Commands

#### 1. EXPENSE - Add an Expense

**Format:**
```
EXPENSE <user-id-of-person-who-paid> <amount> <no-of-users> <space-separated-user-ids> <split-type> [split-values] [expense-name] [notes] [image-urls...]
```

**Examples:**

```bash
# Equal split
EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL

# Exact split
EXPENSE u1 1250 2 u2 u3 EXACT 370 880

# Percent split
EXPENSE u4 1200 4 u1 u2 u3 u4 PERCENT 40 20 20 20

# Share split
EXPENSE u4 1200 4 u1 u2 u3 u4 SHARE 2 1 1 1

# With expense name, notes, and images
EXPENSE u1 1000 3 u1 u2 u3 EQUAL Electricity-Bill Monthly-bill http://example.com/bill1.jpg http://example.com/bill2.jpg
```

#### 2. SHOW - Display Balances

**Format:**
```
SHOW                    # Show all balances
SHOW <user-id>          # Show balances for a specific user
```

**Examples:**

```bash
SHOW                    # Show all balances
SHOW u1                 # Show balances for user u1
```

**Output Format:**
```
<user-id-of-x> owes <user-id-of-y>: <amount>
```

#### 3. PASSBOOK - View Transaction History

**Format:**
```
PASSBOOK <user-id>
```

**Example:**

```bash
PASSBOOK u1
```

Displays all expenses the user was involved in (either paid or participated).

#### 4. UPDATE_EXPENSE - Update Expense Details

**Format:**
```
UPDATE_EXPENSE <expense-id> [--name "name"] [--notes "notes"] [--images url1 url2 ...]
```

**Example:**

```bash
UPDATE_EXPENSE <expense-id> --name "Updated Name" --notes "Updated notes" --images http://example.com/new.jpg
```

#### 5. SIMPLIFY - Toggle Expense Simplification

**Format:**
```
SIMPLIFY <true|false>
```

**Example:**

```bash
SIMPLIFY true   # Enable expense simplification
SIMPLIFY false  # Disable expense simplification
```

## Example Usage

### Complete Scenario

```bash
# Create users (done programmatically)
# u1, u2, u3, u4

# Show initial balances (should be empty)
SHOW

# User1 pays electricity bill of 1000, split equally
EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL

# Check balances
SHOW u1
# Output:
# u2 owes u1: 250
# u3 owes u1: 250
# u4 owes u1: 250

# User1 buys items for u2 and u3 with exact amounts
EXPENSE u1 1250 2 u2 u3 EXACT 370 880

# Check all balances
SHOW
# Output:
# u2 owes u1: 620
# u3 owes u1: 1130
# u4 owes u1: 250

# User4 pays restaurant bill, split by percentage
EXPENSE u4 1200 4 u1 u2 u3 u4 PERCENT 40 20 20 20

# Check balances (with netting)
SHOW
# Output:
# u1 owes u4: 230
# u2 owes u1: 620
# u2 owes u4: 240
# u3 owes u1: 1130
# u3 owes u4: 240
```

## Split Types Explained

### EQUAL Split

Splits the expense equally among all participants. Handles rounding to ensure the total matches exactly.

**Example:** 1000 split among 3 people = 333.34, 333.33, 333.33

### EXACT Split

Each participant pays an exact specified amount. The sum must equal the total expense amount.

**Example:** 1250 split as 370 and 880

### PERCENT Split

Splits based on percentage shares. Percentages must sum to 100%.

**Example:** 1200 split as 40%, 20%, 20%, 20% = 480, 240, 240, 240

### SHARE Split

Splits proportionally based on share values. Each participant gets a share value, and the amount is split proportionally.

**Example:** 1200 with shares 2, 1, 1, 1 (total shares = 5)
- Share 2: (2/5) × 1200 = 480
- Share 1: (1/5) × 1200 = 240 each

## Expense Simplification

When enabled, the application uses graph algorithms to minimize the number of transactions by:
- Netting out mutual debts (A owes B, B owes A → net amount)
- Simplifying chains (A owes B, B owes C → A owes C if beneficial)

**Example:**
- Before: User1 owes User2: 250, User2 owes User3: 200
- After: User1 owes User2: 50, User1 owes User3: 200

## Validation Rules

1. **PERCENT Split**: Sum of percentages must equal 100%
2. **EXACT Split**: Sum of exact amounts must equal total expense amount
3. **User Validation**: All user IDs in expense must exist
4. **Amount Validation**: Amount must be a valid positive number

## Error Handling

The application handles various error cases:
- Invalid user IDs
- Invalid split type
- Invalid split values (percentages not summing to 100, exact amounts not matching total)
- Invalid expense IDs for updates
- Invalid command formats

## Testing

Unit tests are provided in `src/test/ExpenseSharingAppTest.java` covering:
- User management
- All split types (EQUAL, EXACT, PERCENT, SHARE)
- Balance tracking and netting
- Passbook functionality
- Expense updates
- Expense simplification
- Edge cases and error handling

Run tests using JUnit 4.

## Design Decisions

1. **In-Memory Storage**: All data is stored in memory (no database layer) as per requirements
2. **Service Layer Separation**: Separation between UserService and ExpenseService
3. **Strategy Pattern**: Allows adding new split types without modifying existing code
4. **Factory Pattern**: Centralizes object creation logic with validation
5. **Lazy Balance Initialization**: Balance entries are created when needed

## Future Enhancements

Possible improvements:
- Delete expense functionality
- Settle up feature (mark debts as paid)
- Export balances to CSV/JSON
- Group/room management
- Currency support
- Recurring expenses
- Expense categories/tags

## License

This project is provided as-is for educational purposes.

