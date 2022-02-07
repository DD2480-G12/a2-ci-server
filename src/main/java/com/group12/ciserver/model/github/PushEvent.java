package com.group12.ciserver.model.github;

import lombok.Data;

@Data
public class PushEvent {

    /**
     * The full git ref that was pushed. Example: refs/heads/main or refs/tags/v3.14.1
     */
    private String ref;

    /**
     * The SHA of the most recent commit on {@link PushEvent#ref} after the push.
     */
    private String after;

    /**
     * The repository where the event occurred.
     */
    private Repository repository;

    public String getBranchName() {
        if (ref.startsWith("refs/tags/")) {
            return ref.substring(10);
        }
        if (ref.startsWith("refs/heads/")) {
            return ref.substring(11);
        }
        if (ref.startsWith("refs/remotes/")) {
            return ref.substring(13);
        }
        return ref.substring(5);
    }
}
