package com.bt.util;

import org.junit.jupiter.api.BeforeEach;
import com.bt.exception.InvalidLogEntryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogFileReaderTest {
    
    private static class TestLogProcessor implements LogEntryProcessor {
        private final List<String> logins = new ArrayList<>();
        private final List<String> logouts = new ArrayList<>();
        
        @Override
        public void process(String username, LocalDateTime timestamp) {
            // This method is required by the interface but not used in tests
            // since we're overriding the specific processLogin/processLogout methods
        }
        
        @Override
        public void processLogin(String username, LocalDateTime timestamp) {
            logins.add(username + " " + timestamp);
        }
        
        @Override
        public void processLogout(String username, LocalDateTime timestamp) {
            logouts.add(username + " " + timestamp);
        }
        
        public int getLoginCount() {
            return logins.size();
        }
        
        public int getLogoutCount() {
            return logouts.size();
        }
        
        public List<String> getLogins() {
            return logins;
        }
        
        public List<String> getLogouts() {
            return logouts;
        }
    }
    
    @TempDir
    Path tempDir;
    private Path testLogFile;
    private TestLogProcessor processor;
    
    @BeforeEach
    void setUp() throws IOException {
        testLogFile = tempDir.resolve("test.log");
        processor = new TestLogProcessor();
    }
    
    @Test
    void testProcessValidLogFile() throws IOException, InvalidLogEntryException {
        // Given
        String content = """
            # Test log file
            user1 login 2024-01-01 09:00:00
            user2 login 2024-01-01 09:05:00
            user1 logout 2024-01-01 17:00:00
            user2 logout 2024-01-01 17:30:00
            """.stripIndent();
        
        Files.writeString(testLogFile, content);
        
        // When
        LogFileReader.processLogFile(testLogFile.toString(), processor);
        
        // Then
        assertEquals(2, processor.getLoginCount(), "Should have 2 login entries");
        assertEquals(2, processor.getLogoutCount(), "Should have 2 logout entries");
        
        // Verify login entries
        assertFalse(processor.getLogins().isEmpty(), "Should have login entries");
        assertTrue(processor.getLogins().get(0).startsWith("user1 2024-01-01T09:00"), 
            "First login entry should be for user1 at 09:00");
        assertTrue(processor.getLogins().get(1).startsWith("user2 2024-01-01T09:05"), 
            "Second login entry should be for user2 at 09:05");
            
        // Verify logout entries
        assertFalse(processor.getLogouts().isEmpty(), "Should have logout entries");
        assertTrue(processor.getLogouts().get(0).startsWith("user1 2024-01-01T17:00"), 
            "First logout entry should be for user1 at 17:00");
        assertTrue(processor.getLogouts().get(1).startsWith("user2 2024-01-01T17:30"), 
            "Second logout entry should be for user2 at 17:30");
    }
    
    @Test
    void testProcessLogFileWithMissingLogout() throws IOException, InvalidLogEntryException {
        // Given
        String content = """
            user1 login 2024-01-01 09:00:00
            user2 login 2024-01-01 09:05:00
            user1 logout 2024-01-01 17:00:00
            # user2 forgot to log out
            """.stripIndent();
        
        Files.writeString(testLogFile, content);
        
        // When
        LogFileReader.processLogFile(testLogFile.toString(), processor);
        
        // Then
        assertEquals(2, processor.getLoginCount(), "Should have 2 login entries");
        assertEquals(1, processor.getLogoutCount(), "Should have 1 logout entry");
        assertTrue(processor.getLogins().stream().anyMatch(login -> login.startsWith("user1 2024-01-01T09:00")), 
            "Should have login for user1 at 09:00");
        assertTrue(processor.getLogins().stream().anyMatch(login -> login.startsWith("user2 2024-01-01T09:05")), 
            "Should have login for user2 at 09:05");
        assertTrue(processor.getLogouts().stream().anyMatch(logout -> logout.startsWith("user1 2024-01-01T17:00")), 
            "Should have logout for user1 at 17:00");
    }
    
    @Test
    void testProcessLogFileWithInvalidLines() throws IOException {
        // Given
        String content = """
            # Invalid line formats below
            user1 login
            login 2024-01-01 09:00:00
            user1 logout 2024-01-01 17:00:00
            user1 login 2024-01-01 09:00:00 extra
            """.stripIndent();
        
        Files.writeString(testLogFile, content);
        
        // When/Then - Should throw InvalidLogEntryException for invalid lines
        assertThrows(InvalidLogEntryException.class, () ->
            LogFileReader.processLogFile(testLogFile.toString(), processor));
        
        // Verify no logins or logouts were processed due to the exception
        assertEquals(0, processor.getLoginCount());
        assertEquals(0, processor.getLogoutCount());
    }
    
    @Test
    void testProcessLogFileWithInvalidTimestamp() {
        // Given
        String content = """
            user1 login 2024-13-01 25:00:00  # Invalid date/time
            user1 logout 2024-01-01 17:00:00
            """.stripIndent();

        try {
            Files.writeString(testLogFile, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // When/Then - Should throw InvalidLogEntryException for invalid timestamp
        assertThrows(InvalidLogEntryException.class, () -> 
            LogFileReader.processLogFile(testLogFile.toString(), processor));
            
        // Verify no logins or logouts were processed due to the exception
        assertEquals(0, processor.getLoginCount(), "No logins should be processed when an exception occurs");
        assertEquals(0, processor.getLogoutCount(), "No logouts should be processed when an exception occurs");
    }
    
    @Test
    void testProcessEmptyFile() throws IOException, InvalidLogEntryException {
        // Given
        Files.writeString(testLogFile, "");
        
        // When
        LogFileReader.processLogFile(testLogFile.toString(), processor);
        
        // Then - Empty file should be processed without errors
        assertEquals(0, processor.getLoginCount(), "No logins should be processed from empty file");
        assertEquals(0, processor.getLogoutCount(), "No logouts should be processed from empty file");
    }
    
    @Test
    void testProcessNonExistentFile() {
        // When/Then
        assertThrows(IOException.class, () -> 
            LogFileReader.processLogFile("nonexistent.log", processor)
        );
    }
    
    @Test
    void testProcessLogFileWithCommentsAndEmptyLines() throws IOException, InvalidLogEntryException {
        // Given
        String content = """
            # This is a comment
            
            user1 login 2024-01-01 09:00:00
            
            # Another comment
            user1 logout 2024-01-01 17:00:00
            
            """.stripIndent();
        
        Files.writeString(testLogFile, content);
        
        // When
        LogFileReader.processLogFile(testLogFile.toString(), processor);
        
        // Then - Comments and empty lines should be ignored, valid entries processed
        assertEquals(1, processor.getLoginCount(), "Should process one login entry");
        assertEquals(1, processor.getLogoutCount(), "Should process one logout entry");
        assertTrue(processor.getLogins().stream().anyMatch(login -> login.startsWith("user1 2024-01-01T09:00")), 
            "Should have login for user1 at 09:00");
        assertTrue(processor.getLogouts().stream().anyMatch(logout -> logout.startsWith("user1 2024-01-01T17:00")), 
            "Should have logout for user1 at 17:00");
    }
}
