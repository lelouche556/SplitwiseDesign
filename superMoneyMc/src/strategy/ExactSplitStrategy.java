package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for splitting expenses with exact amounts specified for each participant.
 */
public class ExactSplitStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> calculateSplit(Expense expense) throws IllegalArgumentException {
        List<User> participants = expense.getParticipants();
        List<Double> splitValues = expense.getSplitValues();
        double totalAmount = expense.getAmount();

        if (participants.size() != splitValues.size()) {
            throw new IllegalArgumentException("Number of participants must match number of split values");
        }

        // Validate that sum of exact amounts equals total amount
        double sum = 0.0;
        for (Double value : splitValues) {
            sum += value;
        }

        if (Math.abs(sum - totalAmount) > 0.01) { // Allow small floating point differences
            throw new IllegalArgumentException(
                    String.format("Sum of exact amounts (%.2f) does not equal total amount (%.2f)", 
                            sum, totalAmount));
        }

        Map<User, Double> splitMap = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            double amount = roundToTwoDecimals(splitValues.get(i));
            splitMap.put(participants.get(i), amount);
        }

        return splitMap;
    }

    private double roundToTwoDecimals(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

