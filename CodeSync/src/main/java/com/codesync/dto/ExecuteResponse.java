package com.codesync.dto;

public class ExecuteResponse {

    private String stdout;
    private String stderr;
    private String output;
    private int exitCode;
    private String language;
    private String version;

    public ExecuteResponse() {
    }

    public ExecuteResponse(String stdout, String stderr, String output, int exitCode, String language, String version) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.output = output;
        this.exitCode = exitCode;
        this.language = language;
        this.version = version;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
