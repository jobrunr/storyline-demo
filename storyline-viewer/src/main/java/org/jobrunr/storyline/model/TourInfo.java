package org.jobrunr.storyline.model;

import java.util.List;

/**
 * Everything the dashboard-centered tour at /tour needs for a single step: what to narrate,
 * which live dashboard element to point the beacon at, and what the visitor can trigger from
 * the popup. Steps without a {@code tour:} block still get a usable TourInfo (see StorylineReader),
 * so the tour never breaks while the content pass is in flight.
 *
 * @param narration  short, spoken-word explanation shown in the popup (may contain inline HTML)
 * @param anchor     the dashboard element the beacon attaches to, null for {@code info} steps
 * @param placement  preferred popup side: auto, top, right, bottom or left
 * @param mode       dashboard (beacon on the stage), app (slide-in banking form) or info (centered card)
 * @param actions    one-click triggers rendered as buttons inside the popup
 * @param screenshot  absolute path of a still to show for {@code info} steps
 * @param skip        leaves this step out of the tour entirely; the written guide keeps it
 * @param historyView how a job page shows its history while this step is open: timeline or chart
 * @param liveNotice  replaces the guide's notice on the live demo, an empty value drops it
 */
public record TourInfo(
        String narration,
        TourAnchor anchor,
        String placement,
        String mode,
        List<TourAction> actions,
        String screenshot,
        boolean skip,
        String historyView,
        String liveNotice) {

    public static final String MODE_DASHBOARD = "dashboard";
    public static final String MODE_APP = "app";
    public static final String MODE_INFO = "info";

    public static final String HISTORY_TIMELINE = "timeline";
    public static final String HISTORY_CHART = "chart";

    /**
     * @param page             dashboard URL the stage must be on, e.g. {@code /dashboard/jobs?state=FAILED}
     * @param selector         primary CSS selector, resolved inside the dashboard iframe
     * @param fallbackSelector used when the primary selector never becomes visible
     * @param reveal           optional selector clicked first to expose a collapsed control (the Filters panel)
     * @param match            {@code path} to keep the visitor on this exact page, {@code prefix} to let them roam
     */
    public record TourAnchor(String page, String selector, String fallbackSelector, String reveal, String match) {

        public static final String MATCH_PATH = "path";
        public static final String MATCH_PREFIX = "prefix";
    }

    /**
     * A trigger may answer with a {@code stageUrl}, typically the page of the job it just created. The
     * stage then moves there and the beacon re-anchors on {@code focus}, with the popup on {@code placement}.
     *
     * @param anonymous whether the endpoint may be called without signing in
     * @param focus     selector to point at once the stage followed the trigger's {@code stageUrl}
     * @param placement popup side once the stage followed the trigger, null keeps the step's
     */
    public record TourAction(String label, String url, String method, boolean anonymous, String focus, String placement) {
    }
}
