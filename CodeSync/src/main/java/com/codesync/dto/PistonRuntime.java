package com.codesync.dto;

import java.util.List;

/** Maps one entry from Piston's GET /runtimes response. */
public record PistonRuntime(String language, String version, List<String> aliases) {
}
