package com.group12.ciserver.github.model;

import com.group12.ciserver.model.github.PushEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PushEventTest {

    @Test
    public void givenRefsTags_whenGetBranchName_thenBranchName() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setRef("refs/tags/branch/name");

        String actualBranchName = pushEvent.getBranchName();

        assertThat(actualBranchName).isEqualTo("branch/name");
    }

    @Test
    public void givenRefsHeads_whenGetBranchName_thenBranchName() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setRef("refs/heads/branch/name");

        String actualBranchName = pushEvent.getBranchName();

        assertThat(actualBranchName).isEqualTo("branch/name");
    }

    @Test
    public void givenRefsRemotes_whenGetBranchName_thenBranchName() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setRef("refs/remotes/branch/name");

        String actualBranchName = pushEvent.getBranchName();

        assertThat(actualBranchName).isEqualTo("branch/name");
    }

    @Test
    public void givenRefs_whenGetBranchName_thenBranchName() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setRef("refs/branch/name");

        String actualBranchName = pushEvent.getBranchName();

        assertThat(actualBranchName).isEqualTo("branch/name");
    }
}
