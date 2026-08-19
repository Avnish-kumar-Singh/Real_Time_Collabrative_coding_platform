package com.codesync.controller;

import com.codesync.dto.ExecuteRequest;
import com.codesync.dto.ExecuteResponse;
import com.codesync.service.CodeExecutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionController {

    private final CodeExecutionService codeExecutionService;

    public ExecutionController(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @PostMapping("/execute")
    public ExecuteResponse execute(@RequestBody ExecuteRequest request) {
        return codeExecutionService.execute(
                request.getLanguage(),
                request.getCode(),
                request.getStdin(),
                request.getPath(),
                request.getArgs());
    }
}
