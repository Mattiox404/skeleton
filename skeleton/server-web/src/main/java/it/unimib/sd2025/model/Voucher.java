package it.unimib.sd2025.model;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.json.bind.annotation.JsonbTransient;

/**
 * Modello per rappresentare un buono della Carta Cultura Giovani.
 */
public class Voucher {
    public enum Category {
        CINEMA("cinema"),
        MUSICA("musica"), 
        CONCERTI("concerti"),
        EVENTI_CULTURALI("eventi culturali"),
        LIBRI("libri"),
        MUSEI("musei"),
        STRUMENTI_MUSICALI("strumenti musicali"),
        TEATRO("teatro"),
        DANZA("danza");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        public static Category fromString(String text) {
            for (Category category : Category.values()) {
                if (category.displayName.equalsIgnoreCase(text)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Unknown category: " + text);
        }
    }

    public enum Status {
        ACTIVE("active"),
        CONSUMED("consumed");

        private final String value;

        Status(String value) { this.value = value; }
        
        public String getValue() { return value; }

        public static Status fromString(String text) {
            for (Status status : Status.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status: " + text);
        }
    }

    private int id;
    private int userId;
    private double amount;
    private Category category;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime consumedAt;

    // Costruttori
    public Voucher() {
        this.status = Status.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public Voucher(int userId, double amount, Category category) {
        this();
        this.userId = userId;
        this.amount = amount;
        this.category = category;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @JsonbTransient
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }

    /**
     * Espone al JSON un oggetto categoryInfo
     * con value e displayName
     */
    public Map<String, String> getCategoryInfo() {
        if (category != null) {
            return Map.of(
                "value", category.name(),
                "displayName", category.getDisplayName()
            );
        } else {
            return null;
        }
    }

    /**
     * Segna il buono come consumato.
     */
    public boolean consume() {
        if (status == Status.ACTIVE) {
            status = Status.CONSUMED;
            consumedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Verifica se il buono può essere modificato.
     */
    public boolean isModifiable() {
        return status == Status.ACTIVE;
    }

    @Override
    public String toString() {
        return String.format(
            "Voucher{id=%d, userId=%d, amount=%.2f, category=%s, status=%s}",
            id, userId, amount,
            category != null ? category.getDisplayName() : "null",
            status.getValue()
        );
    }
}
