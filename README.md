# Multithreaded_Log_Anomaly_Detector


## Project Overview

A Java-based log monitoring system built to demonstrate concurrent
programming concepts — not just reading and writing files, but handling
a continuous, high-volume stream of data the way a real backend service
would.

The project was built in stages, each one proving a specific concept
before adding complexity on top of it.

## Step 1 — Single-Threaded Baseline (LogGenerator + LogAnalyzer)

`LogGenerator.java` creates a fake log file (`sample.log`) with realistic
INFO and ERROR entries, including deliberate "error bursts" to simulate
incidents like a database outage.

`LogAnalyzer.java` reads that file top to bottom, counts ERROR entries
per minute, and flags any minute with 10 or more errors as a possible
incident.

This step proved the core detection logic works correctly before any
concurrency was introduced — processed 2,000 lines in 47ms and correctly
identified every injected error burst.

## Step 2 — Simulating a Live Log Stream (LiveLogSimulator)

Real servers don't produce a finished file sitting on disk — logs arrive
continuously and unpredictably. `LiveLogSimulator.java` simulates this
with a producer thread that generates log lines forever, at a variable
pace, with sudden bursts of traffic mixed in.

A `BlockingQueue` sits between the producer and whatever is consuming
the logs, acting as a thread-safe buffer.

## Step 3 — Proving the Bottleneck, Then Fixing It

**Single-threaded consumer:** The first version used one thread to pull
lines off the queue and process them one at a time. Under load, the
backlog climbed continuously and never recovered — 0 to over 5,000
queued lines within seconds, demonstrating a hard throughput ceiling
that no amount of optimization could fix within a single thread.

**Multithreaded consumer:** Replaced the single consumer with a pool of
4 worker threads using `ExecutorService`, all pulling from the same
queue concurrently. Under identical load, the backlog now spikes during
bursts but consistently recovers to zero — proving that distributing
work across threads raises the system's overall throughput ceiling.

`AtomicInteger` was used for the shared processed-line counter to keep
it safe across multiple threads updating it simultaneously.

## Concepts Demonstrated

- Producer/consumer pattern with `BlockingQueue`
- Thread creation and management (`Thread`, `ExecutorService`,
  `Executors.newFixedThreadPool`)
- Thread-safe shared state (`AtomicInteger`)
- Benchmarking and comparing single-threaded vs. multithreaded
  throughput under simulated load
- Graceful handling of `InterruptedException` in long-running threads

## What's Next

Wiring the real anomaly-detection logic (per-minute error counting) into
the multithreaded consumer so workers aren't just simulating work but
actually performing thread-safe detection, plus proper shutdown handling
for the thread pool.