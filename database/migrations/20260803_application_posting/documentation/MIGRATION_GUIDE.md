# ApplicationPosting 마이그레이션 가이드

실행 전에 반드시 [ROLLBACK.md](ROLLBACK.md)를 확인하고 운영 DB 스냅샷을 생성한다. 로컬 사전 검증 결과는 [VALIDATION_RESULT.md](VALIDATION_RESULT.md)에서 확인할 수 있다. 애플리케이션을 중지한 뒤 아래 순서로 실행한다. MySQL 클라이언트의 `source` 명령을 사용해야 각 파일의 `DELIMITER` 구문이 정상 처리된다.

## Phase 1~3: 스키마 확장과 데이터 백필

```sql
source database/migrations/20260803_application_posting/V20260803_01__expand_application_postings.sql;
source database/migrations/20260803_application_posting/V20260803_02__backfill_application_postings.sql;
source database/migrations/20260803_application_posting/V20260803_03__constrain_application_postings.sql;
source database/migrations/20260803_application_posting/verification/verify_application_posting_migration.sql;
```

Phase 3과 검증이 성공하면 새 애플리케이션을 `ddl-auto: validate`로 기동한다.

## Phase 4: 레거시 컬럼 제거

새 애플리케이션의 운영 검증과 롤백 대기 기간이 끝난 뒤에만 실행한다.

```sql
source database/migrations/20260803_application_posting/V20260803_04__drop_legacy_job_posting_columns.sql;
source database/migrations/20260803_application_posting/verification/verify_application_posting_migration.sql;
```
