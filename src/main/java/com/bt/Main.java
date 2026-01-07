package com.bt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double DAILY_RATE = 240.0;

    public static void main(String[] args) {
        String logFilePath = "sessions.log";
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(logFilePath));
            List<LogEntry> entries = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.split("#")[0].trim();
                if (trimmed.isEmpty()) continue;

                String[] parts = trimmed.split("\\s+");
                if (parts.length < 3) continue;

                String username = parts[0];
                String action = parts[1];
                // Combine date and time parts
                String timestampStr = parts[2] + (parts.length > 3 ? " " + parts[3] : "");

                LocalDateTime timestamp = parseTimestamp(timestampStr);
                if (timestamp == null) {
                    System.err.println("Skipping invalid timestamp: " + timestampStr);
                    continue;
                }
                
                entries.add(new LogEntry(username, action, timestamp));
            }
            
            // Sort entries by timestamp to ensure chronological processing
            entries.sort(Comparator.comparing(e -> e.timestamp));

            Map<String, List<Session>> userSessions = new TreeMap<>(); // TreeMap to keep users sorted

            for (LogEntry entry : entries) {
                userSessions.putIfAbsent(entry.username, new ArrayList<>());
                List<Session> sessions = userSessions.get(entry.username);

                if ("login".equalsIgnoreCase(entry.action)) {
                    sessions.add(new Session(entry.timestamp));
                } else if ("logout".equalsIgnoreCase(entry.action)) {
                    boolean matched = false;
                    // Find the last open session to close
                    for (int i = sessions.size() - 1; i >= 0; i--) {
                        Session s = sessions.get(i);
                        if (s.logoutTime == null) {
                            s.logoutTime = entry.timestamp;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        System.err.println("Warning: Logout found without matching login for user " + entry.username + " at " + entry.timestamp);
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
                    // Or should it be considered active until now? 
                    // The requirement implies billing for days logged in.
                    // If logout is null, we can assume it's still active or just bill for the start day.
                    // Previous logic used start day if logout is null.
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

    private static LocalDateTime parseTimestamp(String timestampStr) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(timestampStr, formatter);
            } catch (Exception e) {
                // Try next format
            }
        }
        return null;
    }

    static class LogEntry {
        String username;
        String action;
        LocalDateTime timestamp;

        LogEntry(String username, String action, LocalDateTime timestamp) {
            this.username = username;
            this.action = action;
            this.timestamp = timestamp;
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
