# ApplicationPosting 마이그레이션 검증 결과

## 검증 환경

- 검증일: 2026-08-02
- DB: MySQL Community Server 8.0.39
- 애플리케이션: 현재 `chore/application-posting` 작업 트리
- Hibernate 설정: `spring.jpa.hibernate.ddl-auto=validate`
- 검증 DB: 운영 DB와 분리한 로컬 임시 DB

이 결과는 운영 DB 복제본이 아니라 현재 엔티티로 생성한 빈 스키마에 검증용 임시 픽스처를 적용해 만든 레거시 재현 DB를 기준으로 한다. 해당 픽스처는 운영 DB에서 잘못 실행될 위험을 방지하기 위해 저장소에 포함하지 않는다.

## 검증 데이터

- 카드와 문서가 모두 있는 공고 1건
- 카드만 있는 공고 1건
- 카드가 삭제되고 FILE 문서만 남은 공고 1건
- 카드와 문서 어디에서도 참조하지 않는 공고 1건

## Phase 1~3 검증

다음 파일을 순서대로 실행했다.

1. `V20260801_01__expand_application_postings.sql`
2. `V20260801_02__backfill_application_postings.sql`
3. `V20260801_03__constrain_application_postings.sql`
4. `verification/verify_application_posting_migration.sql`

실행 결과:

- `kanban_cards` 백필 대상 2건 모두 매핑 성공
- `documents` 백필 대상 2건 모두 매핑 성공
- `application_postings` 3건 생성
- 카드가 없고 FILE 문서만 남은 1건의 `source_job_posting_id`가 `NULL`로 변경됨
- 유니크 제약, FK, `ON DELETE SET NULL`, 인덱스 및 데이터 정합성 검증 통과
- 레거시 `job_posting_id` 컬럼이 남은 상태에서 애플리케이션 `ddl-auto: validate` 기동 성공
- `Started ServerApplication` 로그 확인

## Phase 4 검증

다음 파일을 순서대로 실행했다.

1. `V20260801_04__drop_legacy_job_posting_columns.sql`
2. `verification/verify_application_posting_migration.sql`

실행 결과:

- `kanban_cards.job_posting_id` 제거 확인
- `documents.job_posting_id` 제거 확인
- Phase 1~4의 `schema_migrations` 기록 확인
- Phase 4 완료 상태에서 애플리케이션 `ddl-auto: validate` 기동 성공
- `Started ServerApplication` 로그 확인

## 재등록 유니크 제약 검증

문서만 남아 원본 연결이 해제된 공고에 대해 동일한 `(user_id, source_job_posting_id)` 조합으로 새 `ApplicationPosting`을 INSERT했다.

- `uk_application_postings_user_source` 충돌 없이 INSERT 성공
- 테스트 INSERT는 트랜잭션에서 확인 후 롤백

## 검증 결론

로컬 MySQL 8 재현 DB에서는 Phase 1~4 실행, 구조·데이터 검증 SQL, Phase 3·4 각각의 `ddl-auto: validate` 기동이 모두 성공했다. 운영 적용 전에는 운영 DB 스냅샷을 생성하고 동일한 검증 SQL을 다시 실행해야 한다.
