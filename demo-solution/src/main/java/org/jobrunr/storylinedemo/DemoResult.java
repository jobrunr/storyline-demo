package org.jobrunr.storylinedemo;

/** What the tour's one-click triggers answer with: a message its popup can show verbatim. */
public record DemoResult(boolean ok, String message) {

    static DemoResult ok(String message) {
        return new DemoResult(true, message);
    }

    static DemoResult nothingToDo(String message) {
        return new DemoResult(false, message);
    }
}
