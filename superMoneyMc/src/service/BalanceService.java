package service;

import model.Transaction;

import java.util.Map;

/**
 * Service class for displaying balances.
 */
public class BalanceService {
    private ExpenseService expenseService;

    public BalanceService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public void showAllBalances() {
        Map<String, Map<String, Transaction>> allBalances = expenseService.getAllBalances();
        boolean hasBalances = false;

        for (Map.Entry<String, Map<String, Transaction>> userEntry : allBalances.entrySet()) {
            Map<String, Transaction> userBalances = userEntry.getValue();

            for (Transaction transaction : userBalances.values()) {
                if (Math.abs(transaction.getAmount()) > 0.01) {
                    hasBalances = true;
                    double amount = transaction.getAmount();
                    String amountStr = amount == Math.floor(amount) 
                            ? String.format("%.0f", amount) 
                            : String.format("%.2f", amount);
                    System.out.println(transaction.getFromUser().getUserId() + 
                            " owes " + transaction.getToUser().getUserId() + 
                            ": " + amountStr);
                }
            }
        }

        if (!hasBalances) {
            System.out.println("No balances");
        }
    }

    public void showUserBalances(String userId) {
        Map<String, Transaction> userBalances = expenseService.getUserBalances(userId);
        
        if (userBalances.isEmpty()) {
            System.out.println("No balances");
            return;
        }

        for (Transaction transaction : userBalances.values()) {
            if (Math.abs(transaction.getAmount()) > 0.01) {
                double amount = transaction.getAmount();
                String amountStr = amount == Math.floor(amount) 
                        ? String.format("%.0f", amount) 
                        : String.format("%.2f", amount);
                System.out.println(transaction.getFromUser().getUserId() + 
                        " owes " + transaction.getToUser().getUserId() + 
                        ": " + amountStr);
            }
        }
    }
}

