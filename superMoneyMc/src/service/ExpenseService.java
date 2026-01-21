package service;

import factory.SplitStrategyFactory;
import model.Expense;
import model.Transaction;
import model.User;
import strategy.SplitStrategy;

import java.util.*;

/**
 * Service class for managing expenses and transactions.
 */
public class ExpenseService {
    private final UserService userService;
    private final Map<String, Expense> expenses;
    private Map<String, Map<String, Transaction>> balances; // userId -> (userId -> Transaction)
    private final List<Expense> expenseHistory;
    private boolean simplifyExpenses;

    public ExpenseService(UserService userService) {
        if (userService == null) {
            throw new IllegalArgumentException("UserService cannot be null");
        }
        this.userService = userService;
        this.expenses = new HashMap<>();
        this.balances = new HashMap<>();
        this.expenseHistory = new ArrayList<>();
        this.simplifyExpenses = false;
    }

    /**
     * Ensures a balance entry exists for a user (lazy initialization).
     */
    private void ensureBalanceEntry(String userId) {
        balances.putIfAbsent(userId, new HashMap<>());
    }

    public void addExpense(Expense expense) {
        expenses.put(expense.getExpenseId(), expense);
        expenseHistory.add(expense);
        
        SplitStrategy strategy = SplitStrategyFactory.createStrategy(expense.getSplitType());
        Map<User, Double> splitMap = strategy.calculateSplit(expense);
        
        updateBalances(expense.getPaidBy(), splitMap);
        
        if (simplifyExpenses) {
            simplifyBalances();
        }
    }

    public Expense getExpense(String expenseId) {
        return expenses.get(expenseId);
    }

    public void updateExpense(String expenseId, String expenseName, String notes, List<String> imageUrls) {
        Expense expense = expenses.get(expenseId);
        if (expense == null) {
            throw new IllegalArgumentException("Expense not found: " + expenseId);
        }
        
        if (expenseName != null && !expenseName.trim().isEmpty()) {
            expense.setExpenseName(expenseName);
        }
        
        if (notes != null) {
            expense.setNotes(notes);
        }
        
        if (imageUrls != null) {
            expense.getImageUrls().clear();
            for (String imageUrl : imageUrls) {
                expense.addImageUrl(imageUrl);
            }
        }
    }

    private void updateBalances(User paidBy, Map<User, Double> splitMap) {
        for (Map.Entry<User, Double> entry : splitMap.entrySet()) {
            User participant = entry.getKey();
            double amount = entry.getValue();
            
            // Skip if participant is the one who paid
            if (participant.equals(paidBy)) {
                continue;
            }
            
            String participantId = participant.getUserId();
            String paidById = paidBy.getUserId();
            
            // Ensure balance entries exist
            ensureBalanceEntry(participantId);
            ensureBalanceEntry(paidById);
            
            // Check if reverse transaction exists (paidBy owes participant)
            Map<String, Transaction> paidByBalances = balances.get(paidById);
            Transaction reverseTransaction = paidByBalances.get(participantId);
            
            if (reverseTransaction != null && reverseTransaction.getAmount() > 0.01) {
                // Net out the amounts
                double existingAmount = reverseTransaction.getAmount();
                if (amount >= existingAmount) {
                    // Participant now owes paidBy (net amount)
                    reverseTransaction.setAmount(0);
                    
                    // Update participant's balance to paidBy
                    Map<String, Transaction> participantBalances = balances.get(participantId);
                    Transaction transaction = participantBalances.get(paidById);
                    if (transaction == null) {
                        transaction = new Transaction(participant, paidBy, amount - existingAmount);
                        participantBalances.put(paidById, transaction);
                    } else {
                        transaction.setAmount(transaction.getAmount() + amount - existingAmount);
                    }
                } else {
                    // PaidBy still owes participant (net amount)
                    reverseTransaction.setAmount(existingAmount - amount);
                }
            } else {
                // No reverse transaction, create normal transaction
                Map<String, Transaction> participantBalances = balances.get(participantId);
                Transaction transaction = participantBalances.get(paidById);
                if (transaction == null) {
                    transaction = new Transaction(participant, paidBy, amount);
                    participantBalances.put(paidById, transaction);
                } else {
                    transaction.addAmount(amount);
                }
            }
        }
    }

    public Map<String, Map<String, Transaction>> getAllBalances() {
        if (simplifyExpenses) {
            return getSimplifiedBalances();
        }
        return balances;
    }

