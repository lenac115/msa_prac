package com.example.auth.test;

import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/auth/public")
@RequiredArgsConstructor
public class TestResetController {

    private final UserRepository userRepository;

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        userRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}