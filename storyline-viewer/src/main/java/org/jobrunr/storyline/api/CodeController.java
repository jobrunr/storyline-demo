package org.jobrunr.storyline.api;

import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

@Controller
public class CodeController {

    private final Storyline storyline;

    public CodeController(Storyline storyline) {
        this.storyline = storyline;
    }

    @GetMapping(value = "/code/{*codeReference}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String code(@PathVariable String codeReference, HttpServletRequest request) {
        String focus = request.getParameter("focus");
        String show = Optional.ofNullable(request.getParameter("show")).orElse("focus");

        String code = getCodeFileAsStringFromProjectOrGithub(codeReference);
        code = getCodeToShow(code, focus, show);
        code += getFileViewSwitch(codeReference, focus, show);
        return code;
    }

    private static String getCodeToShow(String code, String focus, String show) {
        if (isNotBlank(focus) && "focus".equals(show)) {
            int startLine = -1, endLine = -1;
            if (focus.startsWith("L")) {
                startLine = Integer.parseInt(substringBefore(focus, "-").replace("L", ""));
                endLine = Integer.parseInt(substringAfter(focus, "-").replace("L", ""));
            } else if (focus.startsWith("#")) { // find relevant method
                throw new UnsupportedOperationException("Method focus not implemented yet");
            }
            return stream(code.split(System.lineSeparator()))
                    .skip(startLine - 1)
                    .limit(endLine - startLine + 1)
                    .collect(joining(System.lineSeparator()));
        }
        return code;
    }

    private String getFileViewSwitch(String codeFile, String focus, String show) {
        String fileName = StringUtils.substringAfterLast(codeFile, "/");
        String toShow = "focus".equals(show) ? "all" : "focus";
        String label = "all".equals(toShow) ? "view complete file" : "view change";
        return System.lineSeparator() + String.format("""
                <a id="%s-view-switch" hx-swap-oob="true" class="is-inline-block mr-2" hx-get="/code%s" hx-target="next pre code" hx-on::after-settle="highlightCode(this.parentElement.querySelector('pre code'));">(%s)</a>
                """, fileName, codeFile + "?focus=" + focus + "&show=" + toShow, label);
    }

    private String getCodeFileAsStringFromProjectOrGithub(String codeFile) {
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
}
