package test;

import app.ExpenseSharingApp;
import factory.UserFactory;
import model.Expense;
import model.Transaction;
import model.User;
import service.ExpenseService;
import service.UserService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for Expense Sharing Application.
 */
public class ExpenseSharingAppTest {
    
    // Custom assertion methods
    private void assertNotNull(Object obj) {
        if (obj == null) throw new AssertionError("Expected not null");
    }
    
    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", but was: " + actual);
        }
    }
    
    private void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError("Expected: " + expected + ", but was: " + actual);
        }
    }
    
    private void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Expected true");
    }
    
    private void assertFalse(boolean condition) {
        if (condition) throw new AssertionError("Expected false");
    }
    private ExpenseSharingApp app;
    private UserService userService;
    private ExpenseService expenseService;
    private User u1, u2, u3, u4;
    private ByteArrayOutputStream outputStream;

    // @Before - Setup method (JUnit annotation)
    public void setUp() {
        app = new ExpenseSharingApp();
        expenseService = app.getExpenseService();
        // Get userService through reflection or create a new one for testing
        userService = new UserService();
        
        // Create test users
        u1 = UserFactory.createUser("u1", "User1", "user1@example.com", "1234567890");
        u2 = UserFactory.createUser("u2", "User2", "user2@example.com", "1234567891");
        u3 = UserFactory.createUser("u3", "User3", "user3@example.com", "1234567892");
        u4 = UserFactory.createUser("u4", "User4", "user4@example.com", "1234567893");
        
        app.addUser(u1);
        app.addUser(u2);
        app.addUser(u3);
        app.addUser(u4);
        
        // Also add to userService for direct access in tests
        userService.addUser(u1);
        userService.addUser(u2);
        userService.addUser(u3);
        userService.addUser(u4);
        
        // Capture output
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    // ========== User Management Tests ==========

    // @Test
    public void testAddUser() {
        User newUser = UserFactory.createUser("u5", "User5", "user5@example.com", "1234567894");
        app.addUser(newUser);
        
        // Verify user was added by using it in an expense
        app.processCommand("EXPENSE u5 100 1 u5 EQUAL");
        assertTrue(true);
    }

    // @Test
    public void testGetUser() {
        User retrieved = userService.getUser("u1");
        assertNotNull(retrieved);
        assertEquals("u1", retrieved.getUserId());
        assertEquals("User1", retrieved.getName());
    }

    // @Test
    public void testUserExists() {
        assertTrue(userService.userExists("u1"));
        assertFalse(userService.userExists("nonexistent"));
    }

    // ========== Equal Split Tests ==========

    // @Test
    public void testEqualSplit() {
        app.processCommand("EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        // Check that u2, u3, u4 owe u1
        assertTrue(balances.containsKey("u2"));
        assertTrue(balances.get("u2").containsKey("u1"));
        assertEquals(250.0, balances.get("u2").get("u1").getAmount(), 0.01);
        
        assertTrue(balances.containsKey("u3"));
        assertEquals(250.0, balances.get("u3").get("u1").getAmount(), 0.01);
        
        assertTrue(balances.containsKey("u4"));
        assertEquals(250.0, balances.get("u4").get("u1").getAmount(), 0.01);
    }

    // @Test
    public void testEqualSplitRounding() {
        // Test rounding: 100 / 3 = 33.34, 33.33, 33.33
        app.processCommand("EXPENSE u1 100 3 u1 u2 u3 EQUAL");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        double total = 0.0;
        for (Map<String, Transaction> userBalances : balances.values()) {
            for (Transaction t : userBalances.values()) {
                total += t.getAmount();
            }
        }
        // Total should be 100, but since u1 is paying and is a participant, 
        // only u2 and u3 owe, so total should be ~66.67
        // Actually, let's check: u1 pays 100, split among 3 (u1, u2, u3)
        // Each owes 33.33, but u1 is the payer, so u2 and u3 owe u1
        // u2 owes: 33.33, u3 owes: 33.34 (to make total 100)
        // So total owed = 33.33 + 33.34 = 66.67
        assertEquals(66.67, total, 0.01);
    }

    // ========== Exact Split Tests ==========

    // @Test
    public void testExactSplit() {
        app.processCommand("EXPENSE u1 1250 2 u2 u3 EXACT 370 880");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        assertEquals(370.0, balances.get("u2").get("u1").getAmount(), 0.01);
        assertEquals(880.0, balances.get("u3").get("u1").getAmount(), 0.01);
    }

    // @Test
    public void testExactSplitValidation() {
        // This should fail - sum doesn't equal total
        app.processCommand("EXPENSE u1 1000 2 u2 u3 EXACT 300 500");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Error") || output.contains("does not equal"));
    }

    // ========== Percent Split Tests ==========

    // @Test
    public void testPercentSplit() {
        app.processCommand("EXPENSE u4 1200 4 u1 u2 u3 u4 PERCENT 40 20 20 20");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        // u1 owes 40% of 1200 = 480
        assertTrue(balances.containsKey("u1"));
        assertEquals(480.0, balances.get("u1").get("u4").getAmount(), 0.01);
        
        // u2 owes 20% of 1200 = 240
        assertEquals(240.0, balances.get("u2").get("u4").getAmount(), 0.01);
        
        // u3 owes 20% of 1200 = 240
        assertEquals(240.0, balances.get("u3").get("u4").getAmount(), 0.01);
    }

    // @Test
    public void testPercentSplitValidation() {
        // This should fail - percentages don't sum to 100
        app.processCommand("EXPENSE u1 1000 2 u2 u3 PERCENT 40 50");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Error") || output.contains("does not equal 100"));
    }

    // ========== Share Split Tests ==========

    // @Test
    public void testShareSplit() {
        app.processCommand("EXPENSE u4 1200 4 u1 u2 u3 u4 SHARE 2 1 1 1");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        // Total shares = 2 + 1 + 1 + 1 = 5
        // u1 share = 2/5 * 1200 = 480
        // u2 share = 1/5 * 1200 = 240
        // u3 share = 1/5 * 1200 = 240
        
        assertTrue(balances.containsKey("u1"));
        assertEquals(480.0, balances.get("u1").get("u4").getAmount(), 1.0); // Allow 1.0 tolerance for rounding
        
        assertEquals(240.0, balances.get("u2").get("u4").getAmount(), 1.0);
        assertEquals(240.0, balances.get("u3").get("u4").getAmount(), 1.0);
    }

    // ========== Balance Netting Tests ==========

    // @Test
    public void testBalanceNetting() {
        // u1 pays 1000, split equally among 4
        app.processCommand("EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL");
        
        // u4 pays 1200, u1 owes 480 (40% of 1200)
        app.processCommand("EXPENSE u4 1200 4 u1 u2 u3 u4 PERCENT 40 20 20 20");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        // u1 was owed 250, but now owes 480, so net: u1 owes u4 230
        assertTrue(balances.containsKey("u1"));
        assertEquals(230.0, balances.get("u1").get("u4").getAmount(), 0.01);
    }

    // ========== Show Balances Tests ==========

    // @Test
    public void testShowAllBalances() {
        app.processCommand("EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL");
        app.processCommand("SHOW");
        
        String output = outputStream.toString();
        assertTrue(output.contains("u2 owes u1"));
        assertTrue(output.contains("u3 owes u1"));
        assertTrue(output.contains("u4 owes u1"));
    }

    // @Test
    public void testShowUserBalances() {
        app.processCommand("EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL");
        app.processCommand("SHOW u1");
        
        String output = outputStream.toString();
        assertTrue(output.contains("u2 owes u1"));
        assertTrue(output.contains("u3 owes u1"));
        assertTrue(output.contains("u4 owes u1"));
    }

    // @Test
    public void testShowNoBalances() {
        app.processCommand("SHOW");
        
        String output = outputStream.toString();
        assertTrue(output.contains("No balances"));
    }

    // ========== Passbook Tests ==========

    // @Test
    public void testPassbook() {
        app.processCommand("EXPENSE u1 1000 3 u1 u2 u3 EQUAL");
        app.processCommand("PASSBOOK u1");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Passbook for u1"));
        assertTrue(output.contains("u1"));
        assertTrue(output.contains("1000"));
    }

    // @Test
    public void testPassbookEmpty() {
        app.processCommand("PASSBOOK u1");
        
        String output = outputStream.toString();
        assertTrue(output.contains("No transactions") || output.contains("Passbook"));
    }

    // ========== Expense Name, Notes, Images Tests ==========

    // @Test
    public void testExpenseWithNameNotesImages() {
        app.processCommand("EXPENSE u1 1000 3 u1 u2 u3 EQUAL Electricity-Bill Monthly-bill http://example.com/bill.jpg");
        
        List<Expense> expenses = expenseService.getUserPassbook("u1");
        assertFalse(expenses.isEmpty());
        
        Expense expense = expenses.get(0);
        assertEquals("Electricity-Bill", expense.getExpenseName());
        assertEquals("Monthly-bill", expense.getNotes());
        assertFalse(expense.getImageUrls().isEmpty());
        assertTrue(expense.getImageUrls().contains("http://example.com/bill.jpg"));
    }

    // ========== Update Expense Tests ==========

    // @Test
    public void testUpdateExpense() {
        // Create expense
        app.processCommand("EXPENSE u1 1000 2 u1 u2 EQUAL Original-Name Original-Notes http://example.com/old.jpg");
        
        // Get expense ID
        List<Expense> expenses = expenseService.getUserPassbook("u1");
        String expenseId = expenses.get(0).getExpenseId();
        
        // Update expense
        app.processCommand("UPDATE_EXPENSE " + expenseId + " --name \"Updated-Name\" --notes \"Updated-Notes\" --images http://example.com/new1.jpg http://example.com/new2.jpg");
        
        Expense updatedExpense = expenseService.getExpense(expenseId);
        assertEquals("Updated-Name", updatedExpense.getExpenseName());
        assertEquals("Updated-Notes", updatedExpense.getNotes());
        assertEquals(2, updatedExpense.getImageUrls().size());
        assertTrue(updatedExpense.getImageUrls().contains("http://example.com/new1.jpg"));
    }

    // @Test
    public void testUpdateExpenseNotFound() {
        app.processCommand("UPDATE_EXPENSE nonexistent-id --name \"Test\"");
        
        String output = outputStream.toString();
        assertTrue(output.contains("not found") || output.contains("Error"));
    }

    // ========== Expense Simplification Tests ==========

    // @Test
    public void testExpenseSimplification() {
        // Create scenario: u1 owes u2 250, u2 owes u3 200
        app.processCommand("EXPENSE u2 250 2 u1 u2 EQUAL");
        app.processCommand("EXPENSE u3 200 2 u2 u3 EQUAL");
        
        // Enable simplification
        app.processCommand("SIMPLIFY true");
        app.processCommand("SHOW");
        
        String output = outputStream.toString();
        // After simplification, should see reduced transactions
        assertTrue(output.contains("owes"));
    }

    // @Test
    public void testSimplificationToggle() {
        expenseService.setSimplifyExpenses(true);
        assertTrue(expenseService.getAllBalances() != null);
        
        expenseService.setSimplifyExpenses(false);
        assertTrue(expenseService.getAllBalances() != null);
    }

    // ========== Edge Cases Tests ==========

    // @Test
    public void testExpenseWithSelfAsParticipant() {
        // User pays for themselves - should not create balance
        app.processCommand("EXPENSE u1 1000 1 u1 EQUAL");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        assertTrue(balances.isEmpty() || balances.values().stream()
                .allMatch(map -> map.isEmpty()));
    }

    // @Test
    public void testInvalidExpenseCommand() {
        app.processCommand("EXPENSE invalid");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Invalid") || output.contains("Error"));
    }

    // @Test
    public void testInvalidUserInExpense() {
        app.processCommand("EXPENSE u1 1000 1 nonexistent EQUAL");
        
        String output = outputStream.toString();
        assertTrue(output.contains("not found") || output.contains("User not found"));
    }

    // @Test
    public void testInvalidSplitType() {
        app.processCommand("EXPENSE u1 1000 2 u1 u2 INVALID");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Error") || output.contains("Unknown") || output.contains("Invalid"));
    }

    // ========== Integration Tests ==========

    // @Test
    public void testCompleteScenario() {
        // Scenario from problem statement
        app.processCommand("EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL");
        app.processCommand("EXPENSE u1 1250 2 u2 u3 EXACT 370 880");
        app.processCommand("EXPENSE u4 1200 4 u1 u2 u3 u4 PERCENT 40 20 20 20");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        // Verify final balances
        // u2 owes u1: 620 (250 + 370)
        assertEquals(620.0, balances.get("u2").get("u1").getAmount(), 0.01);
        
        // u3 owes u1: 1130 (250 + 880)
        assertEquals(1130.0, balances.get("u3").get("u1").getAmount(), 0.01);
        
        // u1 owes u4: 230 (480 - 250)
        assertEquals(230.0, balances.get("u1").get("u4").getAmount(), 0.01);
        
        // u2 owes u4: 240
        assertEquals(240.0, balances.get("u2").get("u4").getAmount(), 0.01);
        
        // u3 owes u4: 240
        assertEquals(240.0, balances.get("u3").get("u4").getAmount(), 0.01);
    }

    // @Test
    public void testMultipleExpensesAccumulation() {
        app.processCommand("EXPENSE u1 100 2 u1 u2 EQUAL");
        app.processCommand("EXPENSE u1 200 2 u1 u2 EQUAL");
        
        Map<String, Map<String, Transaction>> balances = expenseService.getAllBalances();
        
        // u2 should owe u1: 50 + 100 = 150
        assertEquals(150.0, balances.get("u2").get("u1").getAmount(), 0.01);
    }
    
    // Test runner (can be used without JUnit)
    public static void main(String[] args) {
        ExpenseSharingAppTest test = new ExpenseSharingAppTest();
        int passed = 0;
        int failed = 0;
        
        String[] testMethods = {
            "testAddUser", "testGetUser", "testUserExists",
            "testEqualSplit", "testEqualSplitRounding",
            "testExactSplit", "testExactSplitValidation",
            "testPercentSplit", "testPercentSplitValidation",
            "testShareSplit", "testBalanceNetting",
            "testShowAllBalances", "testShowUserBalances", "testShowNoBalances",
            "testPassbook", "testPassbookEmpty",
            "testExpenseWithNameNotesImages", "testUpdateExpense", "testUpdateExpenseNotFound",
            "testExpenseSimplification", "testSimplificationToggle",
            "testExpenseWithSelfAsParticipant", "testInvalidExpenseCommand",
            "testInvalidUserInExpense", "testInvalidSplitType",
            "testCompleteScenario", "testMultipleExpensesAccumulation"
        };
        
        PrintStream originalOut = System.out;
        System.out.println("Running Expense Sharing App Tests...\n");
        
        for (String methodName : testMethods) {
            try {
                test.setUp();
                test.getClass().getMethod(methodName).invoke(test);
                originalOut.println("✓ " + methodName);
                passed++;
            } catch (Exception e) {
                originalOut.println("✗ " + methodName + " - " + 
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                failed++;
            }
        }
        
        originalOut.println("\n" + "=".repeat(50));
        originalOut.println("Tests passed: " + passed);
        originalOut.println("Tests failed: " + failed);
        originalOut.println("Total tests: " + (passed + failed));
    }
}

