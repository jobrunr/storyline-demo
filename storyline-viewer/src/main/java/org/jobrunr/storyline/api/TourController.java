package org.jobrunr.storyline.api;

import org.jobrunr.storyline.model.Storyline;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Serves the dashboard-centered tour. Every route renders the same shell: the step number lives in
 * the URL purely so analytics can compare /tour/step/N against the guide's /storyline/step/N funnel,
 * while the overlay engine itself reads it from the address bar and drives the rest client-side.
 */
@Controller
public class TourController {

    private final Storyline storyline;
    private final String tourJson;
    private final List<TourActView> tourActs;

    /** One act with its tour steps, in tour numbering: what the shell template iterates over. */
    public record TourActView(TourData.Act act, List<TourData.Step> steps) {
    }

    public TourController(Storyline storyline) {
        this.storyline = storyline;
        TourData data = TourData.from(storyline);
        this.tourJson = asEmbeddableJson(data);
        this.tourActs = data.acts().stream()
                .map(act -> new TourActView(act, data.steps().stream()
                        .filter(step -> step.act() == act.index())
                        .toList()))
                .toList();
    }

    @GetMapping({"/tour", "/tour/"})
    public String tour(Model model) {
        return render(model);
    }

    @GetMapping("/tour/step/{stepNumber}")
    public String step(@PathVariable int stepNumber, Model model) {
        return render(model);
    }

    private String render(Model model) {
        model.addAttribute("storyline", storyline);
        model.addAttribute("tourJson", tourJson);
        model.addAttribute("tourActs", tourActs);
        return "tour";
    }

    /**
     * Escapes every {@code <} so the blob can never close its own {@code <script>} tag. {@code \\u003c}
     * is an ordinary JSON string escape, so the parsed value is unchanged.
     */
    private static String asEmbeddableJson(TourData data) {
        return new ObjectMapper().writeValueAsString(data).replace("<", "\\u003c");
    }
}
