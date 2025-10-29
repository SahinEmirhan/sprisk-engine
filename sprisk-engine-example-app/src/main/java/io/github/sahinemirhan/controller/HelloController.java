package io.github.sahinemirhan.controller;

import io.github.sahinemirhan.starter.annotation.RiskCheck;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RiskCheck(userId = "#headers['X-User-Id'] ?: #user", ruleProperties = {"BRUTE_FORCE.enabled=false", "CREDENTIAL_STUFFING.enabled=false"})
public class HelloController {

    @GetMapping("/hello")
    @RiskCheck(ruleProperties = {"IP_VELOCITY.enabled=true", "IP_VELOCITY.windowSeconds=30", "IP_VELOCITY.maxPerWindow=10"})
    public ResponseEntity<String> hello(@RequestParam String user) {
        return ResponseEntity.ok("Hello" + user);
    }

}
