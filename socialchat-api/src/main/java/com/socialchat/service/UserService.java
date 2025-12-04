package com.socialchat.service;

import com.socialchat.dto.user.UserResponse;
import com.socialchat.dto.user.UserUpdateRequest;
import com.socialchat.entity.User;
import com.socialchat.exception.ResourceNotFoundException;
import com.socialchat.mapper.UserMapper;
import com.socialchat.repository.UserRepository;
import com.socialchat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final SecurityUtils securityUtils;

    public UserResponse getCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return userMapper.toResponse(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        User user = securityUtils.getCurrentUser();

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);
        log.info("User updated: {}", user.getUsername());

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse uploadPhoto(MultipartFile file) {
        User user = securityUtils.getCurrentUser();

        String filename = fileStorageService.storeFile(file, "photos");
        user.setPhotoUrl("/media/" + filename);
        user = userRepository.save(user);

        log.info("Photo uploaded for user: {}", user.getUsername());

        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteCurrentUser() {
        User user = securityUtils.getCurrentUser();
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }
}
