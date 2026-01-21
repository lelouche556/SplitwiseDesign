package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for splitting expenses based on percentage shares.
 */
public class PercentSplitStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> calculateSplit(Expense expense) throws IllegalArgumentException {
        List<User> participants = expense.getParticipants();
        List<Double> splitValues = expense.getSplitValues();
        double totalAmount = expense.getAmount();

        if (participants.size() != splitValues.size()) {
            throw new IllegalArgumentException("Number of participants must match number of percentages");
        }

        // Validate that percentages sum to 100
        double sumPercent = 0.0;
        for (Double percent : splitValues) {
            sumPercent += percent;
        }

        if (Math.abs(sumPercent - 100.0) > 0.01) { // Allow small floating point differences
            throw new IllegalArgumentException(
                    String.format("Sum of percentages (%.2f) does not equal 100", sumPercent));
        }

        Map<User, Double> splitMap = new HashMap<>();
        double totalDistributed = 0.0;
        
        for (int i = 0; i < participants.size() - 1; i++) {
            double percent = splitValues.get(i);
            double amount = roundToTwoDecimals((totalAmount * percent) / 100.0);
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

