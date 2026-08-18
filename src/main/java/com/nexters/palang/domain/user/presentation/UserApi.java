package com.nexters.palang.domain.user.presentation;

import com.nexters.palang.domain.user.presentation.dto.LikedOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.MeResponse;
import com.nexters.palang.domain.user.presentation.dto.MyOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.MyPassageListResponse;
import com.nexters.palang.domain.user.presentation.dto.OnboardingCompleteResponse;
import com.nexters.palang.domain.user.presentation.dto.UpdateBackgroundColorRequest;
import com.nexters.palang.domain.user.presentation.dto.UpdateNicknameRequest;
import com.nexters.palang.global.common.error.ErrorResponse;
import com.nexters.palang.global.common.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "마이페이지 API")
public interface UserApi {

    @Operation(summary = "내 프로필 조회", description = "닉네임, 이메일, 프로필 이미지, 배경색, 가입 경로(SNS), "
            + "지금까지 남긴 흔적 수를 조회합니다. 이메일은 SNS 이메일 동의 여부에 따라 없을 수 있습니다(null). "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음(탈퇴 포함) (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<MeResponse>> getMe();

    @Operation(summary = "닉네임 변경", description = "최대 15자, 중복 불가, 하루 1회만 변경할 수 있습니다(달력일 기준). "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "닉네임 누락/15자 초과 (COMMON_400_1) "
                    + "또는 오늘 이미 변경함 (USER_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_1", value = "{\"type\":\"/api/users/me/nickname\","
                                    + "\"title\":\"COMMON_400_1\",\"status\":400,"
                                    + "\"detail\":\"닉네임은 15자를 초과할 수 없습니다.\"}"),
                            @ExampleObject(name = "USER_400_1", value = "{\"type\":\"/api/users/me/nickname\","
                                    + "\"title\":\"USER_400_1\",\"status\":400,"
                                    + "\"detail\":\"닉네임은 하루에 한 번만 변경할 수 있습니다.\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/nickname\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음 (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/nickname\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임 (USER_409_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/nickname\",\"title\":\"USER_409_1\","
                                    + "\"status\":409,\"detail\":\"이미 사용 중인 닉네임입니다.\"}")))
    })
    ResponseEntity<DataResponse<MeResponse>> modifyNickname(@Valid UpdateNicknameRequest request);

    @Operation(summary = "배경색 변경", description = "흔적 보기 화면의 배경색을 변경합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "배경색 누락/20자 초과 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/background-color\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"배경색은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/background-color\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음 (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/background-color\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<MeResponse>> modifyBackgroundColor(@Valid UpdateBackgroundColorRequest request);

    @Operation(summary = "프로필 이미지 변경", description = "프로필 이미지를 업로드하여 변경합니다(jpeg/png). "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 파일이 아님 (USER_400_2)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/profile-image\",\"title\":\"USER_400_2\","
                                    + "\"status\":400,\"detail\":\"이미지 파일만 업로드할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/profile-image\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음 (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/profile-image\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<MeResponse>> modifyProfileImage(
            @Parameter(description = "프로필 이미지 파일 (jpeg/png)", required = true) MultipartFile image
    );

    @Operation(summary = "회원 탈퇴", description = "소프트 삭제 처리하고 닉네임을 익명화합니다(다른 이용자의 대화 맥락 유지를 위해 "
            + "공개된 발췌·의견·댓글은 남습니다). 카카오 연동 해제를 함께 시도하나 실패해도 탈퇴 자체는 계속 진행되며(best-effort), "
            + "발급된 모든 리프레시 토큰은 즉시 무효화됩니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음(이미 탈퇴 포함) (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<Void>> withdraw();

    @Operation(summary = "온보딩 완료 처리", description = "로그인 직후 온보딩 4단계를 마치면 호출합니다(FR-AUTH-05). "
            + "이미 완료한 사용자가 다시 호출해도 에러 없이 그대로 완료 상태를 유지합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/onboarding-complete\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음 (USER_404_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/onboarding-complete\",\"title\":\"USER_404_1\","
                                    + "\"status\":404,\"detail\":\"해당 사용자를 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<DataResponse<OnboardingCompleteResponse>> completeOnboarding();

    @Operation(summary = "내가 남긴 흔적 목록", description = "내가 작성한 흔적을 최신순으로 조회합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/opinions\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"'size' 파라미터의 값이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/opinions\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<MyOpinionListResponse>> getMyOpinions(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "좋아요 누른 흔적 목록", description = "내가 좋아요를 누른 흔적을 좋아요 누른 순서대로(최신순) 조회합니다. "
            + "좋아요 취소는 이 API가 아니라 흔적 좋아요 토글 API가 담당합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/likes\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"'size' 파라미터의 값이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/likes\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<LikedOpinionListResponse>> getLikedOpinions(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );

    @Operation(summary = "내 대목 목록", description = "내가 흔적을 남긴 대목을 최신순으로 조회합니다. "
            + "소유 기준은 최초 작성자가 아니라 해당 대목에 흔적을 남긴 사용자이며, 병합된 대목은 흔적을 남긴 모든 사용자에게 노출됩니다. "
            + "bookId를 지정하면 해당 책으로 한정하고, 생략하면 전체 책을 대상으로 합니다. "
            + "spoilerOnly=true면 스포일러 대목만 조회합니다. "
            + "Authorization: Bearer {accessToken} 헤더로 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "page/size 형식 오류 (COMMON_400_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/passages\",\"title\":\"COMMON_400_1\","
                                    + "\"status\":400,\"detail\":\"'size' 파라미터의 값이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 누락 (AUTH_401_1)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"type\":\"/api/users/me/passages\",\"title\":\"AUTH_401_1\","
                                    + "\"status\":401,\"detail\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<DataResponse<MyPassageListResponse>> getMyPassages(
            @Parameter(description = "책 ID (생략 시 전체 책 대상)") Long bookId,
            @Parameter(description = "스포일러 대목만 조회 여부 (기본값 false)") boolean spoilerOnly,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)") int size
    );
}
