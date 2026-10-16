# 운영 앱 서버(EC2) 및 관련 리소스. 기존 리소스를 import로 편입한 것이므로
# name, description 등 교체를 유발하는 값은 실제 값과 반드시 일치해야 한다.

resource "aws_security_group" "app" {
  name        = "launch-wizard-1"
  description = "launch-wizard-1 created 2026-06-18T16:07:48.749Z"
  vpc_id      = var.vpc_id

  tags = {
    Name = "swyp-prod-security"
  }
}

import {
  to = aws_security_group.app
  id = "sg-06fe5ea692a3a68f0"
}

# 22번(SSH) 규칙은 관리하지 않는다. 배포 시 deploy.yml이 러너 IP를 동적으로 추가/삭제한다.

# [DEMO] AI 리뷰 위험 탐지 시연용. 머지 금지.
resource "aws_vpc_security_group_ingress_rule" "ssh_demo" {
  security_group_id = aws_security_group.app.id
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_ipv4         = "0.0.0.0/0"
  description       = "DEMO: SSH open to the world"
}

resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.app.id
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
  description       = "Nginx inbound port for external requests"
}

import {
  to = aws_vpc_security_group_ingress_rule.http
  id = "sgr-0c021a4a8b274e54f"
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.app.id
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
  description       = "Reserved for future SSL/TLS setup"
}

import {
  to = aws_vpc_security_group_ingress_rule.https
  id = "sgr-049967e7a76856a20"
}

# 모니터링 EC2에서 들어오는 수집(scrape) 포트
locals {
  monitoring_ingress = {
    actuator = {
      port        = 8080
      rule_id     = "sgr-04a75ddc497c972e8"
      description = "Actuator - monitoring EC2"
    }
    node_exporter = {
      port        = 9100
      rule_id     = "sgr-0d2f621a76e609080"
      description = null
    }
    mysql_exporter = {
      port        = 9104
      rule_id     = "sgr-04b71c735b2b4e44c"
      description = null
    }
    redis_exporter = {
      port        = 9121
      rule_id     = "sgr-047507963561b9600"
      description = "redis-exporter - monitoring EC2"
    }
  }
}

resource "aws_vpc_security_group_ingress_rule" "monitoring" {
  for_each = local.monitoring_ingress

  security_group_id            = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = each.value.port
  to_port                      = each.value.port
  referenced_security_group_id = var.monitoring_sg_id
  description                  = each.value.description
}

import {
  for_each = local.monitoring_ingress
  to       = aws_vpc_security_group_ingress_rule.monitoring[each.key]
  id       = each.value.rule_id
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"

  tags = {
    Name = "swyp-ec2-security"
  }
}

import {
  to = aws_vpc_security_group_egress_rule.all
  id = "sgr-0541c22baa0da5274"
}

resource "aws_instance" "app" {
  ami                    = "ami-06259b63260eddc13"
  instance_type          = "t3.micro"
  key_name               = "swyp-prod"
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = "swyp-ec2-ecr-pull"
  monitoring             = false
  ebs_optimized          = true

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 15
    iops                  = 3000
    throughput            = 125
    encrypted             = false
    delete_on_termination = true

    tags = {
      Name = "swyp-prod"
    }
  }

  tags = {
    Name = "swyp-prod"
  }

  # AMI 갱신이나 user_data 변경이 서버 교체로 이어지지 않도록 무시
  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

import {
  to = aws_instance.app
  id = "i-0d201c68c2fb9498e"
}

resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = "swyp-prod"
  }
}

import {
  to = aws_eip.app
  id = "eipalloc-02e4dc9f36251b298"
}
