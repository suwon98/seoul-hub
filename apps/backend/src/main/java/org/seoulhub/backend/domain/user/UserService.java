package org.seoulhub.backend.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public Long signUp(UserSignUpRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        User user = requestDto.toEntity(requestDto.getPassword());
        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }
}
