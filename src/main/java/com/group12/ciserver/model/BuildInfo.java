package com.group12.ciserver.model;


import java.time.OffsetDateTime;

public class BuildInfo {

    private final long uid;
    private final String commitId;
    private final String content;
    private final OffsetDateTime timestamp;

    public BuildInfo(String commitId, String content) {
        this(-1, commitId, content);
    }

    public BuildInfo(long uid, String commitId, String content) {
        this.uid = uid;
        this.commitId = commitId;
        this.content = content;
        this.timestamp = OffsetDateTime.now();
    }

    public BuildInfo(long uid, String commitId, String content, OffsetDateTime ts) {
        this.uid = uid;
        this.commitId = commitId;
        this.content = content;
        this.timestamp = ts;
    }

    public OffsetDateTime getTimestamp() {
        return this.timestamp;
    }

    public long getUID() {
        return this.uid;
    }

    public String getCommitId() {
        return this.commitId;
    }

    public String getContent() {
        return this.content;
    }
}
