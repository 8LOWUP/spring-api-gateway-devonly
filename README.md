# Spring API Gateway (Dev Only)

이 프로젝트는 **개발 서버 환경 전용 API Gateway**입니다.

여러 마이크로서비스(member-service, mcp-service, workspace-service)로 들어오는 요청을 단일 진입점으로 집약해 라우팅합니다.

운영 환경이 아닌 **단순 개발/테스트 목적**으로 제작되었습니다.

---

## 기능

- **마이크로서비스 라우팅**
    - 서비스별 접두 경로(prefix)를 붙여 API 호출 가능
    - 예:
        - http://{gateway}/member/** → member-service
        - http://{gateway}/mcp/** → mcp-service
        - http://{gateway}/workspace/** → workspace-service
- **Swagger 통합 문서 (개발 편의용)**
    - 각 서비스의 Swagger(OpenAPI 3) 문서를 API Gateway를 통해 접근 가능
    - 게이트웨이에서 **간단한 Swagger Aggregator 기능**을 제공하여, 하나의 Swagger UI에서 모든 API를 확인할 수 있음
    - 단, 운영 환경에서는 권장하지 않음 (게이트웨이의 책임 분리 필요)

---

## 서비스 라우팅 구조

예시: mcp-service

- 실제 MCP 애플리케이션 컨트롤러에 /mcp prefix가 이미 존재
- Gateway도 /mcp로 라우팅하기 때문에, 최종 접근 경로는 /mcp/mcp 형태가 됨
- 프론트엔드 접근 시 유의 필요

| 서비스명 | 게이트웨이 경로 | 컨테이너명 | 내부 포트 |
| --- | --- | --- | --- |
| member-service | /member/** | member | 8081 |
| mcp-service | /mcp/** | mcp | 8081 |
| workspace-service | /workspace/** | workspace | 8081 |

---

## ⚙설정

서비스 라우팅 정보 및 포트 설정은 application.yml에서 관리합니다.

🔗 [application.yml 바로가기](https://github.com/8LOWUP/spring-api-gateway-devonly/blob/main/src/main/resources/application.yml)

만약 컨테이너명 또는 내부 포트를 변경할 경우, 위 설정 파일을 수정해야 합니다.

---

## Swagger 사용 방법

1. 각 마이크로서비스는 기본적으로 /v3/api-docs, /swagger-ui.html 엔드포인트를 노출합니다.
2. 게이트웨이에서 각 서비스 Swagger를 프록시 경로로 접근할 수 있습니다.
    - http://{gateway}/member/swagger-ui.html
    - http://{gateway}/mcp/swagger-ui.html
    - http://{gateway}/workspace/swagger-ui.html
3. 또한, 게이트웨이에 **Swagger Aggregator**가 포함되어 있어 다음 주소에서 전체 API를 한 화면에서 확인 가능합니다.
    - http://{gateway}/swagger-ui.html

> ⚠️⚠️⚠️ 주의: 이 Aggregator는 개발 서버 전용 기능입니다. 운영 환경에서는 반드시 독립적인 API 문서 서버를 두는 것을 권장합니다.

---

## 🛠️ 개발 서버 배포 방식

- VM 환경에서 각 서비스가 **독립된 컨테이너**로 실행됨
- API Gateway도 동일하게 컨테이너로 실행되며, 다른 서비스와 동일 네트워크 내에서 통신

---

## 📌 참고

- Spring Cloud Gateway 기반
- Swagger 통합은 springdoc-openapi 사용
- 개발 서버 전용이므로 운영 환경에서는 사용하지 말 것
