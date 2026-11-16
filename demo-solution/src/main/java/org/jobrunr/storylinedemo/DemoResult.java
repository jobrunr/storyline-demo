package org.jobrunr.storylinedemo;

/**
 * What the tour's one-click triggers answer with: a message its popup can show verbatim and, optionally,
 * the dashboard page the stage should move to, such as the job the trigger just created.
 */
public record DemoResult(boolean ok, String message, String stageUrl) {

    static DemoResult ok(String message) {
        return new DemoResult(true, message, null);
    }

    static DemoResult ok(String message, String stageUrl) {
        return new DemoResult(true, message, stageUrl);
    }

    static DemoResult nothingToDo(String message) {
        return new DemoResult(false, message, null);
    }
}
