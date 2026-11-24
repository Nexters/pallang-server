package com.nexters.palang.domain.book.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 화면에 표기할 작가명으로 정리한다(이미지 저장 프레임의 "모순 · 양귀자" 등).
// Book.author는 들어온 경로에 따라 형식이 섞여 있다:
//   - 알라딘 연동 시절(v1.6.0 이전): "양귀자 (지은이)", "알베르 카뮈 (지은이), 김화영 (옮긴이)",
//     "김영하, 김연수 (지은이)"처럼 이름 뒤에 역할이 붙고, 역할 하나가 앞선 여러 이름에 걸린다.
//   - 카카오 연동 이후: "양귀자", "김영하, 김연수"처럼 이름만 쉼표로 이어져 있다(번역자는 없음).
//   - 직접 입력: 사용자가 입력한 그대로.
// 저장된 값은 그대로 두고 표기할 때만 역할 표기를 떼고, 번역·감수처럼 저자가 아닌 역할의 이름은 뺀다.
public final class BookAuthorFormatter {

    private static final String SEPARATOR = ", ";
    private static final Pattern NAME_WITH_ROLE = Pattern.compile("^(.*?)\\s*\\(([^()]+)\\)$");
    private static final Set<String> NON_AUTHOR_ROLES = Set.of("옮긴이", "역", "역자", "번역", "감수", "해설");

    private BookAuthorFormatter() {
    }

    public static String toDisplayName(String author) {
        if (author == null || author.isBlank()) {
            return author;
        }
        List<String> authors = new ArrayList<>();
        List<String> allNames = new ArrayList<>();
        List<String> pendingNames = new ArrayList<>();
        for (String token : author.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = NAME_WITH_ROLE.matcher(trimmed);
            if (!matcher.matches()) {
                // 역할이 아직 안 나온 이름: 뒤에 나올 역할이 함께 걸린다("김영하, 김연수 (지은이)").
                pendingNames.add(trimmed);
                continue;
            }
            addIfPresent(pendingNames, matcher.group(1).trim());
            allNames.addAll(pendingNames);
            if (!NON_AUTHOR_ROLES.contains(matcher.group(2).trim())) {
                authors.addAll(pendingNames);
            }
            pendingNames.clear();
        }
        // 끝까지 역할이 붙지 않은 이름(카카오/직접 입력 형식)은 저자로 본다.
        authors.addAll(pendingNames);
        allNames.addAll(pendingNames);
        // 모든 이름이 번역자 등으로만 표기된 예외적인 경우엔 빈 값 대신 역할만 떼어 낸 전체 이름을 보여준다.
        List<String> result = authors.isEmpty() ? allNames : authors;
        return result.isEmpty() ? author.trim() : String.join(SEPARATOR, result);
    }

    private static void addIfPresent(List<String> names, String name) {
        if (!name.isEmpty()) {
            names.add(name);
        }
    }
}
