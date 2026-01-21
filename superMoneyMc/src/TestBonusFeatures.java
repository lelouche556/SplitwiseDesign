import app.ExpenseSharingApp;
import factory.UserFactory;
import model.User;
import service.ExpenseService;

/**
 * Test class for bonus features:
 * 1. Expense name, notes, and images
 * 2. Update expense functionality
 * 3. Expense simplification
 */
public class TestBonusFeatures {
    public static void main(String[] args) {
        ExpenseSharingApp app = new ExpenseSharingApp();

        // Create users
        User u1 = UserFactory.createUser("u1", "User1", "user1@example.com", "1234567890");
        User u2 = UserFactory.createUser("u2", "User2", "user2@example.com", "1234567891");
        User u3 = UserFactory.createUser("u3", "User3", "user3@example.com", "1234567892");

        app.addUser(u1);
        app.addUser(u2);
        app.addUser(u3);

        System.out.println("=== Testing Expense Name, Notes, and Images ===\n");
        
        // Create expense with name, notes, and images
        System.out.println("Creating expense with name, notes, and images:");
        app.processCommand("EXPENSE u1 1000 3 u1 u2 u3 EQUAL Electricity-Bill Monthly-bill http://example.com/bill1.jpg http://example.com/bill2.jpg");
        
        System.out.println("\nShowing passbook for u1:");
        app.processCommand("PASSBOOK u1");

        System.out.println("\n=== Testing Update Expense ===\n");
        
        // Get expense ID from service (in real app, we'd get it from user input or return value)
        ExpenseService expenseService = app.getExpenseService();
        String expenseId = expenseService.getUserPassbook("u1").get(0).getExpenseId();
        
        System.out.println("Updating expense: " + expenseId);
        System.out.println("Before update:");
        app.processCommand("PASSBOOK u1");
        
        System.out.println("\n=== Updating Expense ===\n");
        app.processCommand("UPDATE_EXPENSE " + expenseId + " --name \"Updated-Electricity-Bill\" --notes \"Updated-Monthly-bill\" --images http://example.com/new1.jpg http://example.com/new2.jpg");
        
        System.out.println("\nAfter update:");
        app.processCommand("PASSBOOK u1");

        System.out.println("\n=== Testing Expense Simplification ===\n");
        
        // Creating a scenario where simplification helps
        System.out.println("Creating expenses that can be simplified:");
        app.processCommand("EXPENSE u1 250 2 u1 u2 EQUAL");
        app.processCommand("EXPENSE u2 200 2 u2 u3 EQUAL");
        
        System.out.println("\nBalances before simplification:");
        app.processCommand("SHOW");
        
        System.out.println("\nEnabling simplification:");
        app.processCommand("SIMPLIFY true");
        
        System.out.println("\nBalances after simplification:");
        app.processCommand("SHOW");
    }
}