    public Map<String, Transaction> getUserBalances(String userId) {
        Map<String, Transaction> userBalances = new HashMap<>();
        
        if (!balances.containsKey(userId)) {
            // Still check if others owe this user
            for (Map.Entry<String, Map<String, Transaction>> entry : balances.entrySet()) {
                if (!entry.getKey().equals(userId)) {
                    Map<String, Transaction> otherBalances = entry.getValue();
                    Transaction transaction = otherBalances.get(userId);
                    if (transaction != null && Math.abs(transaction.getAmount()) > 0.01) {
                        // Create reverse transaction for display
                        User fromUser = userService.getUser(entry.getKey());
                        User toUser = userService.getUser(userId);
                        if (fromUser != null && toUser != null) {
                            Transaction reverseTransaction = new Transaction(
                                    fromUser, 
                                    toUser, 
                                    transaction.getAmount());
                            userBalances.put(entry.getKey(), reverseTransaction);
                        }
                    }
                }
            }
            return userBalances;
        }
        
        Map<String, Transaction> directBalances = balances.get(userId);
        
        // Add balances where this user owes others
        for (Transaction transaction : directBalances.values()) {
            if (Math.abs(transaction.getAmount()) > 0.01) {
                userBalances.put(transaction.getToUser().getUserId(), transaction);
            }
        }
        
        // Add balances where others owe this user
        for (Map.Entry<String, Map<String, Transaction>> entry : balances.entrySet()) {
            if (!entry.getKey().equals(userId)) {
                Map<String, Transaction> otherBalances = entry.getValue();
                Transaction transaction = otherBalances.get(userId);
                if (transaction != null && Math.abs(transaction.getAmount()) > 0.01) {
                    // Create reverse transaction for display
                    User fromUser = userService.getUser(entry.getKey());
                    User toUser = userService.getUser(userId);
                    if (fromUser != null && toUser != null) {
                        Transaction reverseTransaction = new Transaction(
                                fromUser, 
                                toUser, 
                                transaction.getAmount());
                        userBalances.put(entry.getKey(), reverseTransaction);
                    }
                }
            }
        }
        
        return userBalances;
    }

    public List<Expense> getUserPassbook(String userId) {
        List<Expense> userExpenses = new ArrayList<>();
        for (Expense expense : expenseHistory) {
            if (expense.getPaidBy().getUserId().equals(userId) || 
                expense.getParticipants().stream()
                    .anyMatch(u -> u.getUserId().equals(userId))) {
                userExpenses.add(expense);
            }
        }
        return userExpenses;
    }

    public void setSimplifyExpenses(boolean simplifyExpenses) {
        this.simplifyExpenses = simplifyExpenses;
        if (simplifyExpenses) {
            simplifyBalances();
        }
    }

    private void simplifyBalances() {
        Map<String, Map<String, Double>> netBalances = new HashMap<>();
        
        // Calculate net balances
        for (Map.Entry<String, Map<String, Transaction>> userEntry : balances.entrySet()) {
            String userId = userEntry.getKey();
            netBalances.putIfAbsent(userId, new HashMap<>());
            
            for (Transaction transaction : userEntry.getValue().values()) {
                String toUserId = transaction.getToUser().getUserId();
                netBalances.get(userId).put(toUserId, 
                        netBalances.get(userId).getOrDefault(toUserId, 0.0) + transaction.getAmount());
            }
        }
        
        // Simplify: if A owes B and B owes C, simplify to A owes C (if applicable)
        // This is a simplified version - full simplification would require graph algorithms
        Map<String, Map<String, Transaction>> simplified = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Double>> userEntry : netBalances.entrySet()) {
            String userId = userEntry.getKey();
            simplified.putIfAbsent(userId, new HashMap<>());
            
            for (Map.Entry<String, Double> balanceEntry : userEntry.getValue().entrySet()) {
                String toUserId = balanceEntry.getKey();
                double amount = balanceEntry.getValue();
                
                if (Math.abs(amount) > 0.01) {
                    if (amount > 0) {
                        // User owes to someone
                        User fromUser = userService.getUser(userId);
                        User toUser = userService.getUser(toUserId);
                        if (fromUser != null && toUser != null) {
                            Transaction transaction = new Transaction(
                                    fromUser, 
                                    toUser, 
                                    amount);
                            simplified.get(userId).put(toUserId, transaction);
                        }
                    }
                }
            }
        }
        
        // Handle reverse balances (if A owes B, B doesn't need to show A owes B)
        for (Map.Entry<String, Map<String, Transaction>> userEntry : simplified.entrySet()) {
            String userId = userEntry.getKey();
            Map<String, Transaction> userBalances = userEntry.getValue();
            
            for (Map.Entry<String, Transaction> balanceEntry : new ArrayList<>(userBalances.entrySet())) {
                String toUserId = balanceEntry.getKey();
                Transaction transaction = balanceEntry.getValue();
                
                // Check if reverse transaction exists
                if (simplified.containsKey(toUserId) && 
                    simplified.get(toUserId).containsKey(userId)) {
                    Transaction reverseTransaction = simplified.get(toUserId).get(userId);
                    
                    if (transaction.getAmount() > reverseTransaction.getAmount()) {
                        transaction.setAmount(transaction.getAmount() - reverseTransaction.getAmount());
                        simplified.get(toUserId).remove(userId);
                    } else if (reverseTransaction.getAmount() > transaction.getAmount()) {
                        reverseTransaction.setAmount(reverseTransaction.getAmount() - transaction.getAmount());
                        userBalances.remove(toUserId);
                    } else {
                        userBalances.remove(toUserId);
                        simplified.get(toUserId).remove(userId);
                    }
                }
            }
        }
        
