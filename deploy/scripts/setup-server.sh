#!/usr/bin/env bash
#
# Pallang 운영 서버 초기 세팅 스크립트 (Ubuntu 22.04/24.04 LTS 기준).
# Docker Engine + Compose 플러그인, Nginx, Certbot, UFW 방화벽을 설치/구성한다.
#
# 사용법:
#   sudo ./deploy/scripts/setup-server.sh [domain] [email]
#
#   domain, email을 넘기면 Nginx 사이트 등록 + certbot 인증서 발급까지 자동으로 수행한다.
#   생략하면 패키지 설치와 방화벽 설정만 하고, Nginx/인증서 발급은 안내만 출력한다.
#
# 이 스크립트는 저장소를 클론한 뒤 저장소 루트에서 실행한다고 가정한다
# (deploy/nginx/pallang.conf.template 경로를 상대경로로 참조).

set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${2:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [[ "${EUID}" -ne 0 ]]; then
    echo "root 권한(sudo)으로 실행해야 합니다." >&2
    exit 1
fi

log() { echo -e "\n\033[1;32m==> $*\033[0m"; }

log "패키지 목록 갱신"
apt-get update -y
apt-get upgrade -y

log "기본 유틸리티 설치"
apt-get install -y ca-certificates curl gnupg ufw

# ---------------------------------------------------------------------------
# Docker Engine + Compose 플러그인 (공식 apt 저장소)
# ---------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
    log "Docker 공식 GPG 키 및 저장소 등록"
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc

    UBUNTU_CODENAME="$(. /etc/os-release && echo "${VERSION_CODENAME}")"
    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${UBUNTU_CODENAME} stable" \
        > /etc/apt/sources.list.d/docker.list

    apt-get update -y

    log "Docker Engine + Compose 플러그인 설치"
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    systemctl enable --now docker
else
    log "Docker가 이미 설치되어 있습니다 (건너뜀)"
fi

if [[ -n "${SUDO_USER:-}" ]]; then
    log "현재 사용자(${SUDO_USER})를 docker 그룹에 추가 (재로그인 후 sudo 없이 docker 사용 가능)"
    usermod -aG docker "${SUDO_USER}"
fi

# ---------------------------------------------------------------------------
# Nginx
# ---------------------------------------------------------------------------
if ! command -v nginx >/dev/null 2>&1; then
    log "Nginx 설치"
    apt-get install -y nginx
    systemctl enable --now nginx
else
    log "Nginx가 이미 설치되어 있습니다 (건너뜀)"
fi

# ---------------------------------------------------------------------------
# Certbot (Nginx 플러그인 포함)
# ---------------------------------------------------------------------------
if ! command -v certbot >/dev/null 2>&1; then
    log "Certbot 설치"
    apt-get install -y certbot python3-certbot-nginx
else
    log "Certbot이 이미 설치되어 있습니다 (건너뜀)"
fi

# ---------------------------------------------------------------------------
# 방화벽 (UFW) — SSH, HTTP/HTTPS만 허용
# ---------------------------------------------------------------------------
# sshd가 22번이 아닌 포트로 설정돼 있을 수 있으므로, 'OpenSSH' 프로필(22/tcp) 대신
# 실제 리스닝 포트를 확인해서 허용한다. 안 그러면 ufw enable 직후 SSH 접속이 끊길 수 있다.
SSH_PORT="22"
if command -v sshd >/dev/null 2>&1; then
    DETECTED_PORT="$(sshd -T 2>/dev/null | awk '/^port /{print $2; exit}')"
    if [[ -n "${DETECTED_PORT}" ]]; then
        SSH_PORT="${DETECTED_PORT}"
    fi
fi

log "UFW 방화벽 설정 (SSH ${SSH_PORT}/tcp, Nginx Full만 허용)"
ufw allow "${SSH_PORT}/tcp"
ufw allow 'Nginx Full'
ufw --force enable
ufw status verbose

# ---------------------------------------------------------------------------
# Nginx 사이트 등록 + SSL 발급 (domain/email이 주어진 경우에만)
# ---------------------------------------------------------------------------
if [[ -n "${DOMAIN}" && -n "${EMAIL}" ]]; then
    # sed 치환식에 그대로 끼워 넣으므로, 호스트명 형식이 아니면 특수문자를 통한
    # sed 명령 주입/치환 오염을 막기 위해 미리 형식을 검증한다.
    if ! [[ "${DOMAIN}" =~ ^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$ ]]; then
        echo "유효하지 않은 도메인 형식입니다: ${DOMAIN}" >&2
        exit 1
    fi

    log "Nginx 사이트(${DOMAIN}) 등록"
    sed "s/__DOMAIN__/${DOMAIN}/g" "${REPO_ROOT}/deploy/nginx/pallang.conf.template" \
        > "/etc/nginx/sites-available/pallang"
    ln -sf /etc/nginx/sites-available/pallang /etc/nginx/sites-enabled/pallang
    rm -f /etc/nginx/sites-enabled/default
    nginx -t
    systemctl reload nginx

    log "Certbot으로 SSL 인증서 발급 및 자동 갱신 등록 (${DOMAIN})"
    certbot --nginx -d "${DOMAIN}" -m "${EMAIL}" --agree-tos --redirect --non-interactive
    systemctl enable --now certbot.timer
else
    log "domain/email 인자가 없어 Nginx 사이트 등록과 인증서 발급은 건너뜁니다."
    echo "필요 시 다음을 수동으로 실행하세요:"
    echo "  sudo ${SCRIPT_DIR}/setup-server.sh <domain> <email>"
fi

log "서버 초기 세팅 완료. 다음 단계:"
cat <<EOF
  1. 저장소를 원하는 경로로 클론 (이미 되어 있다면 생략)
  2. cp deploy/.env.example .env  후 값 채우기
  3. docker compose up -d --build
  4. curl -I http://127.0.0.1:8080  로 컨테이너 헬스체크
  5. (미실행 시) sudo ${SCRIPT_DIR}/setup-server.sh <domain> <email> 로 Nginx/SSL 구성
EOF
