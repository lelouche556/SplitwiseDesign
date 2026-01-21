package service;

import model.Expense;

import java.util.List;

/**
 * Service class for displaying user passbook/transaction history.
 */
public class PassbookService {
    private ExpenseService expenseService;

    public PassbookService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public void showPassbook(String userId) {
        List<Expense> expenses = expenseService.getUserPassbook(userId);
        
        if (expenses.isEmpty()) {
            System.out.println("No transactions found for user: " + userId);
            return;
        }

        System.out.println("\n=== Passbook for " + userId + " ===");
        for (Expense expense : expenses) {
            System.out.println("\nExpense ID: " + expense.getExpenseId());
            System.out.println("Name: " + expense.getExpenseName());
            System.out.println("Paid by: " + expense.getPaidBy().getUserId());
            System.out.println("Amount: " + String.format("%.2f", expense.getAmount()));
            System.out.println("Split Type: " + expense.getSplitType());
            System.out.println("Participants: " + expense.getParticipants().stream()
                    .map(u -> u.getUserId())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            if (!expense.getNotes().isEmpty()) {
                System.out.println("Notes: " + expense.getNotes());
            }
            if (!expense.getImageUrls().isEmpty()) {
                System.out.println("Images: " + expense.getImageUrls().size() + " image(s)");
            }
            System.out.println("Date: " + expense.getCreatedAt());
        }
        System.out.println("\n================================\n");
    }
}

