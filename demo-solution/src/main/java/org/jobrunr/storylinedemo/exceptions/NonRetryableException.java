package org.jobrunr.storylinedemo.exceptions;

import org.jobrunr.JobRunrException;

public class NonRetryableException extends JobRunrException {

    public NonRetryableException(String message) {
        super(message, true);
    }
}
