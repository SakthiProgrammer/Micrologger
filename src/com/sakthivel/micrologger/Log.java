package com.sakthivel.micrologger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static volatile Log instance;
    private static volatile boolean initialized = false;
    private String uId;
    private Object reference;
    private static String fileName;
    private static FileWriter fileWriter;
    private static LocalDate currentDate;
    private static LogConfig config = new LogConfig(); // default config

    private Log() {
    }

    private static String getFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        String base = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String micro = String.format("%06d", now.getNano() / 1000);
        return base + ":" + micro;
    }

    // === INIT ===
    public static synchronized void init(LogConfig customConfig) {
        if (initialized)
            return;

        config = customConfig != null ? customConfig : new LogConfig();
        initFile();
        initialized = true;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (Exception e) {
                System.err.println("Shutdown flush failed: " + e.getMessage());
            }
        }));
    }

    public static synchronized void init() {
        init(null);
    }

    private static void initFile() {
        try {
            LocalDateTime now = LocalDateTime.now();
            String nanoPart = String.format("%09d", now.getNano());
            String timestamp = now.format(FILE_DATE_FORMAT) + "-" + nanoPart;

            fileName = config.getLogDirectory() + "/" + timestamp + ".txt";

            File file = new File(fileName);
            file.getParentFile().mkdirs();
            file.createNewFile();

            fileWriter = new FileWriter(file, true);
            currentDate = LocalDate.now();

            fileWriter.write("Log file created at " + timestamp + "\n");
            fileWriter.flush();

            if (config.isPrintToConsole()) {
                System.out.println("Log initialized: " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("Log init failed:");
            e.printStackTrace();
        }
    }

    // === SINGLETON ===
    public static Log getInstance() {
        if (instance == null) {
            synchronized (Log.class) {
                if (instance == null) {
                    init();
                    instance = new Log();
                }
            }
        }
        return instance;
    }

    // === PUBLIC LOGGING METHODS ===
    public void error(String... msg) {
        log("ERROR", msg);
    }

    public void setUid(String uid) {
        this.uId = uid;
    }

    public void setReference(Object ref) {
        this.reference = ref;
    }

    public void removeReference() {
        this.reference = null;
    }

    public void info(String... msg) {
        log("INFO", msg);
    }

    public void debug(String... msg) {
        log("DEBUG", msg);
    }

    public void statement(String... msg) {
        log("STATEMENT", msg);
    }

    public void details(String... msg) {
        log("DETAILS", msg);
    }

    // === INTERNAL LOGIC ===
    private synchronized void log(String level, String... msg) {
        if (!initialized || fileWriter == null) {
            System.err.println("Logger not initialized.");
            return;
        }

        try {
            rotateIfNeeded();

            String timeStamp = getFormattedTime();
            String message = String.join(" ", msg);
            String location = getCallerLocation();

            // Add uid and reference only if not null
            StringBuilder lineBuilder = new StringBuilder();
            lineBuilder.append(timeStamp)
                    .append(" @@ [ ").append(level).append(" ] @@");

            if (uId != null) {
                lineBuilder.append(" ( ").append(uId).append(" ) @@");
            }

            if (reference != null) {
                lineBuilder.append(" ( ").append(reference).append(" ) @@");
            }

            lineBuilder.append(" ").append(location).append(" @@ ").append(message);

            String rawLine = lineBuilder.toString();

            // Write to file
            fileWriter.write(rawLine + "\n");
            fileWriter.flush();

            // Print to console
            if (config.isPrintToConsole()) {
                String line = config.isColorEnabled() ? formatConsoleLine(rawLine) : "| " + rawLine;
                System.out.println(line);
            }

        } catch (Exception e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }

    private static String getCallerLocation() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stack) {
            if (!el.getClassName().equals(Log.class.getName()) &&
                    !el.getClassName().equals(Thread.class.getName())) {
                return "/" + el.getClassName().replace('.', '/') + ".java:" +
                        el.getLineNumber() + " >> " + el.getMethodName() + "()";
            }
        }
        return "Unknown Source";
    }

    private static void rotateIfNeeded() throws IOException {
        if (!config.isRotateDaily())
            return;

        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            if (fileWriter != null)
                fileWriter.close();
            initFile();
        }
    }

    private static String formatConsoleLine(String rawLine) {
        try {
            int timeEnd = rawLine.indexOf(" @@ ");
            String timestamp = rawLine.substring(0, timeEnd);

            int levelStart = rawLine.indexOf("[", timeEnd);
            int levelEnd = rawLine.indexOf("]", levelStart);
            String level = rawLine.substring(levelStart, levelEnd + 1).trim();

            String levelColor = colorizeLevel(level);
            String grayColor = "\u001B[90m";
            String reset = "\u001B[0m";

            // Parse uid and ref if present
            int afterLevel = rawLine.indexOf("] @@", levelEnd) + 4;
            String remaining = rawLine.substring(afterLevel);

            String uid = null, ref = null, location, message;

            if (remaining.startsWith(" (")) {
                int uidStart = remaining.indexOf(" (");
                int uidEnd = remaining.indexOf(") @@", uidStart);
                uid = remaining.substring(uidStart + 2, uidEnd);

                int refStart = remaining.indexOf(" (", uidEnd);
                int refEnd = remaining.indexOf(") @@", refStart);
                if (refStart != -1 && refEnd != -1) {
                    ref = remaining.substring(refStart + 2, refEnd);
                    location = remaining.substring(refEnd + 5, remaining.indexOf("@@", refEnd + 5)).trim();
                    message = remaining.substring(remaining.lastIndexOf("@@") + 3).trim();
                } else {
                    location = remaining.substring(uidEnd + 5, remaining.indexOf("@@", uidEnd + 5)).trim();
                    message = remaining.substring(remaining.lastIndexOf("@@") + 3).trim();
                }
            } else {
                location = remaining.substring(0, remaining.indexOf("@@")).trim();
                message = remaining.substring(remaining.lastIndexOf("@@") + 3).trim();
            }

            // Construct colorized output
            StringBuilder sb = new StringBuilder();
            sb.append(levelColor).append("| ").append(grayColor).append(timestamp).append(reset);
            sb.append(" @@ ").append(levelColor).append(level).append(reset);

            if (uid != null)
                sb.append(" @@ ").append("\u001B[96m( ").append(uid).append(" )\u001B[0m");

            if (ref != null)
                sb.append(" @@ ").append("\u001B[93m( ").append(ref).append(" )\u001B[0m");

            sb.append(" @@ ").append(grayColor).append(location).append(reset);
            sb.append(" @@ ").append(message);

            return sb.toString();

        } catch (Exception e) {
            return rawLine;
        }
    }

    private static String colorizeLevel(String level) {
        switch (level) {
            case "[ ERROR ]":
                return "\u001B[31m"; // Red
            case "[ INFO ]":
                return "\u001B[34m"; // Blue
            case "[ DEBUG ]":
                return "\u001B[36m"; // Cyan
            case "[ DETAILS ]":
                return "\u001B[32m"; // Green
            case "[ STATEMENT ]":
                return "\u001B[35m"; // Magenta
            default:
                return "\u001B[0m"; // Reset
        }

    }

    public static String getFileName() {
        return fileName;
    }
}
