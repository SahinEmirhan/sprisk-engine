package io.github.sahinemirhan.controller;

import io.github.sahinemirhan.starter.annotation.RiskCheck;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/test")
@RiskCheck(userId = "#headers['X-User-Id'] ?: #user", ruleProperties = {"BRUTE_FORCE.enabled=false", "CREDENTIAL_STUFFING.enabled=false"})
public class TestController {

    @GetMapping("/read")
    public ResponseEntity<String> read(@RequestParam("user") String user) {
        return ResponseEntity.ok("OK READ user=" + user);
    }

    @GetMapping("/login-fail")
    @RiskCheck(
            evaluateOnFailure = true,
            ruleProperties = {
                    "BRUTE_FORCE.enabled=true",
                    "BRUTE_FORCE.windowSeconds=300",
                    "BRUTE_FORCE.maxAttempts=5",
                    "CREDENTIAL_STUFFING.enabled=true",
                    "CREDENTIAL_STUFFING.windowSeconds=600",
                    "CREDENTIAL_STUFFING.maxDistinctUsers=3"
            }
    )
    public ResponseEntity<String> loginFail(@RequestParam("user") String user) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Simulated login failure for user=" + user);
    }

    @GetMapping("/transfer")
    @RiskCheck(
            evaluateAfterSuccess = true,
            ruleProperties = {
                    "USER_VELOCITY.enabled=true",
                    "USER_VELOCITY.windowSeconds=60",
                    "USER_VELOCITY.maxPerWindow=10"
            }
    )
    public ResponseEntity<Boolean> transfer(@RequestParam("user") String user) {
        return ResponseEntity.ok(true);
    }
}
