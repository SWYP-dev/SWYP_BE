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

    @Column(name = "is_test_account", nullable = false)
    private boolean testAccount;

    public static User create(String email, String nickname, String profileImage, AuthProvider provider, String providerId) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.profileImage = profileImage;
        user.provider = provider;
        user.providerId = providerId;
        return user;
    }

    /**
     * 로그인 없이 전체 기능을 체험할 수 있는 익명 테스트 계정을 생성한다.
     * 개인식별정보를 수집하지 않으며, 호출할 때마다 독립된 유저로 생성되어
     * 동시에 여러 테스터가 사용해도 데이터가 섞이지 않는다.
     */
    public static User createTestAccount(String email, String nickname) {
        User user = User.create(email, nickname, null, AuthProvider.TEST, null);
        user.testAccount = true;
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
