package org.jobrunr.storyline.model;

import java.util.List;

/**
 * What the mobile card deck at /m shows for a step. Every field falls back to the written guide, so a
 * step only overrides what the phone version should tell differently, without touching /storyline.
 *
 * @param dashboardUrl the page the "See it live" card embeds
 * @param latestJob    dashboard API queries (e.g. {@code states=SUCCEEDED&label=x}) tried in order; the card
 *                     embeds the page of the newest job the first one finds, else {@code dashboardUrl}
 * @param historyView  how a job page shows its history inside that card: timeline or chart
 */
public record MobileInfo(
        String scenario,
        String challenge,
        String solution,
        String tryIt,
        String dashboardUrl,
        List<String> latestJob,
        String historyView) {
}
