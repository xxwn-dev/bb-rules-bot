# Oracle Cloud Free Tier 배포 가이드 (Docker)

## 체크리스트

### 1. Oracle Cloud 인스턴스 생성
- [ ] ARM A1 Ampere 인스턴스 생성 (Always Free)
- [ ] OS: Ubuntu 22.04
- [ ] OCPU 1개, RAM 6GB 설정
- [ ] SSH 키 생성 및 등록
- [ ] 인스턴스 Running 상태 확인

### 2. 포트 오픈
- [ ] Oracle Cloud 콘솔 → Security List → 8080 인바운드 규칙 추가
- [ ] 서버 내 ufw 허용: `sudo ufw allow 8080`

### 3. 서버 Docker 설치
- [ ] Docker 설치 및 서비스 활성화
- [ ] 현재 유저를 docker 그룹에 추가
```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

### 4. Dockerfile 작성
- [x] 프로젝트 루트에 `Dockerfile` 추가
- [ ] 로컬에서 빌드 확인: `./gradlew build`

### 5. GitHub Actions CI/CD 구성
- [x] `.github/workflows/deploy.yml` 작성
- [ ] GitHub Repository Secrets 등록 (Settings → Secrets and variables → Actions)
  - [ ] `OCI_HOST` - OCI 콘솔 → 인스턴스 상세 → 공인 IP
  - [ ] `OCI_USER` - Ubuntu 이미지면 `ubuntu` 고정
  - [ ] `OCI_SSH_KEY` - 인스턴스 생성 시 다운로드한 프라이빗 키 파일 전체 내용
  - `GITHUB_TOKEN`은 GitHub이 자동 제공하므로 등록 불필요
- [ ] main push 시 워크플로우 정상 동작 확인

### 6. 환경변수 설정
- [ ] 서버에 `.env` 파일 생성
```
DB_URL=jdbc:postgresql://<neon-host>/neondb?sslmode=require&currentSchema=bb_rules
DB_USERNAME=
DB_PASSWORD=
AI_API_KEY=
AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/
DISCORD_BOT_TOKEN=
INGESTION_SECRET=
```
- [ ] `.env` 파일 권한 설정: `chmod 600 .env`

### 7. 첫 배포 및 검증
- [ ] 컨테이너 정상 기동 확인: `docker ps`
- [ ] 로그 확인: `docker logs -f <container>`
- [ ] Discord에서 봇 멘션 응답 확인
- [ ] `POST /api/ingestion/trigger` 동작 확인

---

## 배포 흐름

```
git push
    → GitHub Actions
    → ./gradlew build (jar 생성)
    → Docker 이미지 빌드
    → GHCR에 이미지 push
    → SSH로 Oracle Cloud 서버 접속
    → 새 이미지 pull
    → 기존 컨테이너 중지 → 새 컨테이너 실행 (--env-file .env)
```
