package com.nexters.palang.domain.passage.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;

// 유사 문장 판정(FR-WRITE-07)용: quotedText → 공백/구두점 제거 정규화 → SHA-256 해시.
// SimilarPassageFinder(조회)와 OpinionService(신규 Passage 생성) 양쪽에서 재사용해 해시 로직 이중화를 막는다.
public final class PassageNormalizer {

    // 한글은 입력 환경에 따라 조합형/완성형(NFC/NFD)이 섞여 들어올 수 있어 해싱 전에 NFC로 통일한다.
    // \p{Punct}는 ASCII 전용이라 한글 문장에 흔한 전각 구두점을 놓치므로 유니코드 구두점 카테고리를 사용한다.
    private static final String NORMALIZE_STRIP_PATTERN = "[\\s\\p{IsPunctuation}]";

    private PassageNormalizer() {
    }

    public static String normalizedHash(String quotedText) {
        String normalized = Normalizer.normalize(quotedText, Normalizer.Form.NFC)
                .replaceAll(NORMALIZE_STRIP_PATTERN, "");
        return sha256Hex(normalized);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
