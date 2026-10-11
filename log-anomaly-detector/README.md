# Log Anomaly Detector — Step 1 (Single-Threaded)

This is the starting point: a plain Java program that reads a log file
top-to-bottom and flags minutes with unusually high error counts.

## How to open this in NetBeans

1. Open NetBeans
2. File -> Open Project -> select the `log-anomaly-detector` folder
   (NetBeans recognizes the `pom.xml` and imports it as a Maven project automatically)
3. Wait for NetBeans to download dependencies (there are none yet, so this is instant)

## How to run it (two programs, run in this order)

**Step A — Generate fake log data:**
- In the Projects panel, expand `Source Packages -> com.tshepo.loganomaly`
- Right-click `LogGenerator.java` -> Run File
- This creates a file called `sample.log` in your project folder with 2000 fake log lines,
  including a few simulated "error bursts"

**Step B — Analyze the log:**
- Right-click `LogAnalyzer.java` -> Run File
- Check the Output panel — you'll see:
  - How many lines were processed and how long it took
  - Error counts broken down by minute
  - Any minute flagged as an anomaly (10+ errors)

## What to look at

Open `sample.log` in NetBeans after generating it — you'll see mostly INFO lines,
with clusters of ERROR lines mixed in. Those clusters are what `LogAnalyzer`
is designed to catch.

## What's next (Step 2)

Right now this reads the whole file in one pass with one thread — simple, but it
would fall behind if logs were arriving live, thousands of lines per second.
Step 2 will introduce a producer/consumer pattern with a thread pool to fix that.
Don't touch that yet — get Step 1 running and understand it first.
