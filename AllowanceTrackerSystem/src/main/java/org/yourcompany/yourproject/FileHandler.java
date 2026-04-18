package org.yourcompany.yourproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileHandler {
    private static final String STUDENT_NAME = "STUDENT_NAME=";
    private static final String STUDENT_ID = "STUDENT_ID=";
    private static final String TOTAL_ALLOWANCE = "TOTAL_ALLOWANCE=";
    private static final String EXPENSE = "EXPENSE=";

    public static void saveData(String fileName, ExpenseManager manager) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(STUDENT_NAME + manager.getStudent().getName());
            writer.newLine();
            writer.write(STUDENT_ID + manager.getStudent().getStudentId());
            writer.newLine();
            writer.write(TOTAL_ALLOWANCE + manager.getTotalAllowance());
            writer.newLine();

            for (int i = 0; i < manager.getExpenseCount(); i++) {
                writer.write(EXPENSE + manager.getExpenseName(i) + ";" + manager.getExpenseAmount(i) + ";" + manager.getExpenseCategory(i));
                writer.newLine();
            }
        }
    }

    public static LoadedData loadData(String fileName) throws IOException {
        Student student = new Student();
        double totalAllowance = 0.0;
        String[] names = new String[ExpenseManager.MAX_EXPENSES];
        double[] amounts = new double[ExpenseManager.MAX_EXPENSES];
        String[] categories = new String[ExpenseManager.MAX_EXPENSES];
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(STUDENT_NAME)) {
                    student.setName(line.substring(STUDENT_NAME.length()).trim());
                } else if (line.startsWith(STUDENT_ID)) {
                    student.setStudentId(line.substring(STUDENT_ID.length()).trim());
                } else if (line.startsWith(TOTAL_ALLOWANCE)) {
                    try {
                        totalAllowance = Double.parseDouble(line.substring(TOTAL_ALLOWANCE.length()).trim());
                    } catch (NumberFormatException e) {
                        totalAllowance = 0.0;
                    }
                } else if (line.startsWith(EXPENSE) && count < ExpenseManager.MAX_EXPENSES) {
                    String[] parts = line.substring(EXPENSE.length()).split(";");
                    if (parts.length == 3) {
                        String name = parts[0].trim();
                        String category = parts[2].trim();
                        try {
                            double amount = Double.parseDouble(parts[1].trim());
                            if (!name.isEmpty() && amount > 0 && ExpenseManager.isValidCategory(category)) {
                                names[count] = name;
                                amounts[count] = amount;
                                categories[count] = category;
                                count++;
                            }
                        } catch (NumberFormatException e) {
                            // skip invalid expense line
                        }
                    }
                }
            }
        }

        return new LoadedData(student, totalAllowance, names, amounts, categories, count);
    }

    public static class LoadedData {
        private final Student student;
        private final double totalAllowance;
        private final String[] expenseNames;
        private final double[] expenseAmounts;
        private final String[] expenseCategories;
        private final int expenseCount;

        public LoadedData(Student student, double totalAllowance, String[] expenseNames, double[] expenseAmounts,
                          String[] expenseCategories, int expenseCount) {
            this.student = student;
            this.totalAllowance = totalAllowance;
            this.expenseNames = expenseNames;
            this.expenseAmounts = expenseAmounts;
            this.expenseCategories = expenseCategories;
            this.expenseCount = expenseCount;
        }

        public Student getStudent() {
            return student;
        }

        public double getTotalAllowance() {
            return totalAllowance;
        }

        public String[] getExpenseNames() {
            return expenseNames;
        }

        public double[] getExpenseAmounts() {
            return expenseAmounts;
        }

        public String[] getExpenseCategories() {
            return expenseCategories;
        }

        public int getExpenseCount() {
            return expenseCount;
        }
    }
}
