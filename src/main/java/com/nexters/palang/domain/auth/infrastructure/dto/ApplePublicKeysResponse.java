package com.nexters.palang.domain.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Apple JWKS(GET https://appleid.apple.com/auth/keys) 응답. RSA 공개키 재구성에 필요한 kid/n/e만 사용한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplePublicKeysResponse(List<ApplePublicKey> keys) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplePublicKey(String kty, String kid, String use, String alg, String n, String e) {
    }
}
