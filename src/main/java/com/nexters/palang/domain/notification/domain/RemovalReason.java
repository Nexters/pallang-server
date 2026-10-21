package com.nexters.palang.domain.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자가 게시물을 삭제할 때 고르는 사유. 사유마다 작성자에게 보낼 안내 문구가 정해져 있다(운영 정책 문서 기준).
@Getter
@AllArgsConstructor
public enum RemovalReason {

    PASSAGE_FIELD_MISUSE("책 내용 입력란에 의견 작성",
            "책 문장 발췌란에 의견이 작성되어 있어 게시글 전체가 삭제되었습니다. "
                    + "책에서 발췌한 문장과 내 의견을 구분해서 작성 부탁드립니다.",
            true),
    ADVERTISEMENT("광고성 내용",
            "광고성 내용이 포함되어 있어 관리자에 의해 게시물 전체가 삭제되었습니다.",
            true),
    ABUSE("욕설·비방",
            "욕설이나 다른 사람을 비방하는 내용이 포함되어 있어 관리자에 의해 게시물 전체가 삭제되었습니다.",
            false),
    SPAM("도배·반복 게시",
            "동일하거나 유사한 내용을 반복해서 게시하여 관리자에 의해 게시물 전체가 삭제되었습니다.",
            true),
    ;

    public static final String CONTACT_EMAIL = "pallang4you@gmail.com";
    public static final String CONTACT_GUIDE =
            "삭제 사유에 대해 궁금한 점이 있거나 문의사항이 필요한 경우 " + CONTACT_EMAIL + "로 문의해주세요.";

    private final String label;
    private final String message;
    // 욕설·비방처럼 내용 자체가 유해한 경우 안내 화면에 내용 일부를 다시 보여주지 않는다.
    private final boolean showsContentPreview;
}
