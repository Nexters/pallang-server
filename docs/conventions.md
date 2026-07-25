# JAVA code 컨벤션

---

## 1. 명명 규칙

변수, 메서드, 클래스 등에는 일관된 명명 규칙을 적용해야한다.

명확하고 의미 있는 이름은 코드를 읽기 쉽게 만들며 거기에 규칙성이 더해지면 팀 전체의 생산성에 도움을 줄 수 있다.

위와 같은 이유로 되도록이면 변수, 메서드, 클래스의 명칭은 의미를 알 수 없는 축약어를 사용하지 않고 풀어서 작성하도록 한다.

## 1-1. Layer 별 메서드

> Layer에서 사용되는 메서드 명명 규칙이며 예제는 “User” 로 작성합니다.
reference: 파트너플랫폼 스쿼드 코드 컨벤션
>

| Method | Controller and Service | Repository |
| --- | --- | --- |
| Data Read | getUser | findByUser<br>countByUser<br>existsByUser |
| Data Insert | createUser | insertUser |
| Data Delete | removeUser | deleteUser |
| Data Modify | modifyUser | updateUser |
| Event 발행 | UserEvent | - |

**Controller ~ Service Layer의 메서드 명칭**과 **Repository Layer의 메서드 명칭**을 **다르게 작성**하여, 비즈니스 로직이 동작하는 메서드인지, 저장소에 Accesss 하기위한 기능을 제공하는 메서드인지 구별할 수 있도록 합니다.

ex) 기능을 명확히 설명할 수 있다면 get, create, remove를 사용하지 않아도 됨

```java
// UserController
@PostMapping
public ResponseDto<Users> signInUser(
		@RequestBody UserRequset.SignInUserDto request
) {
		userService.signInUser(request);
		...
}

// UserService
public void signInUser(
		UserRequset.SignInUserDto request
) {
		userWriteRepository.insertUser(request);
		...
}

// UserWriteRepository
void insertUser(request)

```

## 1-2. Test 규칙

### 1-2-1. DisplayName Annotation

> Junit 5에서 나온 DisplayName annotation 활용
>
- 명사의 나열보다 문장형으로 작성

  > A이면 B이다. 또는
  >
  >
  > A이면 B가 아니고 C다.
  >
    - "~테스트" 지양하기
    - 음료 1개 추가 테스트 : 테스트의 대한 설명이 명확하게 끝나지 않음
    - 음료를 1개 추가할 수 있다. : 설명이 명확하게 끝남
- 테스트 행위에 대한 결과까지 기술하기
    - 음료를 추가할 수 있다.
    - 음료를 추가하면 `주문 목록에 담긴다.`
- 도메인 용어를 사용하기

  > 메서드 자체의 관점보다 도메인 정책 관점으로
  >
    - 특정 시간 이전에 주문을 생성하면 실패한다.
    - `영업 시작 시간` 이전에는 주문을 생성할 수 없다.

### 1-2-2. BDD 스타일 테스트

- BDD란?
    - TDD에서 파생된 개발 방법
    - 함수 단위의 테스트에 집중하기 보다, 시나리오에 기반한 `테스트케이스(TC)` 자체에 집중하여 테스트를 진행
    - 개발자가 아닌 사람이 봐도 이해할 수 있을 정도의 추상화 수준(레벨)을 권장
    - 테스트 자체가 문서의 역할을 할 수 있도록 해야 함을 의미
- Given / When / Then
    - Given : 어떤 환경에서
        - 시나리오 진행에 필요한 모든 준비 과정 (객체, 값, 상태 등)
    - When : 어떤 행동을 진행했을 때
        - 시나리오 행동 진행
    - Then : 어떤 상태 변화가 일어난다
        - 시나리오 진행에 대한 결과 명시, 검증

## 1-3. 요청/응답 객체 필드명

### 1-3-1. 공통(요청/응답)

