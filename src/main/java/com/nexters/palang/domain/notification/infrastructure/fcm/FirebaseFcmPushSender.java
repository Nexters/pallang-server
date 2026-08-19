package com.nexters.palang.domain.notification.infrastructure.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FirebaseFcmPushSender implements FcmPushSender {

    private final FirebaseApp firebaseApp;

    @Override
    public List<String> sendMulticast(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens.isEmpty()) {
            return List.of();
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendEachForMulticast(message);
            return invalidTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 발송 실패", e);
            return List.of();
        }
    }

    private List<String> invalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalid = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }
            MessagingErrorCode errorCode = sendResponse.getException() != null
                    ? sendResponse.getException().getMessagingErrorCode()
                    : null;
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                invalid.add(tokens.get(i));
            }
        }
        return invalid;
    }
}