        this.balances = simplified;
    }

    private Map<String, Map<String, Transaction>> getSimplifiedBalances() {
        // First, calculate net balances (already done in simplifyBalances)
        // Then apply graph-based simplification to minimize number of transactions
        return simplifyBalancesGraph();
    }

    /**
     * Simplifies balances using graph algorithms to minimize the number of transactions.
     */
    private Map<String, Map<String, Transaction>> simplifyBalancesGraph() {
        // Step 1: Calculate net balances for each pair
        Map<String, Map<String, Double>> netBalances = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Transaction>> userEntry : balances.entrySet()) {
            String userId = userEntry.getKey();
            netBalances.putIfAbsent(userId, new HashMap<>());
            
            for (Transaction transaction : userEntry.getValue().values()) {
                String toUserId = transaction.getToUser().getUserId();
                double amount = transaction.getAmount();
                
                // Net out reverse transactions
                if (netBalances.containsKey(toUserId) && 
                    netBalances.get(toUserId).containsKey(userId)) {
                    double reverseAmount = netBalances.get(toUserId).get(userId);
                    if (amount >= reverseAmount) {
                        netBalances.get(toUserId).remove(userId);
                        if (amount > reverseAmount) {
                            netBalances.get(userId).put(toUserId, amount - reverseAmount);
                        }
                    } else {
                        netBalances.get(toUserId).put(userId, reverseAmount - amount);
                        netBalances.get(userId).remove(toUserId);
                    }
                } else {
                    netBalances.get(userId).put(toUserId, 
                            netBalances.get(userId).getOrDefault(toUserId, 0.0) + amount);
                }
            }
        }
        
        // Step 2: Apply transitive simplification (A->B->C becomes A->C if beneficial)
        // This is a simplified version - for full optimization, we'd use minimum cost flow
        Map<String, Map<String, Double>> simplified = new HashMap<>();
        
        // Copy net balances
        for (Map.Entry<String, Map<String, Double>> entry : netBalances.entrySet()) {
            simplified.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        
        // Try to simplify chains: if A owes B and B owes C, see if we can simplify
        boolean changed = true;
        while (changed) {
            changed = false;
            List<String> userIds = new ArrayList<>(simplified.keySet());
            
            for (String a : userIds) {
                Map<String, Double> aBalances = simplified.get(a);
                if (aBalances == null) continue;
                
                for (String b : new ArrayList<>(aBalances.keySet())) {
                    double abAmount = aBalances.get(b);
                    if (abAmount <= 0.01) continue;
                    
                    Map<String, Double> bBalances = simplified.get(b);
                    if (bBalances == null) continue;
                    
                    for (String c : new ArrayList<>(bBalances.keySet())) {
                        if (c.equals(a)) continue; // Avoid cycles
                        
                        double bcAmount = bBalances.get(c);
                        if (bcAmount <= 0.01) continue;
                        
                        // If A owes B and B owes C, we can simplify
                        double minAmount = Math.min(abAmount, bcAmount);
                        
                        // Update: A now owes C directly
                        simplified.putIfAbsent(a, new HashMap<>());
                        simplified.get(a).put(c, 
                                simplified.get(a).getOrDefault(c, 0.0) + minAmount);
                        
                        // Reduce A->B and B->C
                        abAmount -= minAmount;
                        bcAmount -= minAmount;
                        
                        if (abAmount <= 0.01) {
                            simplified.get(a).remove(b);
                        } else {
                            simplified.get(a).put(b, abAmount);
                        }
                        
                        if (bcAmount <= 0.01) {
                            simplified.get(b).remove(c);
                        } else {
                            simplified.get(b).put(c, bcAmount);
                        }
                        
                        changed = true;
                    }
                }
            }
        }
        
        // Step 3: Convert back to Transaction objects
        Map<String, Map<String, Transaction>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> userEntry : simplified.entrySet()) {
            String userId = userEntry.getKey();
            result.putIfAbsent(userId, new HashMap<>());
            
            for (Map.Entry<String, Double> balanceEntry : userEntry.getValue().entrySet()) {
                String toUserId = balanceEntry.getKey();
                double amount = balanceEntry.getValue();
                
                if (Math.abs(amount) > 0.01) {
                    User fromUser = userService.getUser(userId);
                    User toUser = userService.getUser(toUserId);
                    if (fromUser != null && toUser != null) {
                        Transaction transaction = new Transaction(fromUser, toUser, amount);
                        result.get(userId).put(toUserId, transaction);
                    }
                }
            }
        }
        
        return result;
    }
}

