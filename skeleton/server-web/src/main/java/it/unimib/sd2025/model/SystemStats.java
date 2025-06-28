package it.unimib.sd2025.model;

public class SystemStats {
    private int totalUsers;
    private double totalBudget;
    private double availableBudget;
    private double usedBudget;
    private double consumedBudget;
    private int totalVouchers;
    private int activeVouchers;
    private int consumedVouchers;

    // Costruttori
    public SystemStats() {}

    public SystemStats(int totalUsers, double totalBudget, double availableBudget, 
                      double usedBudget, double consumedBudget, int totalVouchers, 
                      int activeVouchers, int consumedVouchers) {
        this.totalUsers = totalUsers;
        this.totalBudget = totalBudget;
        this.availableBudget = availableBudget;
        this.usedBudget = usedBudget;
        this.consumedBudget = consumedBudget;
        this.totalVouchers = totalVouchers;
        this.activeVouchers = activeVouchers;
        this.consumedVouchers = consumedVouchers;
    }

    // Getters e Setters
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getAvailableBudget() { return availableBudget; }
    public void setAvailableBudget(double availableBudget) { this.availableBudget = availableBudget; }

    public double getUsedBudget() { return usedBudget; }
    public void setUsedBudget(double usedBudget) { this.usedBudget = usedBudget; }

    public double getConsumedBudget() { return consumedBudget; }
    public void setConsumedBudget(double consumedBudget) { this.consumedBudget = consumedBudget; }

    public int getTotalVouchers() { return totalVouchers; }
    public void setTotalVouchers(int totalVouchers) { this.totalVouchers = totalVouchers; }

    public int getActiveVouchers() { return activeVouchers; }
    public void setActiveVouchers(int activeVouchers) { this.activeVouchers = activeVouchers; }

    public int getConsumedVouchers() { return consumedVouchers; }
    public void setConsumedVouchers(int consumedVouchers) { this.consumedVouchers = consumedVouchers; }
}
