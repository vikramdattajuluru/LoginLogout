package com.bt.util;

import com.bt.exception.InvalidLogEntryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;


public class LogFileReader {
    private static final Logger logger = LoggerFactory.getLogger(LogFileReader.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_LOG_ENTRY_PARTS = 3;
    
    private LogFileReader() {

    }

    public static void processLogFile(String filePath, LogEntryProcessor processor) 
            throws IOException, InvalidLogEntryException {
        Objects.requireNonNull(filePath, "File path cannot be null");
        Objects.requireNonNull(processor, "Log entry processor cannot be null");
        
        if (filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Log file not found: " + filePath);
        }
        
        logger.debug("Starting to process log file: {}", filePath);
        List<String> lines = Files.readAllLines(path);
        
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            try {
                processLine(line, processor);
            } catch (InvalidLogEntryException e) {
                logger.warn("Skipping invalid log entry at line {}: {}", lineNumber, e.getMessage());
                throw new InvalidLogEntryException("Error at line " + lineNumber + ": " + e.getMessage(), e);
            }
        }
        
        logger.info("Successfully processed {} lines from log file: {}", lines.size(), filePath);
    }
    
    private static void processLine(String line, LogEntryProcessor processor) 
            throws InvalidLogEntryException {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
            return; // Skip empty lines and comments
        }
        
        String logEntry = trimmedLine.split("#")[0].trim(); // Keep only the part before any '#'
        
        String[] parts = logEntry.split("\\s+", MIN_LOG_ENTRY_PARTS);
        if (parts.length < MIN_LOG_ENTRY_PARTS) {
            throw new InvalidLogEntryException(
                String.format("Invalid log entry format. Expected at least %d parts, got %d", 
                    MIN_LOG_ENTRY_PARTS, parts.length));
        }
        
        String username = parts[0].trim();
        String action = parts[1].trim().toLowerCase();
        String timestampStr = parts[2].trim();
        
        if (username.isEmpty()) {
            throw new InvalidLogEntryException("Username cannot be empty");
        }
        
        try {
            LocalDateTime timestamp = parseTimestamp(timestampStr);
            
            switch (action) {
                case "login":
                    processor.processLogin(username, timestamp);
                    break;
                case "logout":
                    processor.processLogout(username, timestamp);
                    break;
                default:
                    throw new InvalidLogEntryException(
                        String.format("Unknown action: '%s'. Must be 'login' or 'logout'", action));
            }
        } catch (DateTimeParseException e) {
            throw new InvalidLogEntryException(
                String.format("Invalid timestamp format: '%s'. Expected format: yyyy-MM-dd HH:mm:ss", timestampStr), e);
        } catch (Exception e) {
            throw new InvalidLogEntryException(
                String.format("Error processing log entry: %s", e.getMessage()), e);
        }
    }
    
    private static LocalDateTime parseTimestamp(String timestampStr) throws DateTimeParseException {
        return LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
    }
    

}
