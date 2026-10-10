variable "aws_region" {
  description = "리소스가 위치한 AWS 리전 (deploy.yml의 AWS_REGION과 동일)"
  type        = string
  default     = "ap-southeast-2"
}

variable "vpc_id" {
  description = "앱 서버가 속한 VPC ID"
  type        = string
  default     = "vpc-0c0bc2a83c71b5391"
}

variable "subnet_id" {
  description = "앱 서버가 속한 서브넷 ID (ap-southeast-2c)"
  type        = string
  default     = "subnet-03b63bfb758219c09"
}

variable "monitoring_sg_id" {
  description = "모니터링 EC2의 보안그룹 ID (exporter 수집 포트 허용 소스)"
  type        = string
  default     = "sg-027992fcf39293ab3"
}
