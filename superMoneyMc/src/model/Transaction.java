package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a transaction entry showing who owes whom how much.
 */
public class Transaction {
    private User fromUser;
    private User toUser;
    private double amount;

    public Transaction(User fromUser, User toUser, double amount) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = roundToTwoDecimals(amount);
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public double getAmount() {
        return amount;
    }

    public void addAmount(double additionalAmount) {
        this.amount = roundToTwoDecimals(this.amount + additionalAmount);
    }

    public void subtractAmount(double subtractAmount) {
        this.amount = roundToTwoDecimals(this.amount - subtractAmount);
    }

    public void setAmount(double amount) {
        this.amount = roundToTwoDecimals(amount);
    }

    private double roundToTwoDecimals(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(fromUser, that.fromUser) &&
                Objects.equals(toUser, that.toUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromUser, toUser);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "fromUser=" + fromUser.getUserId() +
                ", toUser=" + toUser.getUserId() +
                ", amount=" + amount +
                '}';
    }
}

