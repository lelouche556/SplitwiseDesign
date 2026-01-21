package strategy;

import model.Expense;
import model.User;

import java.util.Map;

/**
 * Strategy interface for different expense split types.
 */
public interface SplitStrategy {
    /**
     * Calculates how much each participant owes for the expense.
     *
     * @param expense The expense to split
     * @return Map of user to amount owed
     * @throws IllegalArgumentException if the split values are invalid
     */
    Map<User, Double> calculateSplit(Expense expense) throws IllegalArgumentException;
}

