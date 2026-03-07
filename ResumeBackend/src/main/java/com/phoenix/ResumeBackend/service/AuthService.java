package com.phoenix.ResumeBackend.service;

import com.phoenix.ResumeBackend.document.User;
import com.phoenix.ResumeBackend.dto.AuthResponse;
import com.phoenix.ResumeBackend.dto.RegisterRequest;
import com.phoenix.ResumeBackend.exception.ResourceExistsException;
import com.phoenix.ResumeBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;

    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request){
        log.info("Inside AuthService: register() {}",request);
        if (userRepository.existsByEmail(request.getEmail())){
            throw  new ResourceExistsException("User already exists with this email");
        }

        User newUser = toDocument(request);

        userRepository.save(newUser);

        //TODO: send verification email

        sendVerificationEmail(newUser);
        return toResponse(newUser);
    }

    private void sendVerificationEmail(User newUser){
        log.info("Inside AuthService - sendVerificationEmail(): {}",newUser);
        try{
            String link = appBaseUrl+"/api/auth/verify-email?token="+newUser.getVerificationToken();
            String html= "<div style='font-family:sans-serif'>"+
                    "<h2>Verify your email </h2>"+
                    "<p>Hi " + newUser.getName() + " ,please confirm your email to activate your account</p>"+
                    "<p><a href='"+link
                    +"' style='display:inline-block;padding:10px 16px;background:#6366f1;color:#fff;border-radius:6px;text-decoration:none'>Verify Email</p>"
                    +"<p>Or copy this link: " + link + "</p>"+
                    "<p> this link expires in 24 hours"+
                    "</div>";
            emailService.sendHtmlEmail(newUser.getEmail(),"Verify your email",html);

        }catch (Exception e){
            throw new RuntimeException("failed to send verification email: "+e.getMessage());
        }
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

    public void verifyEmail(String token){
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(()->new RuntimeException("Invalid or expired verification token"));
        if(user.getVerificationToken() != null && user.getVerificationExpires().isBefore(LocalDateTime.now())){
            throw new RuntimeException("verification token has expired. please request new one");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        userRepository.save(user);
    }

}
