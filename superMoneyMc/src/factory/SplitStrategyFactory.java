package factory;

import model.Expense;
import strategy.*;

/**
 * Factory class for creating SplitStrategy instances based on expense split type.
 */
public class SplitStrategyFactory {
    public static SplitStrategy createStrategy(Expense.SplitType splitType) {
        switch (splitType) {
            case EQUAL:
                return new EqualSplitStrategy();
            case EXACT:
                return new ExactSplitStrategy();
            case PERCENT:
                return new PercentSplitStrategy();
            case SHARE:
                return new ShareSplitStrategy();
            default:
                throw new IllegalArgumentException("Unknown split type: " + splitType);
        }
    }
}

