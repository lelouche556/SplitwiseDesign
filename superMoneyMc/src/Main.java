import app.ExpenseSharingApp;
import factory.UserFactory;
import model.User;

/**
 * Main class for the expense sharing application.
 */
public class Main {
    public static void main(String[] args) {
        // Create the expense sharing application
        ExpenseSharingApp app = new ExpenseSharingApp();

        // Create users
        User u1 = UserFactory.createUser("u1", "User1", "user1@example.com", "1234567890");
        User u2 = UserFactory.createUser("u2", "User2", "user2@example.com", "1234567891");
        User u3 = UserFactory.createUser("u3", "User3", "user3@example.com", "1234567892");
        User u4 = UserFactory.createUser("u4", "User4", "user4@example.com", "1234567893");

        // Add users to the application
        app.addUser(u1);
        app.addUser(u2);
        app.addUser(u3);
        app.addUser(u4);

        // Sample input from problem statement
        String[] commands = {
            "SHOW",
            "SHOW u1",
            "EXPENSE u1 1000 4 u1 u2 u3 u4 EQUAL",
            "SHOW u4",
            "SHOW u1",
            "EXPENSE u1 1250 2 u2 u3 EXACT 370 880",
            "SHOW",
            "EXPENSE u4 1200 4 u1 u2 u3 u4 PERCENT 40 20 20 20",
            "SHOW u1",
            "SHOW"
        };

        System.out.println("=== Running Sample Test Cases ===\n");
        
        for (String command : commands) {
            System.out.println("Command: " + command);
            app.processCommand(command);
            System.out.println();
        }

        // Bonus features demonstration
        System.out.println("\n=== Bonus Features Demo ===\n");
        
        // Show passbook for u1
        System.out.println("Showing passbook for u1:");
        app.processCommand("PASSBOOK u1");
        
        // Test SHARE split type
        System.out.println("Testing SHARE split type:");
        System.out.println("Command: EXPENSE u4 1200 4 u1 u2 u3 u4 SHARE 2 1 1 1");
        app.processCommand("EXPENSE u4 1200 4 u1 u2 u3 u4 SHARE 2 1 1 1");
        app.processCommand("SHOW u1");
        
        // Test expense simplification
        System.out.println("\n=== Testing Expense Simplification ===\n");
        System.out.println("Enabling expense simplification:");
        app.processCommand("SIMPLIFY true");
        app.processCommand("SHOW");
    }
}