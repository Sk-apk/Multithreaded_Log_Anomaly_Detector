package com.tshepo.loganomaly;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * STEP 2: Simulates a LIVE, never-ending log stream (unlike Step 1, where the
 * whole file already existed before we started reading it).
 *
 * There are two things happening "at once" here:
 *   1. A PRODUCER thread - keeps generating new log lines forever, at a
 *      random pace, with occasional sudden bursts (like a real server).
 *   2. A CONSUMER - a single "waiter" (just like Step 1) that picks lines
 *      off the queue one at a time and processes them.
 *
 * Between them sits a BlockingQueue - a thread-safe pile where the producer
 * drops lines and the consumer picks them up. Every 2 seconds we print how
 * many lines are WAITING in that pile. Watch that number - if the consumer
 * can't keep up, it will keep climbing instead of staying near zero.
 */
public class LiveLogSimulator {

    // The "pile of order slips" - shared safely between the producer and consumer.
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args)  {

        // --- Start the producer thread ---
        // This code block (a Runnable) runs independently, "in the background",
        // the moment we call .start(). main() does NOT wait for it to finish -
        // in fact it never finishes, it loops forever.
        Thread producerThread = new Thread(LiveLogSimulator::produceLogsForever);
        producerThread.start();

        // --- Start a monitor thread ---
        // This just prints the queue size every 2 seconds so we can watch
        // the backlog grow (or not) in real time.
        Thread monitorThread = new Thread(LiveLogSimulator::monitorQueueSize);
        monitorThread.setDaemon(true); // don't let this thread stop the program from exiting
        monitorThread.start();

        // --- Consumer runs right here in main() - is multi-threaded ---
        consumeLogsForever();
    }

    /**
     * Keeps generating fake log lines forever and drops them on the queue.
     * Most of the time it pauses briefly between lines (normal traffic).
     * Occasionally it fires a rapid burst of 100 lines with almost no pause
     * (simulating a spike - e.g. a flood of requests or a cascading error).
     */
    private static void produceLogsForever() {
        Random random = new Random();
        int lineCounter = 0;

        while (true) {
            lineCounter++;
            boolean isBurst = lineCounter % 300 == 0; // every 500 lines, fire a burst

            if (isBurst) {
                for (int i = 0; i < 400; i++) {
                    logQueue.add(buildLine(random));
                }
            } else {
                logQueue.add(buildLine(random));
                sleepQuietly(random.nextInt(20)); // 0-50ms pause between normal lines
            }
        }
    }

    private static String buildLine(Random random) {
        String time = LocalDateTime.now().format(FORMAT);
        boolean isError = random.nextInt(10) == 0; // ~10% of lines are errors
        String level = isError ? "ERROR" : "INFO";
        String message = isError ? "Database connection timeout" : "Request served";
        return time + " " + level + " " + message;
    }

    /**
     * The "single waiter" - takes one line off the queue at a time and
     * processes it. queue.take() will simply WAIT if the queue is empty,
     * which is fine. The problem shows up when lines pile up FASTER than
     * this loop can take + process them.
     */
    private static void consumeLogsForever() {
      int numberOfwokers = 4;
      ExecutorService workerPool = Executors.newFixedThreadPool(numberOfwokers);
      AtomicInteger processedCount = new AtomicInteger(0);
      
      for (int i = 0; i<numberOfwokers; i++){
      workerPool.submit(() -> {
      while(true) {
          try {
          String line = logQueue.take();
          sleepQuietly(15);
          
          int count = processedCount.incrementAndGet();
          if (count % 200 == 0){
          System.out.println("Consumer has processed " + count + " lines so far...");
          }
          
          }catch (InterruptedException e){
          Thread.currentThread().interrupt();
          return;
          }
      
      
      }
      
      });
      }
        
    }

    /**
     * Every 2 seconds, print how many lines are currently sitting in the
     * queue, waiting to be processed. This is the number to watch.
     */
    private static void monitorQueueSize() {
        while (true) {
            System.out.println(">> Queue backlog: " + logQueue.size() + " lines waiting");
            sleepQuietly(2000);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
