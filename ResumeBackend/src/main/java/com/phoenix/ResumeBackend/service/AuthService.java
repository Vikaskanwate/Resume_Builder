package com.phoenix.ResumeBackend.service;

import com.phoenix.ResumeBackend.document.User;
import com.phoenix.ResumeBackend.dto.AuthResponse;
import com.phoenix.ResumeBackend.dto.RegisterRequest;
import com.phoenix.ResumeBackend.exception.ResourceExistsException;
import com.phoenix.ResumeBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public AuthResponse register(RegisterRequest request){
        log.info("Inside AuthService: register() {}",request);
        if (userRepository.existsByEmail(request.getEmail())){
            throw  new ResourceExistsException("User already exists with this email");
        }

        User newUser = toDocument(request);

        userRepository.save(newUser);

        //TODO: send verification email
        return toResponse(newUser);
    }

    private AuthResponse toResponse(User newUser){
        return AuthResponse.builder()
                .id(newUser.getId())
                .name(newUser.getName())
                .email(newUser.getEmail())
                .profileImageUrl(newUser.getProfileImageUrl())
                .emailVerified((newUser.isEmailVerified()))
                .subscriptionPlan(newUser.getSubscriptionPlan())
                .build();
    }

    private User toDocument(RegisterRequest request){
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(LocalDateTime.now().plusHours(24))
                .build();
    }

}
