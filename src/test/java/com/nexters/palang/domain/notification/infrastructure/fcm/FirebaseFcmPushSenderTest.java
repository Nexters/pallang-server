package com.nexters.palang.domain.notification.infrastructure.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirebaseFcmPushSenderTest {

    @Mock
    private FirebaseApp firebaseApp;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FirebaseFcmPushSender sender;

    @Test
    @DisplayName("모든 토큰 발송에 성공하면 무효 토큰 없이 빈 리스트를 반환한다")
    void returnsEmptyListWhenAllTokensSucceed() throws Exception {
        sender = new FirebaseFcmPushSender(firebaseApp);
        SendResponse success = mockSendResponse(true, null);
        BatchResponse batchResponse = mockBatchResponse(List.of(success));

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .thenReturn(batchResponse);

            List<String> invalidTokens = sender.sendMulticast(List.of("token-a"), "제목", "내용", Map.of());

            assertThat(invalidTokens).isEmpty();
        }
    }

    @Test
    @DisplayName("UNREGISTERED/INVALID_ARGUMENT로 실패한 토큰만 무효 토큰으로 식별하고, 다른 에러 코드는 제외한다")
    void identifiesOnlyUnregisteredOrInvalidArgumentTokensAsInvalid() throws Exception {
        sender = new FirebaseFcmPushSender(firebaseApp);
        SendResponse success = mockSendResponse(true, null);
        SendResponse unregistered = mockSendResponse(false, MessagingErrorCode.UNREGISTERED);
        SendResponse invalidArgument = mockSendResponse(false, MessagingErrorCode.INVALID_ARGUMENT);
        SendResponse internalError = mockSendResponse(false, MessagingErrorCode.INTERNAL);
        BatchResponse batchResponse = mockBatchResponse(List.of(success, unregistered, invalidArgument, internalError));

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .thenReturn(batchResponse);

            List<String> invalidTokens = sender.sendMulticast(
                    List.of("token-success", "token-unregistered", "token-invalid-argument", "token-internal-error"),
                    "제목", "내용", Map.of());

            assertThat(invalidTokens).containsExactly("token-unregistered", "token-invalid-argument");
        }
    }

    @Test
    @DisplayName("FCM 발송 중 예외가 발생해도 전파하지 않고 빈 리스트를 반환한다")
    void returnsEmptyListWhenSendThrows() throws Exception {
        sender = new FirebaseFcmPushSender(firebaseApp);

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .thenThrow(mock(FirebaseMessagingException.class));

            List<String> invalidTokens = sender.sendMulticast(List.of("token-a"), "제목", "내용", Map.of());

            assertThat(invalidTokens).isEmpty();
        }
    }

    @Test
    @DisplayName("토큰이 비어 있으면 FCM을 호출하지 않고 빈 리스트를 반환한다")
    void returnsEmptyListWithoutCallingFcmWhenTokensEmpty() {
        sender = new FirebaseFcmPushSender(firebaseApp);

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            List<String> invalidTokens = sender.sendMulticast(List.of(), "제목", "내용", Map.of());

            assertThat(invalidTokens).isEmpty();
            mocked.verifyNoInteractions();
        }
    }

    private SendResponse mockSendResponse(boolean successful, MessagingErrorCode errorCode) {
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(successful);
        if (!successful) {
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(exception.getMessagingErrorCode()).thenReturn(errorCode);
            when(sendResponse.getException()).thenReturn(exception);
        }
        return sendResponse;
    }

    private BatchResponse mockBatchResponse(List<SendResponse> responses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(responses);
        return batchResponse;
    }
}
