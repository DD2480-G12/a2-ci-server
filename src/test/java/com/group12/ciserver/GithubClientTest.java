package com.group12.ciserver;

import com.group12.ciserver.model.github.CommitState;
import com.group12.ciserver.model.github.Owner;
import com.group12.ciserver.model.github.PushEvent;
import com.group12.ciserver.model.github.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GithubClientTest {

    @Autowired
    private GithubClient githubClient;

    @Test
    void givenCorrectParameters_whenCreateStatusMsg_thenNoException() {
        Owner owner = new Owner();
        owner.setName("DD2480-G12");

        Repository repo = new Repository();
        repo.setName("a2-ci-server");
        repo.setCloneUrl("https://github.com/DD2480-G12/a2-ci-server.git");
        repo.setOwner(owner);

        PushEvent pe = new PushEvent();
        pe.setAfter("3f833431bece5b7799e268498d227e7f2b005a31");
        pe.setRepository(repo);

        assertDoesNotThrow(() -> githubClient.createStatusMsg(pe, CommitState.SUCCESS, "Unit test"));
    }

    @Test
    void givenIncorrectParameters_whenCreateStatusMsg_thenHttpClientErrorException() {
        Owner owner = new Owner();
        owner.setName("DD2480-G12");

        Repository repo = new Repository();
        repo.setName("a2-ci-serverrrrr"); // Misspelled on purpose
        repo.setCloneUrl("https://github.com/DD2480-G12/a2-ci-server.git");
        repo.setOwner(owner);

        PushEvent pe = new PushEvent();
        pe.setAfter("3f833431bece5b7799e268498d227e7f2b005a31");
        pe.setRepository(repo);

        assertThrows(HttpClientErrorException.class, () -> githubClient.createStatusMsg(pe, CommitState.SUCCESS, "Unit test"));
    }
}
