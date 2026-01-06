package com.bt;
import com.bt.exception.InvalidLogEntryException;
import com.bt.model.UserTotal;
import com.bt.service.BillingService;
import com.bt.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
public class Main {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        String logFilePath = "sessions.log";
        BillingService billingService = new BillingService();
        try {
            LogFileReader.processLogFile(logFilePath, new LogEntryProcessor() {

                @Override
                public void process(String username, LocalDateTime timestamp) {

                }

                @Override
                public void processLogin(String username, LocalDateTime timestamp) {
                    billingService.recordLogin(username, timestamp);
                    System.out.printf("Processed: %s login at %s%n", username, timestamp);
                }

                @Override
                public void processLogout(String username, LocalDateTime timestamp) {
                    billingService.recordLogout(username, timestamp);
                    System.out.printf("Processed: %s logout at %s%n", username, timestamp);
                }
            });

            // Generate billing report
            generateBillingReport(billingService, logFilePath);

        } catch (IOException e) {
            System.err.println("Error processing log file: " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidLogEntryException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateBillingReport(BillingService billingService, String logFilePath) throws IOException {
        // Read all lines to find all users and dates
        Set<String> allUsers = new TreeSet<>();
        Set<LocalDate> allDates = new TreeSet<>();

        for (String line : Files.readAllLines(Paths.get(logFilePath))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+", 3);
            if (parts.length >= 3) {
                allUsers.add(parts[0]);
                try {
                    LocalDateTime timestamp = LocalDateTime.parse(parts[2],
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    allDates.add(timestamp.toLocalDate());
                } catch (Exception e) {
                    // Skip invalid timestamps
                }
            }
        }


        System.out.println("Itemized Billing Report");
        ArrayList<UserTotal> usertotal= new ArrayList<>();

        for (String user : allUsers) {
            System.out.println("\nUser: " + user);
           System.out.println("-".repeat(50));

            Map<LocalDate, Double> summary = billingService.getBillingSummary(user);
            if (summary.isEmpty()) {
               System.out.println("  No billing information available");
                continue;
            }

            double total = 0.0;
            for (Map.Entry<LocalDate, Double> entry : summary.entrySet()) {
                LocalDate date = entry.getKey();
                double amount = entry.getValue();
                total += amount;
                System.out.printf("  %s: %.2f%n", date.format(DATE_FORMAT), amount);
            }

           System.out.println("  " + "-".repeat(30));
           System.out.printf("  %-12s %.2f%n", "TOTAL:", total);
            UserTotal usert= new UserTotal(user,total);
            usertotal.add(usert);

        }
        System.out.println("  " + "-".repeat(100));

        System.out.println("Summary bills of all users");
        for(UserTotal ut:usertotal){
            System.out.println("Bill for "+ut.getUsername()+" is "+ut.getTotal());
        }
    }
}