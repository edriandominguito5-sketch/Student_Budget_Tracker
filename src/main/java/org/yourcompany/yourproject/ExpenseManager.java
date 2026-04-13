package org.yourcompany.yourproject;

public class ExpenseManager {
    public static final String[] CATEGORIES = {"Food", "Fare", "School", "Personal", "Others"};
    public static final int MAX_EXPENSES = 50;

    private Student student = new Student();
    private double totalAllowance;
    private final String[] expenseNames = new String[MAX_EXPENSES];
    private final double[] expenseAmounts = new double[MAX_EXPENSES];
    private final String[] expenseCategories = new String[MAX_EXPENSES];
    private int expenseCount;

    public ExpenseManager() {
        totalAllowance = 0.0;
        expenseCount = 0;
    }

    public void createProfile(String name, String studentId) {
        student = new Student(name, studentId);
    }

    public boolean hasProfile() {
        return !student.getName().isEmpty() && !student.getStudentId().isEmpty();
    }

    public boolean isValidAllowance(double amount) {
        return amount > 0;
    }

    public void addAllowance(double amount) {
        totalAllowance += amount;
    }

    public boolean isValidExpense(double amount) {
        return amount > 0 && amount <= getRemainingBalance();
    }

    public boolean hasSpaceForExpense() {
        return expenseCount < MAX_EXPENSES;
    }

    public void addExpense(String name, double amount, String category) {
        expenseNames[expenseCount] = name.trim();
        expenseAmounts[expenseCount] = amount;
        expenseCategories[expenseCount] = category.trim();
        expenseCount++;
    }

    public static boolean isValidCategory(String category) {
        for (String validCategory : CATEGORIES) {
            if (validCategory.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    public Student getStudent() {
        return student;
    }

    public double getTotalAllowance() {
        return totalAllowance;
    }

    public double getTotalExpenses() {
        double total = 0.0;
        for (int i = 0; i < expenseCount; i++) {
            total += expenseAmounts[i];
        }
        return total;
    }

    public double getRemainingBalance() {
        return totalAllowance - getTotalExpenses();
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public String getExpenseName(int index) {
        if (index < 0 || index >= expenseCount) {
            return "";
        }
        return expenseNames[index];
    }

    public double getExpenseAmount(int index) {
        if (index < 0 || index >= expenseCount) {
            return 0.0;
        }
        return expenseAmounts[index];
    }

    public String getExpenseCategory(int index) {
        if (index < 0 || index >= expenseCount) {
            return "";
        }
        return expenseCategories[index];
    }

    public void setData(Student student, double totalAllowance, String[] names, double[] amounts, String[] categories, int count) {
        this.student = student == null ? new Student() : student;
        this.totalAllowance = Math.max(0.0, totalAllowance);
        clearExpenses();

        if (names == null || amounts == null || categories == null) {
            return;
        }

        int safeCount = Math.max(0, Math.min(count, MAX_EXPENSES));
        safeCount = Math.min(safeCount, Math.min(names.length, Math.min(amounts.length, categories.length)));
        for (int i = 0; i < safeCount; i++) {
            if (names[i] != null && categories[i] != null && !names[i].trim().isEmpty()
                    && isValidCategory(categories[i]) && amounts[i] > 0) {
                addExpense(names[i], amounts[i], categories[i]);
            }
        }
    }

    private void clearExpenses() {
        for (int i = 0; i < MAX_EXPENSES; i++) {
            expenseNames[i] = null;
            expenseAmounts[i] = 0.0;
            expenseCategories[i] = null;
        }
        expenseCount = 0;
    }
}
