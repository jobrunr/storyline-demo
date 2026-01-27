package org.jobrunr.storyline.api;

import org.jobrunr.storyline.model.Storyline;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

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

    @GetMapping({"/storyline", "/storyline/"})
    public String guide(Model model) {
        model.addAttribute("storyline", storyline);
        return "guide";
    }

    @GetMapping("/storyline/step/{stepNumber}")
    public String step(@PathVariable int stepNumber, @RequestHeader(value = "HX-Request", required = false) String hxRequest, Model model) {
        model.addAttribute("storyline", storyline);
        model.addAttribute("step", storyline.getStep(stepNumber));
        return hxRequest != null ? "step" : "guide";
    }
}
