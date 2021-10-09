package com.behl.salamanca.service;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.behl.salamanca.constant.OtpContext;
import com.behl.salamanca.dto.OtpVerificationRequestDto;
import com.behl.salamanca.dto.UserAccountCreationRequestDto;
import com.behl.salamanca.dto.UserLoginRequestDto;
import com.behl.salamanca.dto.UserLoginSuccessDto;
import com.behl.salamanca.entity.User;
import com.behl.salamanca.mail.EmailService;
import com.behl.salamanca.repository.UserRepository;
import com.behl.salamanca.security.utility.JwtUtils;
import com.google.common.cache.LoadingCache;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoadingCache<String, Integer> oneTimePasswordCache;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    public ResponseEntity<?> createAccount(final UserAccountCreationRequestDto userAccountCreationRequestDto) {
        if (userRepository.existsByEmailId(userAccountCreationRequestDto.getEmailId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User account already exists for provided email-id");

        final var user = new User();
        user.setEmailId(userAccountCreationRequestDto.getEmailId());
        user.setPassword(passwordEncoder.encode(userAccountCreationRequestDto.getPassword()));
        user.setEmailVerified(false);
        final var savedUser = userRepository.save(user);

        sendOtp(savedUser, "Verify your account");
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> login(final UserLoginRequestDto userLoginRequestDto) {
        final User user = userRepository.findByEmailId(userLoginRequestDto.getEmailId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login credentials"));

        if (!passwordEncoder.matches(userLoginRequestDto.getPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login credentials");

        sendOtp(user, "2FA: Request to log in to your account");
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<UserLoginSuccessDto> verifyOtp(final OtpVerificationRequestDto otpVerificationRequestDto) {
        User user = userRepository.findByEmailId(otpVerificationRequestDto.getEmailId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email-id"));

        if (otpVerificationRequestDto.getContext().equals(OtpContext.SIGN_UP)) {
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }

        Integer storedOneTimePassword = null;
        try {
            storedOneTimePassword = oneTimePasswordCache.get(user.getEmailId());
        } catch (ExecutionException e) {
            log.error("FAILED TO FETCH PAIR FROM OTP CACHE: {}", e);
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        }

        if (storedOneTimePassword != null) {
            if (storedOneTimePassword.equals(otpVerificationRequestDto.getOneTimePassword()))
                return ResponseEntity.ok(UserLoginSuccessDto.builder().jwt(jwtUtils.generateToken(user)).build());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } else
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    private void sendOtp(final User user, final String subject) {
        try {
            if (oneTimePasswordCache.get(user.getEmailId()) != null)
                oneTimePasswordCache.invalidate(user.getEmailId());
        } catch (ExecutionException e) {
            log.error("FAILED TO FETCH PAIR FROM OTP CACHE: {}", e);
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        }

        final var otp = new Random().ints(1, 100000, 999999).sum();
        oneTimePasswordCache.put(user.getEmailId(), otp);

        emailService.sendEmail(user.getEmailId(), subject, "OTP: " + otp);
    }

}
