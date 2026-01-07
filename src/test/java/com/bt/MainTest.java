package com.bt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private static final String LOG_FILE_NAME = "sessions.log";

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testMainWithValidLogFile() throws IOException {
        // Given
        String content = """
            alice login 2026-01-01 09:00:00
            alice logout 2026-01-01 17:00:00
            bob login 2026-01-01 22:00:00
            bob logout 2026-01-02 02:00:00
            """;
        Files.writeString(Paths.get(LOG_FILE_NAME), content);

        // When
        Main.main(new String[]{});

        // Then
        String output = outContent.toString();
        assertTrue(output.contains("User: alice"));
        assertTrue(output.contains("2026-01-01: 240.00"));
        assertTrue(output.contains("TOTAL:       240.00"));

        assertTrue(output.contains("User: bob"));
        assertTrue(output.contains("2026-01-01: 240.00"));
        assertTrue(output.contains("2026-01-02: 240.00"));
        assertTrue(output.contains("TOTAL:       480.00"));
        
        assertTrue(output.contains("Bill for alice is 240.0"));
        assertTrue(output.contains("Bill for bob is 480.0"));
    }

    @Test
    void testMainWithMissingLogout() throws IOException {
        // Given
        String content = """
            dave login 2026-01-01 10:00:00
            """;
        Files.writeString(Paths.get(LOG_FILE_NAME), content);

        // When
        Main.main(new String[]{});

        // Then
        String output = outContent.toString();
        assertTrue(output.contains("User: dave"));
        assertTrue(output.contains("2026-01-01: 240.00"));
        assertTrue(output.contains("TOTAL:       240.00"));
    }

    @Test
    void testMainWithInvalidLines() throws IOException {
        // Given
        String content = """
            # This is a comment
            invalid line
            alice login 2026-01-01 09:00:00
            alice logout 2026-01-01 17:00:00
            """;
        Files.writeString(Paths.get(LOG_FILE_NAME), content);

        // When
        Main.main(new String[]{});

        // Then
        String output = outContent.toString();
        assertTrue(output.contains("User: alice"));
        assertTrue(output.contains("2026-01-01: 240.00"));
    }
}
