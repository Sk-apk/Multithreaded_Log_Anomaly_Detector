package com.tshepo.loganomaly;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * STEP 1: The single-threaded version ("one waiter").
 *
 * This reads a log file from top to bottom, one line at a time, and counts
 * how many ERROR lines happened in each minute. It does everything itself:
 * read -> parse -> count -> move to next line.
 *
 * This works fine for a file sitting on disk. The problem shows up when logs
 * arrive continuously and fast (like a real server) - this loop can't do
 * anything else while it's busy processing one line. That's the pain point
 * that justifies adding threads later.
 */
public class LogAnalyzer {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        String logFilePath = "sample.log";

        // Key = "yyyy-MM-dd HH:mm" (the minute), Value = how many ERROR lines in that minute
        Map<String, Integer> errorCountsByMinute = new LinkedHashMap<>();

        long startTime = System.currentTimeMillis();
        int totalLines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                processLine(line, errorCountsByMinute);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;

        System.out.println("Processed " + totalLines + " lines in " + durationMs + "ms\n");
        System.out.println("Error counts per minute:");
        errorCountsByMinute.forEach((minute, count) ->
                System.out.println("  " + minute + " -> " + count + " errors"));

        // A very simple threshold check: flag any minute with 10+ errors.
        System.out.println("\nAnomalies detected:");
        boolean foundAny = false;
        for (Map.Entry<String, Integer> entry : errorCountsByMinute.entrySet()) {
            if (entry.getValue() >= 10) {
                System.out.println("  !! " + entry.getKey() + " had " + entry.getValue()
                        + " errors - possible incident");
                foundAny = true;
            }
        }
        if (!foundAny) {
            System.out.println("  None.");
        }
    }

    /**
     * Looks at a single log line. If it's an ERROR line, figures out which
     * minute it belongs to and increments that minute's counter.
     */
    private static void processLine(String line, Map<String, Integer> errorCountsByMinute) {
        if (!line.contains("ERROR")) {
            return; // not an error, nothing to count
        }

        // Log lines look like: "2026-07-16 10:15:02 ERROR Database connection timeout"
        // We only need the first 16 characters to get "yyyy-MM-dd HH:mm" (the minute).
        if (line.length() < 16) {
            return; // malformed line, skip it
        }

        String minuteKey = line.substring(0, 16);
        errorCountsByMinute.merge(minuteKey, 1, Integer::sum);
    }
}
