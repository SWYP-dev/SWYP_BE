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

    /**
     * 회원 탈퇴 처리. providerId/email을 익명화해 소셜 계정 연동을 끊어
     * 이후 같은 카카오 계정으로 재로그인하면 신규 가입으로 처리되도록 한다.
     * row 자체는 삭제하지 않고 deletedAt만 기록하며, 보관 기간 경과 후 하드 삭제하는
     * 배치는 관련 도메인(kanban/document/feed/notification 등) cascade 설계가 필요해 별도 이슈로 분리한다.
     */
    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.providerId = null;
        this.email = "withdrawn_" + this.id + "@deleted.chwihap.com";
    }

    public boolean isWithdrawn() {
        return this.deletedAt != null;
    }

}
