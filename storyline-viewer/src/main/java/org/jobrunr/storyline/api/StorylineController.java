package org.jobrunr.storyline.api;

import org.jobrunr.storyline.model.Storyline;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StorylineController {

    private final Storyline storyline;

    public StorylineController(Storyline storyline) {
        this.storyline = storyline;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("storyline", storyline);
        return "index";
    }

    @GetMapping("/storyline")
    public String guide(Model model) {
        model.addAttribute("storyline", storyline);
        return "guide";
    }
}
