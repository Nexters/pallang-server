---
name: swagger-docs
description: Use whenever writing or reviewing Swagger/OpenAPI annotations (@Schema, SecurityScheme) on request/response DTOs in this repository, or when a frontend report says a field's required/optional-ness or a security scheme doesn't match reality. Captures springdoc-openapi behavior discovered while fixing issue #48 that isn't obvious from the springdoc docs.
---

# Swagger/OpenAPI 작성 시 주의사항

이슈 #48(FE의 orval 코드젠 실패, 스펙 위반 리포트)을 고치면서 확인된, springdoc-openapi 기본 동작 중 직관과 다른 부분을 정리합니다.

## 1. springdoc은 primitive 타입도 자동으로 required 처리하지 않는다

**틀리기 쉬운 가정:** "`int`/`long`/`boolean`은 null이 될 수 없으니 springdoc이 알아서 required로 표시해주겠지."

**실제로는 아니다.** `int pageCount`, `long opinionCount`, `boolean isDeleted` 같은 필드도 `@Schema`에 `requiredMode`를 명시하지 않으면 생성된 스펙의 `required` 배열에서 빠진다. 이 프로젝트에서 실제로 `PageInfo`, `LoginResponse`, `OpinionResponse` 등 거의 모든 응답 DTO가 이 문제를 겪고 있었다.

**결론: 응답 DTO에서 백엔드가 항상 값을 채워주는 필드는 타입(primitive/객체 불문)에 관계없이 전부 명시적으로 표시한다.**

```java
// 항상 채워지는 필드
@Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
@Schema(example = "5", requiredMode = Schema.RequiredMode.REQUIRED) int likeCount,
@Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean isDeleted,

// DB 컬럼이 nullable이거나, 비즈니스 로직상 실제로 null/미제공일 수 있는 필드
@Schema(example = "...", nullable = true) String profileImageUrl,
```

리스트 필드(`List<T>`)나 중첩 객체 필드(`PageInfo` 등)도 항상 채워진다면 동일하게 `requiredMode = REQUIRED`를 붙인다. `@ArraySchema`를 쓰는 원시 타입 리스트(`List<Integer>` 등)는 `arraySchema` 속성에 붙여야 한다:

```java
@ArraySchema(schema = @Schema(example = "3"), arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
List<Integer> pageNumbers,
```

## 2. 응답 DTO를 전수조사할 때 파일 경로 패턴을 하나로 단정하지 않는다

대부분의 응답 DTO는 `domain/**/presentation/dto/*Response.java`에 있지만, 예외가 있다(예: `domain/passage/presentation/response/PassageResponse.java` — 내부에 여러 record가 중첩된 형태). 경로/네이밍 패턴 하나만 보고 전수조사했다고 판단하지 말고, 다음처럼 실제 컨트롤러가 반환하는 타입을 기준으로 넓게 검색한다:

```bash
grep -rln "@Schema" src/main/java --include="*.java" | grep -i "presentation"
```

## 3. SecurityScheme: `type: http`에는 `.name()`을 붙이지 않는다

`name` 속성은 OpenAPI 3.0 스펙상 `type: apiKey`에서만 허용된다. JWT Bearer 토큰처럼 `type: http` + `scheme: bearer`를 쓰는 경우 `.name(...)`을 호출하면 스펙 위반이 되어 프론트 코드젠 도구(orval 등)의 스펙 검증이 실패한다.

```java
// 올바른 예 (SwaggerConfig)
new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");
        // .name(...) 금지 — apiKey 타입 전용
```

## 4. 가정하지 말고 `/v3/api-docs`로 직접 검증한다

springdoc의 기본 동작(무엇을 자동으로 required 처리하는지, nullable을 어떻게 반영하는지 등)은 문서만 보고 단정하면 틀리기 쉽다. 변경 후에는 로컬로 앱을 띄워 실제 생성된 스펙을 확인한다:

```bash
./gradlew bootRun --args='--spring.profiles.active=local' &
curl -s http://localhost:8080/v3/api-docs > api-docs.json
# 이후 node/jq 등으로 components.schemas.<이름>.required, components.securitySchemes 확인
```
