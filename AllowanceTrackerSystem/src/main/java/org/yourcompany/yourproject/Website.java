package org.yourcompany.yourproject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Website {
    private static final int PORT = 8080;
    private static final String DATA_FOLDER = "data";

    public static void start() throws IOException {
        Files.createDirectories(Path.of(DATA_FOLDER));

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", exchange -> showHomePage(exchange));
        server.createContext("/action", exchange -> handleAction(exchange));
        server.start();

        System.out.println("Website is running.");
        System.out.println("Open this in your browser: http://localhost:" + PORT + "/");
    }

    private static void showHomePage(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, "{\"ok\":false,\"message\":\"Invalid request method.\"}", "application/json");
            return;
        }

        byte[] html = Files.readAllBytes(Path.of("index.html"));
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, html.length);
        exchange.getResponseBody().write(html);
        exchange.getResponseBody().close();
    }

    private static void handleAction(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "{\"ok\":false,\"message\":\"Invalid request method.\"}", "application/json");
            return;
        }

        Map<String, String> form = readForm(exchange);
        String action = value(form, "action");

        if (action.equals("create")) {
            createProfile(exchange, form);
        } else if (action.equals("login")) {
            login(exchange, form);
        } else if (action.equals("allowance")) {
            addAllowance(exchange, form);
        } else if (action.equals("expense")) {
            addExpense(exchange, form);
        } else if (action.equals("clear")) {
            clearData(exchange, form);
        } else {
            send(exchange, "{\"ok\":false,\"message\":\"Unknown action.\"}", "application/json");
        }
    }

    private static void createProfile(HttpExchange exchange, Map<String, String> form) throws IOException {
        String name = value(form, "name");
        String studentId = value(form, "studentId");

        if (name.isEmpty() || studentId.isEmpty()) {
            send(exchange, "{\"ok\":false,\"message\":\"Enter student name and ID.\"}", "application/json");
            return;
        }

        Path file = studentFile(studentId);
        if (Files.exists(file)) {
            send(exchange, "{\"ok\":false,\"message\":\"Student ID already exists.\"}", "application/json");
            return;
        }

        ExpenseManager manager = new ExpenseManager();
        manager.createProfile(name, studentId);
        FileHandler.saveData(file.toString(), manager);
        sendState(exchange, "Profile created successfully.", manager);
    }

    private static void login(HttpExchange exchange, Map<String, String> form) throws IOException {
        ExpenseManager manager = loadManager(value(form, "studentId"));

        if (manager == null || !manager.hasProfile()) {
            send(exchange, "{\"ok\":false,\"message\":\"Profile not found.\"}", "application/json");
            return;
        }

        sendState(exchange, "Login successful.", manager);
    }

    private static void addAllowance(HttpExchange exchange, Map<String, String> form) throws IOException {
        ExpenseManager manager = loadManager(value(form, "studentId"));
        if (manager == null || !manager.hasProfile()) {
            send(exchange, "{\"ok\":false,\"message\":\"Please log in first.\"}", "application/json");
            return;
        }

        double amount = parseNumber(value(form, "amount"));
        if (!manager.isValidAllowance(amount)) {
            send(exchange, "{\"ok\":false,\"message\":\"Allowance must be greater than 0.\"}", "application/json");
            return;
        }

        manager.addAllowance(amount);
        FileHandler.saveData(studentFile(manager.getStudent().getStudentId()).toString(), manager);
        sendState(exchange, "Allowance added successfully.", manager);
    }

    private static void addExpense(HttpExchange exchange, Map<String, String> form) throws IOException {
        ExpenseManager manager = loadManager(value(form, "studentId"));
        if (manager == null || !manager.hasProfile()) {
            send(exchange, "{\"ok\":false,\"message\":\"Please log in first.\"}", "application/json");
            return;
        }

        String name = value(form, "name");
        String category = value(form, "category");
        double amount = parseNumber(value(form, "amount"));

        if (name.isEmpty()) {
            send(exchange, "{\"ok\":false,\"message\":\"Enter expense name.\"}", "application/json");
            return;
        }

        if (!ExpenseManager.isValidCategory(category)) {
            send(exchange, "{\"ok\":false,\"message\":\"Choose a valid category.\"}", "application/json");
            return;
        }

        if (!manager.hasSpaceForExpense()) {
            send(exchange, "{\"ok\":false,\"message\":\"Expense list is full.\"}", "application/json");
            return;
        }

        if (!manager.isValidExpense(amount)) {
            send(exchange, "{\"ok\":false,\"message\":\"Expense is not valid.\"}", "application/json");
            return;
        }

        manager.addExpense(name, amount, category);
        FileHandler.saveData(studentFile(manager.getStudent().getStudentId()).toString(), manager);
        sendState(exchange, "Expense recorded successfully.", manager);
    }

    private static void clearData(HttpExchange exchange, Map<String, String> form) throws IOException {
        ExpenseManager manager = loadManager(value(form, "studentId"));
        if (manager == null || !manager.hasProfile()) {
            send(exchange, "{\"ok\":false,\"message\":\"Please log in first.\"}", "application/json");
            return;
        }

        ExpenseManager newManager = new ExpenseManager();
        newManager.createProfile(manager.getStudent().getName(), manager.getStudent().getStudentId());
        FileHandler.saveData(studentFile(newManager.getStudent().getStudentId()).toString(), newManager);
        sendState(exchange, "Data cleared successfully.", newManager);
    }

    private static ExpenseManager loadManager(String studentId) throws IOException {
        if (studentId.isEmpty()) {
            return null;
        }

        Path file = studentFile(studentId);
        if (!Files.exists(file)) {
            return null;
        }

        FileHandler.LoadedData data = FileHandler.loadData(file.toString());
        ExpenseManager manager = new ExpenseManager();
        manager.setData(
                data.getStudent(),
                data.getTotalAllowance(),
                data.getExpenseNames(),
                data.getExpenseAmounts(),
                data.getExpenseCategories(),
                data.getExpenseCount()
        );
        return manager;
    }

    private static Path studentFile(String studentId) {
        String cleanId = studentId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return Path.of(DATA_FOLDER, cleanId + ".txt");
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new LinkedHashMap<>();

        if (body.isEmpty()) {
            return form;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            form.put(key, value);
        }

        return form;
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    private static String value(Map<String, String> form, String key) {
        String value = form.get(key);
        return value == null ? "" : value.trim();
    }

    private static double parseNumber(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void sendState(HttpExchange exchange, String message, ExpenseManager manager) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"ok\":true,");
        json.append("\"message\":\"").append(safe(message)).append("\",");
        json.append("\"name\":\"").append(safe(manager.getStudent().getName())).append("\",");
        json.append("\"studentId\":\"").append(safe(manager.getStudent().getStudentId())).append("\",");
        json.append("\"totalAllowance\":").append(format(manager.getTotalAllowance())).append(",");
        json.append("\"totalExpenses\":").append(format(manager.getTotalExpenses())).append(",");
        json.append("\"remainingBalance\":").append(format(manager.getRemainingBalance())).append(",");
        json.append("\"expenses\":[");

        for (int i = 0; i < manager.getExpenseCount(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("{");
            json.append("\"name\":\"").append(safe(manager.getExpenseName(i))).append("\",");
            json.append("\"amount\":").append(format(manager.getExpenseAmount(i))).append(",");
            json.append("\"category\":\"").append(safe(manager.getExpenseCategory(i))).append("\"");
            json.append("}");
        }

        json.append("]}");
        send(exchange, json.toString(), "application/json");
    }

    private static void send(HttpExchange exchange, String text, String contentType) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.getResponseBody().close();
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String safe(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
