package com.codesync.dto;

/** Maps Piston's POST /execute response. */
public record PistonExecuteResult(String language, String version, RunResult run, RunResult compile) {

    public record RunResult(String stdout, String stderr, String output, int code, String signal) {
    }
}
