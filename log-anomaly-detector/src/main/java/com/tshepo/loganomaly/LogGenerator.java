package com.tshepo.loganomaly;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Generates a fake server log file for testing the anomaly detector.
 *
 * Why this exists: We don't have a real production server, so we simulate
 * one. Most of the log lines are normal (INFO). Every so often, we inject
 * a "burst" of ERROR lines to simulate something going wrong - like a
 * database timing out. This gives our detector something real to catch.
 */
public class LogGenerator {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] INFO_MESSAGES = {
        "User login success user=%s",
        "Request served in %dms",
        "File uploaded successfully size=%dKB",
        "Cache refreshed",
        "Health check OK"
    };

    private static final String[] ERROR_MESSAGES = {
        "Database connection timeout",
        "Failed login attempt user=%s",
        "Null pointer exception in PaymentService",
        "503 Service Unavailable",
        "Disk write failed"
    };

    private static final String[] USERS = {"Tshepo", "Lerato", "Sipho", "Naledi", "Kagiso"};

    public static void main(String[] args) throws IOException {
        String outputPath = "sample.log";
        int totalLines = 2000;

        generate(outputPath, totalLines);
        System.out.println("Generated " + totalLines + " log lines into " + outputPath);
    }

    public static void generate(String outputPath, int totalLines) throws IOException {
        Random random = new Random();
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(30);

        try (FileWriter writer = new FileWriter(outputPath)) {
            for (int i = 0; i < totalLines; i++) {
                // Advance time by a small random amount each line, so timestamps
                // look realistic instead of all happening in the same second.
                timestamp = timestamp.plusSeconds(random.nextInt(2));

                // Every ~300 lines, simulate a 15-line ERROR burst (e.g. DB outage).
                boolean inErrorBurst = (i % 300) < 15;

                String line;
                if (inErrorBurst || random.nextInt(20) == 0) {
                    line = buildErrorLine(timestamp, random);
                } else {
                    line = buildInfoLine(timestamp, random);
                }

                writer.write(line + System.lineSeparator());
            }
        }
    }

    private static String buildInfoLine(LocalDateTime timestamp, Random random) {
        String template = INFO_MESSAGES[random.nextInt(INFO_MESSAGES.length)];
        String message = formatMessage(template, random);
        return timestamp.format(FORMAT) + " INFO " + message;
    }

    private static String buildErrorLine(LocalDateTime timestamp, Random random) {
        String template = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
        String message = formatMessage(template, random);
        return timestamp.format(FORMAT) + " ERROR " + message;
    }

    private static String formatMessage(String template, Random random) {
        if (template.contains("%s")) {
            return String.format(template, USERS[random.nextInt(USERS.length)]);
        } else if (template.contains("%d")) {
            return String.format(template, random.nextInt(500));
        }
        return template;
    }
}
