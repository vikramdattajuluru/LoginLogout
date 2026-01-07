package com.bt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double DAILY_RATE = 240.0;

    public static void main(String[] args) {
        String logFilePath = "sessions.log";
        Map<String, List<Session>> userSessions = new TreeMap<>(); // TreeMap to keep users sorted

        try {
            List<String> lines = Files.readAllLines(Paths.get(logFilePath));
            for (String line : lines) {
                String trimmed = line.split("#")[0].trim();
                if (trimmed.isEmpty()) continue;

                String[] parts = trimmed.split("\\s+");
                if (parts.length < 3) continue;

                String username = parts[0];
                String action = parts[1];
                // Combine date and time parts
                String timestampStr = parts[2] + (parts.length > 3 ? " " + parts[3] : "");

                LocalDateTime timestamp;
                try {
                    timestamp = LocalDateTime.parse(timestampStr, DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    System.err.println("Skipping invalid timestamp: " + timestampStr);
                    continue;
                }

                userSessions.putIfAbsent(username, new ArrayList<>());
                List<Session> sessions = userSessions.get(username);

                if ("login".equalsIgnoreCase(action)) {
                    sessions.add(new Session(timestamp));
                } else if ("logout".equalsIgnoreCase(action)) {
                    // Find the last open session to close
                    for (int i = sessions.size() - 1; i >= 0; i--) {
                        Session s = sessions.get(i);
                        if (s.logoutTime == null) {
                            s.logoutTime = timestamp;
                            break;
                        }
                    }
                }
            }

            System.out.println("Itemized Billing Report");
            Map<String, Double> userTotals = new LinkedHashMap<>();

            for (Map.Entry<String, List<Session>> entry : userSessions.entrySet()) {
                String user = entry.getKey();
                List<Session> sessions = entry.getValue();

                System.out.println("\nUser: " + user);
                System.out.println("-".repeat(50));

                Map<LocalDate, Double> dailyBills = new TreeMap<>();
                for (Session s : sessions) {
                    LocalDateTime start = s.loginTime;
                    // If session is still open, assume it ends on the start day for billing purposes
                    LocalDateTime end = s.logoutTime != null ? s.logoutTime : start;

                    LocalDate currentDate = start.toLocalDate();
                    LocalDate endDate = end.toLocalDate();

                    while (!currentDate.isAfter(endDate)) {
                        dailyBills.put(currentDate, DAILY_RATE);
                        currentDate = currentDate.plusDays(1);
                    }
                }

                if (dailyBills.isEmpty()) {
                    System.out.println("  No billing information available");
                    continue;
                }

                double total = 0;
                for (Map.Entry<LocalDate, Double> billEntry : dailyBills.entrySet()) {
                    System.out.printf("  %s: %.2f%n", billEntry.getKey().format(DATE_FORMATTER), billEntry.getValue());
                    total += billEntry.getValue();
                }

                System.out.println("  " + "-".repeat(30));
                System.out.printf("  %-12s %.2f%n", "TOTAL:", total);

                userTotals.put(user, total);
            }

            System.out.println("  " + "-".repeat(100));
            System.out.println("Summary bills of all users");
            for (Map.Entry<String, Double> entry : userTotals.entrySet()) {
                System.out.println("Bill for " + entry.getKey() + " is " + entry.getValue());
            }

        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
    }

    static class Session {
        LocalDateTime loginTime;
        LocalDateTime logoutTime;

        Session(LocalDateTime loginTime) {
            this.loginTime = loginTime;
        }
    }
}
