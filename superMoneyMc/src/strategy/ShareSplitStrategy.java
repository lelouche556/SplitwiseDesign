package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for splitting expenses based on share values.
 * Each participant has a share value, and the amount is split proportionally.
 */
public class ShareSplitStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> calculateSplit(Expense expense) throws IllegalArgumentException {
        List<User> participants = expense.getParticipants();
        List<Double> splitValues = expense.getSplitValues();
        double totalAmount = expense.getAmount();

        if (participants.size() != splitValues.size()) {
            throw new IllegalArgumentException("Number of participants must match number of shares");
        }

        // Calculate total shares
        double totalShares = 0.0;
        for (Double share : splitValues) {
            if (share < 0) {
                throw new IllegalArgumentException("Share values cannot be negative");
            }
            totalShares += share;
        }

        if (totalShares == 0) {
            throw new IllegalArgumentException("Total shares cannot be zero");
        }

        Map<User, Double> splitMap = new HashMap<>();
        double totalDistributed = 0.0;
        
        for (int i = 0; i < participants.size() - 1; i++) {
            double share = splitValues.get(i);
            double amount = roundToTwoDecimals((totalAmount * share) / totalShares);
            splitMap.put(participants.get(i), amount);
            totalDistributed += amount;
        }
        
        // Last person gets the remaining amount to ensure exact total
        double lastPersonAmount = roundToTwoDecimals(totalAmount - totalDistributed);
        splitMap.put(participants.get(participants.size() - 1), lastPersonAmount);

        return splitMap;
    }

    private double roundToTwoDecimals(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

