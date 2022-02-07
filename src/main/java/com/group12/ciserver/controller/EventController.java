package com.group12.ciserver.controller;

import com.group12.ciserver.GithubClient;
import com.group12.ciserver.model.github.CommitState;
import com.group12.ciserver.model.github.PushEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EventController {

    @Autowired
    private GithubClient githubClient;

    @PostMapping("/push-events")
    public ResponseEntity<Void> pushEvent(@RequestBody PushEvent pushEvent) {
        log.info("Received push event, pushEvent={}", pushEvent);
        return ResponseEntity.noContent().build();
    }
}
