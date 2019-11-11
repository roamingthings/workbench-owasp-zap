package de.roamingthings.workbenchowaspzap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/smoke")
public class SmokeController {
    @GetMapping
    public ResponseEntity<String> doSmoke() {
        return ok("Smoking");
    }

    @GetMapping("/2")
    public ResponseEntity<String> doSmoke2() {
        return ok("Smoking2");
    }

    @GetMapping("/3")
    public ResponseEntity<String> doSmoke3() {
        return ok("Smoking3");
    }
}
