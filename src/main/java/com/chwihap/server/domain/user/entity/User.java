package com.chwihap.server.domain.user.entity;

import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "idx_users_provider_id",
                        columnNames = {"provider_id", "provider"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", nullable = true, length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(nullable = true)
    private String providerId;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    public static User create(String email, String nickname, String profileImage, AuthProvider provider, String providerId) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.profileImage = profileImage;
        user.provider = provider;
        user.providerId = providerId;
        return user;
    }

    public void updateProfile(String nickname, String profileImage) {
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

}
