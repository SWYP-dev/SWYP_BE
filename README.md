# CHWIHAP (취합) — 취업 공고 통합 관리 서비스 · Backend

> SWYP 웹 14기 팀 프로젝트 백엔드 레포지토리입니다.
> 여러 채용 플랫폼에 흩어진 공고를 한곳에 모으고, 지원 과정을 칸반 보드로 관리할 수 있는 서비스입니다.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20S3-FF9900?logo=amazonaws&logoColor=white)](https://aws.amazon.com/)
[![Deploy](https://github.com/SWYP-dev/SWYP_BE/actions/workflows/deploy.yml/badge.svg)](https://github.com/SWYP-dev/SWYP_BE/actions/workflows/deploy.yml)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [핵심 기능](#핵심-기능)
- [아키텍처](#아키텍처)
- [기술 스택](#기술-스택)

## 프로젝트 소개

취업 준비생은 워크넷, 잡코리아, 사람인, 점핏 등 여러 플랫폼을 오가며 공고를 확인하고, 지원 현황은 각자 메모장이나 엑셀로 따로 관리하는 경우가 많습니다. **취합**은 여러 외부 채용 데이터 소스를 하나의 스키마로 통합해 피드 형태로 제공하고, 지원 과정을 칸반 보드로 관리할 수 있게 해 이 두 가지 문제를 함께 해결하는 서비스입니다.

## 핵심 기능

- **소셜 로그인 & 인증** — 카카오 OAuth2 로그인, JWT Access/Refresh Token, Refresh Token 보안 강화 및 Rate Limiting
- **공고 피드** — 워크넷·공공데이터·인사혁신처·잡아바 등 복수 외부 API를 배치로 수집·정규화해 통합 피드로 제공, 키워드/필터 검색, 스크랩
- **칸반 보드** — 지원 단계(지원 예정/지원 완료/서류 통과/면접 등)별 공고 관리
- **문서 관리** — 이력서/자기소개서 등 지원 서류 버전 관리, S3 업로드
- **알림** — 지원 마감 임박, 상태 변경 등 이메일 알림
- **관측 가능성** — Prometheus + Grafana 기반 모니터링, 배치 작업 성공률/소요시간 지표 수집

## 아키텍처

<img width="1653" height="973" alt="취합_인프라구성도3 drawio" src="https://github.com/user-attachments/assets/973b78a0-ef7f-46cd-9a28-e216867b0dd6" />


- **무중단 배포**: main 브랜치 push → GitHub Actions에서 빌드·테스트 → Blue/Green 컨테이너 전환 → Nginx가 트래픽을 살아있는 컨테이너로 스위칭
- **비용 절충**: 관리형 DB(RDS)·캐시(ElastiCache) 대신 EC2 한 대에 Docker로 직접 구성 — 소규모 트래픽 단계에서 비용 대비 합리적인 선택으로 판단, 이에 따른 트레이드오프(백업/장애복구 수동화 등)는 인지하고 있으며 개선 로드맵으로 관리 중
- **관측성**: node/mysqld/redis exporter + Prometheus + Grafana로 인프라·애플리케이션 지표 수집, 배치 작업 성공 여부·소요시간을 커스텀 Gauge로 노출

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA |
| Database | MySQL 8.0, Redis 7 (Redisson) |
| Auth | OAuth2 (Kakao), JWT (jjwt) |
| Storage | AWS S3 |
| Infra / CI-CD | EC2, Docker, Nginx (Blue/Green), GitHub Actions |
| Monitoring | Prometheus, Grafana, Micrometer |
| API 문서 | springdoc-openapi (Swagger UI) |
| Test / Load Test | JUnit5, k6 |


