package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for splitting expenses equally among all participants.
 * Handles rounding to ensure total equals the expense amount.
 */
public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> calculateSplit(Expense expense) {
        List<User> participants = expense.getParticipants();
        double amount = expense.getAmount();
        int participantCount = participants.size();

        Map<User, Double> splitMap = new HashMap<>();
        double sharePerPerson = amount / participantCount;
        
        // Round to 2 decimal places
        BigDecimal bd = new BigDecimal(sharePerPerson);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double roundedShare = bd.doubleValue();

        // Distribute the amount ensuring total equals the expense amount
        double totalDistributed = 0.0;
        for (int i = 0; i < participantCount - 1; i++) {
            splitMap.put(participants.get(i), roundedShare);
            totalDistributed += roundedShare;
        }
        
        // Last person gets the remaining amount to ensure exact total
        double lastPersonShare = roundToTwoDecimals(amount - totalDistributed);
        splitMap.put(participants.get(participantCount - 1), lastPersonShare);

        return splitMap;
    }

    private double roundToTwoDecimals(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

