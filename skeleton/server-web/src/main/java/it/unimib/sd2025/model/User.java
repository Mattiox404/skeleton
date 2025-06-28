package it.unimib.sd2025.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modello per rappresentare un utente del sistema Carta Cultura Giovani.
 */
public class User {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String fiscalCode;
    private double totalBudget;
    private double availableBudget;
    private double usedBudget;
    private double consumedBudget;
    private LocalDateTime registrationDate;

    // Costruttori
    public User() {
        this.totalBudget = 500.0; // Budget iniziale di 500€
        this.availableBudget = 500.0;
        this.usedBudget = 0.0;
        this.consumedBudget = 0.0;
        this.registrationDate = LocalDateTime.now();
    }

    public User(String name, String surname, String email, String fiscalCode) {
        this();
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.fiscalCode = fiscalCode;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFiscalCode() { return fiscalCode; }
    public void setFiscalCode(String fiscalCode) { this.fiscalCode = fiscalCode; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getAvailableBudget() { return availableBudget; }
    public void setAvailableBudget(double availableBudget) { this.availableBudget = availableBudget; }

    public double getUsedBudget() { return usedBudget; }
    public void setUsedBudget(double usedBudget) { this.usedBudget = usedBudget; }

    public double getConsumedBudget() { return consumedBudget; }
    public void setConsumedBudget(double consumedBudget) { this.consumedBudget = consumedBudget; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    /**
     * Aggiorna i budget quando un buono viene creato.
     */
    public boolean reserveBudget(double amount) {
        if (availableBudget >= amount) {
            availableBudget -= amount;
            usedBudget += amount;
            return true;
        }
        return false;
    }

    /**
     * Aggiorna i budget quando un buono viene consumato.
     */
    public void consumeBudget(double amount) {
        usedBudget -= amount;
        consumedBudget += amount;
    }

    /**
     * Aggiorna i budget quando un buono viene cancellato.
     */
    public void releaseBudget(double amount) {
        usedBudget -= amount;
        availableBudget += amount;
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, name='%s', surname='%s', fiscalCode='%s', available=%.2f}", 
                           id, name, surname, fiscalCode, availableBudget);
    }
}

/**
 * Rappresenta lo stato riassuntivo dei budget di un utente.
 */
class UserBudgetSummary {
    private double available;
    private double used;
    private double consumed;
    private double total;

    public UserBudgetSummary(double available, double used, double consumed, double total) {
        this.available = available;
        this.used = used;
        this.consumed = consumed;
        this.total = total;
    }

    // Getters
    public double getAvailable() { return available; }
    public double getUsed() { return used; }
    public double getConsumed() { return consumed; }
    public double getTotal() { return total; }
}


