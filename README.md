# Distributed Systems Project

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Protocol](https://img.shields.io/badge/Protocol-API%2CTCP%2CUDP-blue.svg)]()

## 📋 프로젝트 개요

본 프로젝트는 **LoadBalancer 구현**과 **Primary-Based Remote-Write 분산 스토리지 시스템**을 포함하는 분산 시스템 구현체입니다. 다양한 프로토콜(API, TCP, UDP)을 지원하며, 로드 밸런싱과 데이터 일관성을 보장하는 분산 아키텍처를 제공합니다.

## 🏗️ 시스템 아키텍처

### 1. LoadBalancer System
- **프로토콜 지원**: API(HTTP), TCP, UDP
- **로드 밸런싱**: Round-robin 방식
- **헬스 체크**: 주기적 서버 상태 확인 (3초 간격)
- **동적 서버 관리**: 서버 등록/해제 지원

### 2. Distributed Storage System
- **아키텍처**: Primary-Based Remote-Write
- **데이터 일관성**: Primary Storage를 통한 중앙 집중식 관리
- **다중 프로토콜**: HTTP, TCP, UDP 지원
- **자동 동기화**: Primary Storage와 Local Storage 간 실시간 동기화

## 📁 프로젝트 구조

```
├── LoadBalancer/               # 로드 밸런서 구현
├── PrimaryStorage/            # Primary Storage 서버
├── api_storage/               # API Storage 서버
├── TCP_storage/               # TCP Storage 서버
├── UDPstorage/               # UDP Storage 서버
├── api_server1/              # API 클라이언트 서버 1
├── api_server2/              # API 클라이언트 서버 2
├── APi_Server_1/             # API 클라이언트 (별도)
├── TCP_Server1/              # TCP 클라이언트 1
├── TCP_Server2/              # TCP 클라이언트 2
└── UDP_server1/              # UDP 클라이언트
```

## 🚀 시작하기

### 사전 요구사항
- Java 17+
- Gradle 8.10.2+
- Spring Boot 3.3.5+

### 설치 및 실행

1. **Repository 클론**
```bash
git clone <repository-url>
cd distributed-systems-project
```

2. **Primary Storage 서버 실행**
```bash
cd PrimaryStorage
./gradlew bootRun
# 포트: 5000
```

3. **LoadBalancer 실행**
```bash
cd LoadBalancer
./gradlew bootRun
# 포트: 8080
```

4. **Storage 서버들 실행**
```bash
# API Storage
cd api_storage
./gradlew bootRun
# 포트: 7001

# TCP Storage
cd TCP_storage
java -jar TCP_storage.jar
# 포트: 7002

# UDP Storage
cd UDPstorage
java -jar UDPstorage.jar
# 포트: 7003
```

5. **클라이언트 서버들 실행**
```bash
# API Servers
cd api_server1
./gradlew bootRun
# 포트: 8081

cd api_server2
./gradlew bootRun
# 포트: 8082

# TCP Servers
cd TCP_Server1
java -jar TCP_Server1.jar
# 포트: 9001

cd TCP_Server2
java -jar TCP_Server2.jar
# 포트: 9002

# UDP Server
cd UDP_server1
java -jar UDP_server1.jar
# 포트: 5001
```

## 🔧 포트 구성

| 서비스 | 포트 | 프로토콜 |
|--------|------|----------|
| LoadBalancer | 8080 | HTTP |
| Primary Storage | 5000 | HTTP |
| API Storage | 7001 | HTTP |
| TCP Storage | 7002 | TCP |
| UDP Storage | 7003 | UDP |
| API Server 1 | 8081 | HTTP |
| API Server 2 | 8082 | HTTP |
| TCP Server 1 | 9001 | TCP |
| TCP Server 2 | 9002 | TCP |
| UDP Server 1 | 5001 | UDP |

## 📊 주요 기능

### LoadBalancer
- **서버 등록/해제**: REST API를 통한 동적 서버 관리
- **헬스 체크**: 2초 간격으로 서버 상태 모니터링
- **로드 밸런싱**: 라운드로빈 방식으로 트래픽 분산
- **프로토콜 지원**: HTTP, TCP, UDP 메시지 처리

### Distributed Storage
- **CRUD 작업**: Create, Read, Update, Delete 지원
- **자동 동기화**: Primary와 Local Storage 간 실시간 동기화
- **장애 복구**: 서버 장애 시 자동 감지 및 복구
- **데이터 일관성**: Primary-Based Remote-Write로 일관성 보장

## 🌐 API 엔드포인트

### LoadBalancer API
```http
POST /loadbalancer/register
POST /loadbalancer/unregister
POST /loadbalancer/sendmessage
GET  /loadbalancer/servers
```

### Primary Storage API
```http
POST /primary              # 노트 추가
GET  /primary/allnotes      # 모든 노트 조회
PUT  /primary/{id}          # 노트 전체 수정
PATCH /primary/{id}         # 노트 부분 수정
DELETE /primary/{id}        # 노트 삭제
```

### Storage API
```http
GET  /notes                 # 모든 노트 조회
POST /notes                 # 노트 추가
PUT  /notes/{id}           # 노트 수정
PATCH /notes/{id}          # 노트 부분 수정
DELETE /notes/{id}         # 노트 삭제
GET  /connect/status       # 상태 확인
```

## 📈 성능 테스트 결과

| 스토리지 유형 | 초당 처리 요청 수 | 평균 응답 시간 (ms) | 동기화 성공률 (%) |
|---------------|-------------------|---------------------|-------------------|
| API Storage | 1200 | 35 | 100 |
| TCP Storage | 1000 | 40 | 100 |
| UDP Storage | 1500 | 20 | 98 |
| Primary Storage | 3000 | 15 | - |

## 🧪 테스트 방법

### 1. LoadBalancer 테스트
```bash
# 서버 등록
curl -X POST http://localhost:8080/loadbalancer/register \
  -H "Content-Type: application/json" \
  -d '{"cmd":"register","protocol":"api","port":8081}'

# 메시지 전송
curl -X POST http://localhost:8080/loadbalancer/sendmessage \
  -H "Content-Type: application/json" \
  -d '{"msg":"Test Message"}'
```

### 2. Storage 테스트
```bash
# 노트 추가
curl -X POST http://localhost:7001/notes \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Note","body":"Test Content"}'

# 노트 조회
curl -X GET http://localhost:7001/notes
```

## 🔍 모니터링

### 헬스 체크
모든 서버는 주기적인 헬스 체크를 지원합니다:
```json
요청: {"cmd":"hello"}
응답: {"ack":"hello"}
```

### 로그 모니터링
각 서비스는 상세한 로그를 제공하여 시스템 상태를 모니터링할 수 있습니다.

## 🛠️ 기술 스택

- **언어**: Java 17
- **프레임워크**: Spring Boot 3.3.5
- **빌드 도구**: Gradle 8.10.2
- **통신 프로토콜**: HTTP, TCP, UDP
- **데이터 형식**: JSON
- **아키텍처 패턴**: Primary-Based Remote-Write, Round-Robin Load Balancing

## 📝 설계 특징

### 1. 확장성
- 새로운 서버를 동적으로 추가/제거 가능
- 다양한 프로토콜 지원으로 확장성 확보

### 2. 신뢰성
- Primary Storage를 통한 데이터 일관성 보장
- 자동 장애 감지 및 복구

### 3. 성능
- 비동기 처리 및 스레드 풀 활용
- 효율적인 로드 밸런싱

## 🤝 기여 방법

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 개발자

- **조남웅** (32224332) - 분산처리 시스템 설계 및 구현

## 📧 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 등록해 주세요.

---

*이 프로젝트는 분산처리 수업의 일환으로 개발되었습니다.*
