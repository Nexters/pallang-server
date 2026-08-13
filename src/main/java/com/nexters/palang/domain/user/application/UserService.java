package com.nexters.palang.domain.user.application;

import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.storage.FileStorageService;
import com.nexters.palang.global.storage.ImageMimeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String PROFILE_IMAGE_SUB_DIRECTORY = "profile-images";

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public User getMe(Long userId) {
        return getActiveUser(userId);
    }

    @Transactional
    public User modifyNickname(Long userId, String nickname) {
        User user = getActiveUser(userId);
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new UserException(UserErrorCode.NICKNAME_ALREADY_IN_USE);
        }
        user.changeNickname(nickname);
        return user;
    }

    @Transactional
    public User modifyBackgroundColor(Long userId, String backgroundColor) {
        User user = getActiveUser(userId);
        user.changeBackgroundColor(backgroundColor);
        return user;
    }

    @Transactional
    public User modifyProfileImage(Long userId, MultipartFile image) {
        if (ImageMimeType.from(image.getContentType()).isEmpty()) {
            throw new UserException(UserErrorCode.INVALID_IMAGE_FILE);
        }
        User user = getActiveUser(userId);
        String profileImageUrl = fileStorageService.store(image, PROFILE_IMAGE_SUB_DIRECTORY);
        user.changeProfileImage(profileImageUrl);
        return user;
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getActiveUser(userId);
        user.withdraw();
    }

    @Transactional
    public void completeOnboarding(Long userId) {
        User user = getActiveUser(userId);
        user.completeOnboarding();
    }

    // 탈퇴한 사용자는 더 이상 조작 대상이 아니므로 존재하지 않는 것과 동일하게 취급한다.
    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