- 필드명은 DB에서 사용하는 이름을 기준으로 합니다.
    - Ticket Table - status → status
    - Member Table - nickname → nickname
- 요청/응답 객체 내에 동일한 필드를 사용하는 경우, 필드 앞에 식별자(권한 등)를 붙입니다.
    - Member.nickname (UI에 사용자와 담당자의 닉네임이 모두 표시됨)
      → userNickname, managerNickname
- PK는 반드시 도메인명을 붙입니다.
    - Ticket.id → ticketId

### 1-3-2. 요청 객체

- URL에 요청하고자 하는 대상 도메인이 명시되는 경우, 도메인명을 생략합니다. 단, PK는 예외로 합니다.
    - POST /api/user/**tickets**

        ```json
        {
        	"title": "string",  // ticketTitle로 받지 않음
        	"content": "string"
        }
        ```

    - Get /api/user/**tickets/{ticketId}**

### 1-3-3. 응답 객체

- 특정 엔티티의 필드값을 반환하는 경우, (확장성을 고려하여) PK를 함께 반환합니다.
    - UI 상 담당자의 닉네임만 요구되는 경우에도 담당자의 PK를 함께 반환
- List의 경우 필드명 : {도메인명}s (필드명에 자료형 명시 x)
- 중첩된 객체의 속성인 경우, 엔티티 이름을 생략합니다. 단, PK는 예외로 합니다.
    - Ticket 객체를 리스트에 담아 반환하는 경우

        ```json
        {
        	"tickets" : [
        		{
        			"ticketId": "string",
        			"title": "string"
        		}, ...
        	]
        }
        ```

    - List의 경우 필드명 : {도메인명}s (필드명에 자료형 명시 X)

        ```json
        {
        	"tickets" : [
        		{
        			"ticketId": "string",
        			"title": "string"
        		}, ...
        	]
        }
        ```


### 1-3-4. 응답 필드 값

- Enum값은 자료형 그대로 반환합니다. (Enum의 `.name()`값 )

    ```java
    public record TicketExampleResponse(
    	Long ticketId,
    	**TicketStatus status**
    ) {}
    ```


### 1-3-5 페이지네이션 파라미터 이름

- 페이지 번호 : page
- 페이지 크기 : size
- 정렬 기준 : sortType

# 디렉토리 구조(도메인형)

User

├── presentation        // Controller 등 API 요청 처리 , dto
├── application         // Service 등 비즈니스 로직
├── domain              // Entity, VO
├── infrastructure      // JPA Repository, 외부 연동
├── common              // 공통 설정, 예외 처리

댓글

├── presentation        // Controller 등 API 요청 처리 , dto
├── application         // Service 등 비즈니스 로직
├── domain              // Entity, VO
├── infrastructure      // JPA Repository, 외부 연동
├── common              // 공통 설정, 예외 처리

**.gitkeep**

!image.png

- 후보 2

    ```
    ├─domains
    │  └─[도메인명]
    │     ├─application
    │     │  ├─dto
    │     │  │  ├─request
    │     │  │  └─response
    │     │  └─mapper
    │     ├─domain
    │     │  ├─constant
    │     │  └─service
    │     ├─exception
    │     ├─persistence
    │     │  ├─entity
    │     │  ├─mapper
    │     │  └─repository
    │     └─presentation 
    |        |_ controller
    |   |_[도메인명]
    │     ├─application
    │     │  ├─dto
    │     │  │  ├─request
    │     │  │  └─response
    │     │  └─mapper
    │     ├─domain
    │     │  ├─constant
    │     │  └─service
    │     ├─exception
    │     ├─persistence
    │     │  ├─entity
    │     │  ├─mapper
    │     │  └─repository
    │     └─presentation 
    |        |_ controller
    |   |__[도메인명]
    │     ├─application
    │     │  ├─dto
    │     │  │  ├─request
    │     │  │  └─response
    │     │  └─mapper
    │     ├─domain
    │     │  ├─constant
    │     │  └─service
    │     ├─exception
    │     ├─persistence
    │     │  ├─entity
    │     │  ├─mapper
    │     │  └─repository
    │     └─presentation 
    |        |_ controller
    ├─global
    │  ├─exception
    │  ├─response
    │  │  └─code   
    |  ├─utils
    │  └─config
    └─infrastructure
    ```


**1️⃣ domains (하위 개별 도메인 관련 aggregate)**

> **[도메인명]**
>
>
> ex). `user`,  `ticket` …
>
- **application**
    - **dto**: 계층 간 데이터 전송을 위한 객체들 (request / response 구분)
    - **mapper**: 객체 간의 변환 로직 (ex. DTO ↔ Entity) (ex. Converter)
- **domain**
    - **constants**: 상수 클래스(ex. Enum)
    - **service**: repository 계층에 의존하며, 여러 가지의 비즈니스 로직을 제공
- **exception**: 도메인에서 발생할 수 있는 커스텀 예외 정의
- **persistence**: 도메인의 데이터 영속성 계층에 초점, 데이터베이스 및 영속 계층 관련 구현체를 포함
    - **entity**: 데이터베이스와 직접적으로 매핑되는 JPA 엔티티 클래스
        - 영속성 계층에서 사용하는 도메인 객체로, 데이터베이스 테이블과 1:1 매핑
        - DB 관련 필드 및 설정 포함
    - **repository**: 데이터베이스와의 상호작용을 처리하는 Repository 구현체와 관련된 코드를 포함
        - 쿼리를 작성하거나 JPA 커스텀 메서드를 추가해 비즈니스 요구사항에 맞는 데이터 접근을 처리
- **presentation**: REST API 또는 사용자 인터페이스 담당, usecase를 호출하여 응답 반환 (ex. Controller)
- **(선택적) infra**: 해당 도메인 내에서만 사용하는 외부 시스템과의 통합 기능 (ex. DB 이외의 외부 시스템 접근)

**2️⃣ global**

```json
├─global
│  ├─config             # 환경 설정 (Spring, DB, Swagger 등)
│  ├─exception          # 공통 예외 처리 (CustomException, GlobalExceptionHandler)
│  ├─response           # 공통 응답 규격 (ApiResponse, Message)
│  ├─security           # Spring Security 설정 및 인증 로직
│  │  └─jwt             # JWT 관련 (Provider, Filter, EntryPoint)
│  ├─util               # 공통 유틸리티 (Date, String, Encryption)
│  └─common             # 공통 상수(Constant)나 베이스 엔티티(BaseEntity 등)
```

- 패스

  **3️⃣ infrastructure (aws s3)**

    - 외부 계층 관련 기능

      eg. 외부 DB, 외부 API 서버 등


    **4️⃣ utils**
    
    - helper, parser 등의 부가 기능

# ⚠️  커스텀 에러 코드

---

아래와 같은 형식에 따라 세부 도메인별로 CustomErrorCode를 작성하여 사용합니다.

트레일링 콤마(마지막 `,`)를 사용하여 새로운 예외 추가 시, 추가된 부분만 변경 부분으로 인식될 수 있도록 합니다.

```java
package com.wrkr.tickety.domains.ticket.exception;

@Getter
@AllArgsConstructor
public enum DomainErrorCode implements BaseErrorCode {

    DOMAIN_STATUS(HttpStatus.STATUS, "Domain_x00_n", "message"),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
```

| domain | 관련 도메인(Entity 단위) 이름 |
| --- | --- |
| status | 동일한 status에 대해 여러 enum값이 존재할 경우는 name에 의미를 명시 |
| x | HTTP Status code의 마지막 숫자 |
| n | 동일한 x에 대한 error code 추가 순서. 1로 시작 |
| message | ‘입니다.’ 체로 작성 |

```jsx
package com.wrkr.tickety.domains.ticket.exception;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.STATUS, "MEMBER_400_1", "message"),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
```

| **에러 코드** | **HTTP 상태** | **메시지** |
| --- | --- | --- |
| **MEMBER_400_1** | 400 BAD_REQUEST | "아이디는 필수 입력값입니다." |
| **MEMBER_400_2** | 400 BAD_REQUEST | "이미 사용 중인 아이디입니다." |
| **MEMBER_404_1** | 404 NOT_FOUND | "해당 회원을 찾을 수 없습니다." |
| **AUTH_401_1** | 401 UNAUTHORIZED | "토큰이 만료되었습니다." |
| **AUTH_401_2** | 401 UNAUTHORIZED | "잘못된 토큰 형식입니다." |

### 🌟 Swagger 명시

---

컨트롤러에는 다음과 같이 해당 API에서 발생할 수 있는 예외를 명시하여 Swagger에서 한 번에 확인할 수 있도록 합니다.

[[Spring Boot] Swagger의 운영 코드 침투를 막아라 (feat. Springdoc)](https://jaeseo0519.tistory.com/406)

```java
@Tag(name = "User", description = "유저 API")
public interface UserApi {

    @Operation(
            summary = "유저 이름 조회",
            description = "요청 유저의 이름을 조회합니다."
    )
    @ApiResponseExplanations(
            success = @ApiSuccessResponseExplanation(
                    responseClass = NameResponse.class,
                    description = "조회 성공"
            )
    )
    ResponseEntity<ApiResponse<NameResponse>> getName(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user
    );
    
    @Operation(
            summary = "유저 이름 수정",
            description = "요청 유저의 이름을 수정합니다." +
                    "<br>제약사항 : 1~10자, 공백 없는 한영숫자만 가능(^[a-zA-Z0-9가-힣]*$)"
    )
    @ApiResponseExplanations(
            success = @ApiSuccessResponseExplanation(
                    description = "수정 성공"
            )
    )
    ResponseEntity<ApiResponse<Void>> updateName(
            @RequestBody @Valid NameRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user
    );
```

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserUseCase userUseCase;
    private final JwtProvider jwtProvider;

    @Override
    @GetMapping("/name")
    public ResponseEntity<ApiResponse<NameResponse>> getName(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        NameResponse response = userUseCase.getName(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(CommonResponseCode.OK, response));
    }

    @Override
    @PatchMapping("/name")
    public ResponseEntity<ApiResponse<Void>> updateName(
            @RequestBody @Valid NameRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        userUseCase.updateName(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ofSuccess(CommonResponseCode.OK));
    }
```

### 🌐 HTTP Status

---

HttpStatus를 아래와 같은 기준에 따라 설정합니다.

아래를 제외한 HttpStatus는 사용하지 않습니다.

| code | **의미** | **예시** |
| --- | --- | --- |
| 400 | `BAD_REQUEST`<br>잘못된 요청이나 문법 | ticketId로 빈 문자열로 요청함 |
| 401 | `UNAUTHORIZED`<br>인증되지 않은 접근 | JWT가 없이 요청함 |
| 403 | `FORBIDDEN`<br>권한 문제 | 다른 사용자가 `요청`한 `티켓`의 ticketId로 요청함 |
| 404 | `NOT_FOUND`<br>존재하지 않는 객체 | 요청된 ticketId의 복호화 값을 ticketId로 갖는 `티켓`이 존재하지 않음 |
| 409 | `CONFLICT`<br>현재 상태와 충돌 | 중복되는 `닉네임`으로 `회원 등록`을 요청함<br>현재 `티켓 상태`에서 요청될 수 없는 작업을 요청함 |

### 🔗 Reference

---

HTTP 상태 코드 - HTTP | MDN

[](https://namu.wiki/w/HTTP/%EC%9D%91%EB%8B%B5%20%EC%BD%94%EB%93%9C#s-3.4)