package com.recipenetwork.backend.auth;

import com.recipenetwork.backend.common.ApiException;
import com.recipenetwork.backend.common.ImageStorageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountService {

    private static final int MAX_NAME_LENGTH = 255;

    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    public AccountService(UserRepository userRepository, ImageStorageService imageStorageService) {
        this.userRepository = userRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public CurrentUserResponse updateProfile(Long userId, UpdateAccountRequest request) {
        User user = requireUser(userId);
        String displayName = validateDisplayName(request.displayName());
        String email = validateEmail(request.email());

        user.updateProfile(displayName, email);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "E-postadressen används redan.");
        }
        return toResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateAvatar(Long userId, MultipartFile image) {
        User user = requireUser(userId);
        String storedPath = imageStorageService.store(image);
        user.setAvatarUrl(storedPath);
        return toResponse(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad."));
    }

    private String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_NAME", "Namnet får inte vara tomt.");
        }
        String trimmed = displayName.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_NAME", "Namnet är för långt.");
        }
        return trimmed;
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EMAIL", "E-postadress saknas.");
        }
        String trimmed = email.trim();
        if (!trimmed.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EMAIL", "Ogiltig e-postadress.");
        }
        return trimmed;
    }

    private CurrentUserResponse toResponse(User user) {
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl());
    }
}
