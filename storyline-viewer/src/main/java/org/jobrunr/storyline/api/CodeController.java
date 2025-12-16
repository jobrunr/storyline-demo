package org.jobrunr.storyline.api;

import org.apache.commons.lang3.StringUtils;
import org.jobrunr.storyline.model.Storyline;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class CodeController {

    private final Storyline storyline;

    public CodeController(Storyline storyline) {
        this.storyline = storyline;
    }

    @GetMapping(value = "/code/{*codeReference}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String code(@PathVariable String codeReference) {
        String codeFile = StringUtils.substringBefore(codeReference, "#");
        String methodReference = StringUtils.substringAfter(codeReference, "#");

        // Try to load from local project first
        return loadFromLocalProject(codeFile)
                .or(() -> loadFromGitHub(codeFile))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code file not found: " + codeFile));
    }

    private Optional<String> loadFromLocalProject(String codeFile) {
        try {
            Path filePath = Paths.get(storyline.codeRoot(), codeFile);
            if (Files.exists(filePath)) {
                return Optional.of(Files.readString(filePath));
            }
        } catch (IOException e) {
            // File not found locally, will try GitHub
        }
        return Optional.empty();
    }

    private Optional<String> loadFromGitHub(String codeFile) {
        // TODO: Implement GitHub fetching using WebClient or similar
        // For now, return null to indicate not implemented
        return Optional.empty();
    }

    private String extractMethod(String code, String methodName) {
        String[] lines = code.split("\n");
        List<String> methodLines = new ArrayList<>();
        boolean inMethod = false;
        int braceCount = 0;
        boolean foundMethod = false;

        for (String line : lines) {
            // Look for method declaration
            if (!inMethod && line.contains(methodName) && (line.contains("public") || line.contains("private") || line.contains("protected"))) {
                inMethod = true;
                foundMethod = true;
            }

            if (inMethod) {
                methodLines.add(line);

                // Count braces to know when method ends
                for (char c : line.toCharArray()) {
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                }

                // Method ends when braces are balanced
                if (braceCount == 0 && line.contains("}")) {
                    break;
                }
            }
        }

        return foundMethod ? String.join("\n", methodLines) : code;
    }
}
