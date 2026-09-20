# infra/terraform

기존 AWS 리소스를 Terraform으로 관리하기 위한 디렉터리. 앱 배포(blue/green)는 `.github/workflows/deploy.yml`이 계속 담당한다.

## 원칙
- 운영 리소스는 `terraform import`로 편입하고, `terraform plan`이 "No changes"가 될 때까지 코드를 실제 설정에 맞춘다.
- 초기에는 `apply` 없이 `plan`만 실행한다.
- 보안그룹은 인라인 `ingress` 블록 대신 `aws_vpc_security_group_ingress_rule`로 규칙을 개별 관리한다.
- **22번(SSH) 규칙은 코드에 넣지 않는다.** `deploy.yml`이 배포 시마다 러너 IP를 동적으로 추가/삭제한다.

## 로컬 실행
```
cd infra/terraform
terraform init
terraform plan
```
