package app;

import model.Expense;
import model.User;
import service.BalanceService;
import service.ExpenseService;
import service.PassbookService;
import service.UserService;

import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main class for expense sharing.
 * Handles input parsing and sends call to  appropriate services.
 */
public class ExpenseSharingApp {
    private final UserService userService;
    private final ExpenseService expenseService;
    private final BalanceService balanceService;
    private final PassbookService passbookService;

    public ExpenseSharingApp() {
        this.userService = new UserService();
        this.expenseService = new ExpenseService(userService);
        this.balanceService = new BalanceService(expenseService);
        this.passbookService = new PassbookService(expenseService);
    }

    public ExpenseService getExpenseService() {
        return expenseService;
    }

    public void processCommand(String command)  {
        String[] parts = command.trim().split("\\s+");
        
        if (parts.length == 0) {
            return;
        }

        String action = parts[0];

        switch (action) {
            case "SHOW":
                if (parts.length == 1) {
                    balanceService.showAllBalances();
                } else if (parts.length == 2) {
                    balanceService.showUserBalances(parts[1]);
                } else {
                    System.out.println("Invalid SHOW command");
                }
                break;

            case "EXPENSE":
                processExpense(parts);
                break;

            case "UPDATE_EXPENSE":
                processUpdateExpense(parts);
                break;

            case "PASSBOOK":
                if (parts.length == 2) {
                    passbookService.showPassbook(parts[1]);
                } else {
                    System.out.println("Invalid PASSBOOK command. Usage: PASSBOOK <user-id>");
                }
                break;

            case "SIMPLIFY":
                if (parts.length == 2) {
                    boolean enable = Boolean.parseBoolean(parts[1]);
                    expenseService.setSimplifyExpenses(enable);
                    System.out.println("Expense simplification " + (enable ? "enabled" : "disabled"));
                } else {
                    System.out.println("Invalid SIMPLIFY command. Usage: SIMPLIFY <true|false>");
                }
                break;

            default:
                System.out.println("Unknown command: " + action);
        }
    }

    private void processExpense(String[] parts) {
        try {
            if (parts.length < 5) {
                System.out.println("Invalid EXPENSE command. Not enough parameters.");
                return;
            }

            String paidByUserId = parts[1];
            double amount = Double.parseDouble(parts[2]);
            int numUsers = Integer.parseInt(parts[3]);

            if(amount <0){
                throw new NotBoundException("Amount should be in positive");
            }

            if (parts.length < 4 + numUsers + 1) {
                System.out.println("Invalid EXPENSE command. Not enough user IDs.");
                return;
            }

            List<User> participants = new ArrayList<>();
            for (int i = 4; i < 4 + numUsers; i++) {
                User user = userService.getUser(parts[i]);
                if (user == null) {
                    System.out.println("User not found: " + parts[i]);
                    return;
                }
                participants.add(user);
            }

            String splitTypeStr = parts[4 + numUsers];
            Expense.SplitType splitType = Expense.SplitType.valueOf(splitTypeStr);

            List<Double> splitValues = new ArrayList<>();
            if (splitType != Expense.SplitType.EQUAL) {
                int startIndex = 4 + numUsers + 1;
                if (parts.length < startIndex + numUsers) {
                    System.out.println("Invalid EXPENSE command. Not enough split values.");
                    return;
                }
                for (int i = startIndex; i < startIndex + numUsers; i++) {
                    splitValues.add(Double.parseDouble(parts[i]));
                }
            }

            User paidBy = userService.getUser(paidByUserId);
            if (paidBy == null) {
                System.out.println("User not found: " + paidByUserId);
                return;
            }

            String expenseId = UUID.randomUUID().toString();
            
            // Parse optional fields: expense name, notes, and image URLs
            int splitValuesEndIndex = splitType == Expense.SplitType.EQUAL 
                    ? 4 + numUsers + 1 
                    : 4 + numUsers + 1 + numUsers;
            
            String expenseName = "Expense " + expenseId.substring(0, 8);
            String notes = "";
            List<String> imageUrls = new ArrayList<>();
            
            if (parts.length > splitValuesEndIndex) {
                // First optional argument is expense name
                expenseName = parts[splitValuesEndIndex];
                splitValuesEndIndex++;
                
                // Second optional argument is notes (if not a URL)
                if (parts.length > splitValuesEndIndex && 
                    !parts[splitValuesEndIndex].startsWith("http://") && 
                    !parts[splitValuesEndIndex].startsWith("https://")) {
                    notes = parts[splitValuesEndIndex];
                    splitValuesEndIndex++;
                }
                
                // Remaining arguments are image URLs
                for (int i = splitValuesEndIndex; i < parts.length; i++) {
                    if (parts[i].startsWith("http://") || parts[i].startsWith("https://")) {
                        imageUrls.add(parts[i]);
                    }
                }
            }

            Expense expense = new Expense(expenseId, paidBy, amount, expenseName, 
                    splitType, participants, splitValues);
            
            if (!notes.isEmpty()) {
                expense.setNotes(notes);
            }
            for (String imageUrl : imageUrls) {
                expense.addImageUrl(imageUrl);
            }
            
            expenseService.addExpense(expense);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processUpdateExpense(String[] parts) {
        try {
            if (parts.length < 2) {
                System.out.println("Invalid UPDATE_EXPENSE command. Usage: UPDATE_EXPENSE <expense-id> [--name \"name\"] [--notes \"notes\"] [--images url1 url2 ...]");
                return;
            }

            String expenseId = parts[1];
            Expense expense = expenseService.getExpense(expenseId);
            if (expense == null) {
                System.out.println("Expense not found: " + expenseId);
                return;
            }

            String expenseName = null;
            String notes = null;
            List<String> imageUrls = null;

            // Parse optional flags
            for (int i = 2; i < parts.length; i++) {
                if (parts[i].equals("--name") && i + 1 < parts.length) {
                    expenseName = removeQuotes(parts[++i]);
                } else if (parts[i].equals("--notes") && i + 1 < parts.length) {
                    notes = removeQuotes(parts[++i]);
                } else if (parts[i].equals("--images")) {
                    imageUrls = new ArrayList<>();
                    i++;
                    while (i < parts.length && !parts[i].startsWith("--")) {
                        imageUrls.add(parts[i++]);
                    }
                    i--; // Adjust for loop increment
                }
            }

            expenseService.updateExpense(expenseId, expenseName, notes, imageUrls);
            System.out.println("Expense updated successfully: " + expenseId);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
//        catch (Exception e) {
//            System.out.println("Unexpected error: " + e.getMessage());
//            e.printStackTrace();
//        }
    }

    private String removeQuotes(String str) {
        if (str == null) return null;
        str = str.trim();
        if ((str.startsWith("\"") && str.endsWith("\"")) || 
            (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public void addUser(User user) {
        userService.addUser(user);
    }
}

