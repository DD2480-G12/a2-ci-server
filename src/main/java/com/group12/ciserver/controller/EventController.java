package com.group12.ciserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EventController {

    @PostMapping("/push-events")
    public ResponseEntity<Void> pushEvent() {
        log.info("Received a ping");
        return ResponseEntity.noContent().build();
    }
}
