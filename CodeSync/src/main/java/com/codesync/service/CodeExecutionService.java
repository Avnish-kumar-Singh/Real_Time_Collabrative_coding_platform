package com.codesync.service;

import com.codesync.dto.ExecuteResponse;
import com.codesync.dto.PistonExecuteResult;
import com.codesync.dto.PistonRuntime;
import com.codesync.exception.CodeExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes user-submitted code via Piston (https://github.com/engineer-man/piston),
 * a free, open-source, sandboxed code-execution engine. We deliberately do NOT
 * run submitted code directly on this server — that would be an arbitrary remote
 * code execution hole. Piston runs everything in its own isolated containers.
 *
 * As of Feb 2026 the public emkc.org instance is whitelist-only, so this points
 * at a self-hosted Piston instance by default (see application.properties /
 * codesync.piston.base-url). Run one locally with Docker — see README.
 */
@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);
    private static final Duration RUNTIME_CACHE_TTL = Duration.ofHours(1);

    private final RestClient restClient;
    private final String pistonBaseUrl;

    private volatile Map<String, String> languageToVersion = Map.of();
    private volatile Instant cacheExpiry = Instant.EPOCH;

    public CodeExecutionService(@Value("${codesync.piston.base-url}") String pistonBaseUrl) {
        // JDK's default HTTP client tries an HTTP/2 cleartext (h2c) upgrade first,
        // which self-hosted Piston's simple Node server doesn't understand and
        // rejects with 400. SimpleClientHttpRequestFactory always speaks plain
        // HTTP/1.1, matching what curl/PowerShell do by default.
        this.pistonBaseUrl = pistonBaseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(pistonBaseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    /**
     * Java requires the source file name to match its public class exactly, and
     * Piston picks an entry class based on the file name it's given. Without an
     * explicit name, Piston guesses (often wrongly, e.g. the first class in the
     * file) when a submission declares multiple classes. So for Java specifically
     * we detect the actual "public class X" and name the file X.java to match.
     */
    private Map<String, Object> fileEntry(String language, String code, String path) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("content", code);

        String fileName = chooseFileName(language, path, code);
        entry.put("name", fileName);
        return entry;
    }

    private String chooseFileName(String language, String path, String code) {
        if (path != null && !path.isBlank()) {
            return basename(path);
        }
        if (isJava(language)) {
            return detectJavaFileName(code);
        }
        return defaultFileName(language);
    }

    private String defaultFileName(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "Main.java";
            case "python" -> "main.py";
            case "javascript" -> "index.js";
            case "typescript" -> "index.ts";
            case "c" -> "main.c";
            case "cpp", "c++" -> "main.cpp";
            case "csharp", "c#" -> "Program.cs";
            case "go" -> "main.go";
            case "ruby" -> "main.rb";
            case "php" -> "index.php";
            default -> "main.txt";
        };
    }

    private String basename(String path) {
        int index = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private boolean isJava(String language) {
        return "java".equalsIgnoreCase(language);
    }

    private String detectJavaFileName(String code) {
        Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(1) + ".java";
        }
        return "Main.java";
    }

    private static final Pattern PUBLIC_CLASS_PATTERN =
            Pattern.compile("public\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)");

    public ExecuteResponse execute(String language, String code, String stdin, String path, List<String> args) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Language is required");
        }

        String version = resolveVersion(language);
        String safeCode = code == null ? "" : code;

        Map<String, Object> body = new HashMap<>();
        body.put("language", language);
        body.put("version", version);
        body.put("files", List.of(fileEntry(language, safeCode, path)));
        body.put("stdin", stdin == null ? "" : stdin);
        if (args != null && !args.isEmpty()) {
            body.put("args", args);
        }

        try {
            PistonExecuteResult result = restClient.post()
                    .uri("/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PistonExecuteResult.class);

            if (result == null || result.run() == null) {
                throw new CodeExecutionException("No output returned by execution engine");
            }

            PistonExecuteResult.RunResult run = result.run();
            PistonExecuteResult.RunResult compile = result.compile();

            String stderr = run.stderr();
            if (compile != null && compile.code() != 0) {
                stderr = "Compile error:\n" + compile.stderr();
            }

            return new ExecuteResponse(run.stdout(), stderr, run.output(), run.code(), language, version);
        } catch (RestClientException e) {
            log.warn("Code execution failed for language '{}' against Piston at {}: {}",
                    language, pistonBaseUrl, e.getMessage());
            throw new CodeExecutionException(
                    "Execution service unavailable, please try again. "
                            + "(Could not reach Piston at " + pistonBaseUrl
                            + " — make sure the Piston container is running; see README 'Code execution engine'.)");
        }
    }

    private synchronized String resolveVersion(String language) {
        if (Instant.now().isAfter(cacheExpiry) || !languageToVersion.containsKey(language)) {
            refreshRuntimes();
        }
        String version = languageToVersion.get(language);
        if (version == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return version;
    }

    private void refreshRuntimes() {
        try {
            List<PistonRuntime> runtimes = restClient.get()
                    .uri("/runtimes")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PistonRuntime>>() {
                    });

            Map<String, String> updated = new HashMap<>();
            if (runtimes != null) {
                for (PistonRuntime runtime : runtimes) {
                    updated.putIfAbsent(runtime.language(), runtime.version());
                    if (runtime.aliases() != null) {
                        for (String alias : runtime.aliases()) {
                            updated.putIfAbsent(alias, runtime.version());
                        }
                    }
                }
            }
            languageToVersion = updated;
            cacheExpiry = Instant.now().plus(RUNTIME_CACHE_TTL);
        } catch (RestClientException e) {
            log.warn("Could not refresh Piston runtime list from {}: {}", pistonBaseUrl, e.getMessage());
            if (languageToVersion.isEmpty()) {
                throw new CodeExecutionException(
                        "Execution service unavailable, please try again. "
                                + "(Could not reach Piston at " + pistonBaseUrl
                                + " — make sure the Piston container is running; see README 'Code execution engine'.)");
            }
        }
    }
}
