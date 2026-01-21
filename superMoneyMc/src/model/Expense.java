package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an expense transaction in the expense sharing application.
 *
 * userA - 1000 equal split uB, uC, Ud
 *
 */
public class Expense {
    private String expenseId;
    private User paidBy;
    private double amount;
    private String expenseName;
    private String notes;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private SplitType splitType;
    private List<User> participants;
    private List<Double> splitValues;

    public Expense(String expenseId, User paidBy, double amount, String expenseName,
                   SplitType splitType, List<User> participants, List<Double> splitValues) {
        this.expenseId = expenseId;
        this.paidBy = paidBy;
        this.amount = amount;
        this.expenseName = expenseName;
        this.notes = "";
        this.imageUrls = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.splitType = splitType;
        this.participants = participants;
        this.splitValues = splitValues;
    }

    public String getExpenseId() {
        return expenseId;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public double getAmount() {
        return amount;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void addImageUrl(String imageUrl) {
        this.imageUrls.add(imageUrl);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public List<Double> getSplitValues() {
        return splitValues;
    }

    public enum SplitType {
        EQUAL, EXACT, PERCENT, SHARE
    }
}

