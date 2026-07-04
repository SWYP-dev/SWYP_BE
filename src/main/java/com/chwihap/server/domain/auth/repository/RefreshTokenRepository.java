package com.chwihap.server.domain.auth.repository;

import com.chwihap.server.domain.auth.entity.RefreshToken;
import com.chwihap.server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
