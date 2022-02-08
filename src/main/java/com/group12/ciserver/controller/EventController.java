package com.group12.ciserver.controller;


import com.group12.ciserver.model.github.PushEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.group12.ciserver.database.DatabaseWrapper;
import com.group12.ciserver.model.BuildInfo;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;


@RestController
@Slf4j
public class EventController {
    @Autowired
    private DatabaseWrapper databaseWrapper;

    @PostMapping("/push-events")
    public ResponseEntity<Void> pushEvent(@RequestBody PushEvent pushEvent) {
        log.info("Received push event, pushEvent={}", pushEvent);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @ResponseBody
    public ArrayList<BuildInfo> buildHistory() {
        return databaseWrapper.getAllBuilds();
    }

    @GetMapping("/history/{buildId}")
    @ResponseBody
    public ResponseEntity<BuildInfo> buildInfo(@PathVariable String buildId) {
        long uid;
        try {
            uid = Integer.parseInt(buildId);
        } catch (NumberFormatException e) {
            System.err.println("Bad format for build ID: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        // Query database for the buildId
        BuildInfo b = databaseWrapper.getBuildInfo(uid);

        if (b != null)
        {
            return ResponseEntity.ok(b);
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    // for testing
    @GetMapping("/add/{commitId}/{content}")
    @ResponseBody
    public ResponseEntity<Long> addBuildForTesting(@PathVariable String commitId, @PathVariable String content) {
        BuildInfo b = new BuildInfo(commitId, content);
        long result = databaseWrapper.addBuild(b);
        if (result != -1) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    // for testing
    @GetMapping("/remove/{buildId}")
    @ResponseBody
    public ResponseEntity<Boolean> removeBuildForTesting(@PathVariable String buildId) {
        long uid;
        try {
            uid = Integer.parseInt(buildId);
        } catch (NumberFormatException e) {
            System.err.println("Bad format for build ID: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        Boolean result = databaseWrapper.removeBuild(uid);
        if (result)
        {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
