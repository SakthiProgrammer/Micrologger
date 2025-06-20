package com.sakthivel.micrologger;

public class LogConfig {
    private boolean printToConsole = true;
    private boolean rotateDaily = true;
    private String logDirectory = "log";
    private boolean colorEnabled = true;

    public boolean isPrintToConsole() {
        return printToConsole;
    }

    public LogConfig setPrintToConsole(boolean printToConsole) {
        this.printToConsole = printToConsole;
        return this;
    }

    public boolean isRotateDaily() {
        return rotateDaily;
    }

    public LogConfig setRotateDaily(boolean rotateDaily) {
        this.rotateDaily = rotateDaily;
        return this;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public LogConfig setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
        return this;
    }

    public boolean isColorEnabled() {
        return colorEnabled;
    }

    public LogConfig setColorEnabled(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
        return this;
    }
}
