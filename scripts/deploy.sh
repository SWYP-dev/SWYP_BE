#!/usr/bin/env bash
set -euo pipefail

# 실행 위치: EC2 /home/ubuntu/app
# 필요 환경변수: ECR_URI, IMAGE_TAG (GitHub Actions가 SSH로 전달)

APP_DIR="/home/ubuntu/app"
DOCKER_DIR="$APP_DIR/docker"
ACTIVE_COLOR_FILE="$APP_DIR/active_color"
NGINX_BLUE_GREEN_DIR="/etc/nginx/blue-green"
NGINX_ACTIVE_LINK="$NGINX_BLUE_GREEN_DIR/active_upstream.conf"
AWS_REGION="ap-southeast-2"
HEALTH_RETRY=30
HEALTH_INTERVAL=2

: "${ECR_URI:?ECR_URI 환경변수가 필요합니다}"
: "${IMAGE_TAG:?IMAGE_TAG 환경변수가 필요합니다}"
export ECR_URI IMAGE_TAG

# 현재·직전 배포 이미지를 보존하고 해당 ECR 저장소의 나머지 미사용 태그만 삭제한다.
cleanup_unused_images() {
    local images image_ref image_id containers
    if ! images=$(docker image ls --no-trunc --format '{{.Repository}}:{{.Tag}} {{.ID}}' "$ECR_URI"); then
        return 1
    fi

    while read -r image_ref image_id; do
        [ -n "$image_ref" ] || continue
        [ "$image_ref" != "$ECR_URI:<none>" ] || continue
        if [[ " $previous_image_ids " == *" $image_id "* ]]; then
            continue
        fi
        if ! containers=$(docker ps -aq --filter "ancestor=$image_id"); then
            return 1
        fi
        [ -z "$containers" ] || continue
        # 강제 삭제를 사용하지 않아 다른 컨테이너가 참조하는 이미지를 보호한다.
        if ! docker image rm "$image_ref"; then
            return 1
        fi
    done <<< "$images"
}

echo "[deploy] logging in to ECR"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "${ECR_URI%%/*}"

CURRENT_COLOR="$(cat "$ACTIVE_COLOR_FILE" 2>/dev/null || echo blue)"
if [ "$CURRENT_COLOR" = "blue" ]; then
    TARGET_COLOR="green"
    TARGET_PORT=8081
else
    TARGET_COLOR="blue"
    TARGET_PORT=8080
fi

echo "[deploy] current=$CURRENT_COLOR target=$TARGET_COLOR image=${ECR_URI}:${IMAGE_TAG}"

cd "$DOCKER_DIR"

echo "[deploy] pulling image"
docker compose -f "docker-compose.$TARGET_COLOR.yml" pull

echo "[deploy] starting $TARGET_COLOR"
docker compose -f "docker-compose.$TARGET_COLOR.yml" up -d

echo "[deploy] health check on port $TARGET_PORT"
healthy=false
for i in $(seq 1 "$HEALTH_RETRY"); do
    if curl -sf "http://localhost:${TARGET_PORT}/actuator/health" | grep -q '"status":"UP"'; then
        healthy=true
        break
    fi
    sleep "$HEALTH_INTERVAL"
done

if [ "$healthy" != "true" ]; then
    echo "[deploy] health check FAILED — rolling back, $CURRENT_COLOR stays live"
    docker compose -f "docker-compose.$TARGET_COLOR.yml" down
    exit 1
fi

echo "[deploy] health check OK — switching nginx to $TARGET_COLOR"
sudo ln -sfn "$NGINX_BLUE_GREEN_DIR/upstream-$TARGET_COLOR.conf" "$NGINX_ACTIVE_LINK"
sudo nginx -t
sudo systemctl reload nginx

echo "[deploy] stopping old $CURRENT_COLOR"
# 종료 전에 직전 배포의 이미지 ID를 기록한다. 조회 실패 시 정리를 생략한다.
cleanup_ready=true
previous_image_ids=""
if previous_images=$(docker compose -f "docker-compose.$CURRENT_COLOR.yml" images -q); then
    while read -r previous_image; do
        [ -n "$previous_image" ] || continue
        if image_id=$(docker image inspect --format '{{.Id}}' "$previous_image"); then
            previous_image_ids+=" $image_id "
        else
            cleanup_ready=false
        fi
    done <<< "$previous_images"
else
    cleanup_ready=false
fi
docker compose -f "docker-compose.$CURRENT_COLOR.yml" down

echo "$TARGET_COLOR" > "$ACTIVE_COLOR_FILE"

echo "[deploy] cleaning unused application images (keeping previous deployment)"
if [ "$cleanup_ready" != "true" ]; then
    echo "[deploy] WARNING: previous image lookup failed; skipping image cleanup" >&2
elif ! cleanup_unused_images; then
    echo "[deploy] WARNING: image cleanup failed; deployment remains successful" >&2
fi
echo "[deploy] done — live color is now $TARGET_COLOR"
