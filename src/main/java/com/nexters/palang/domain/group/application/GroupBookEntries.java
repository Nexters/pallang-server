package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 기존 단일 책 API 호환: 모임 상세/목록/초대 미리보기 응답의 bookId/bookTitle/startDate/endDate는 모임에
// 가장 먼저 등록된 entry(대표 책) 기준으로 채운다. 책 추가 기능(#225) 전에는 모임마다 entry가 정확히 1개라
// 기존 응답과 값이 같다. #225에서 목록/상세 응답을 여러 권 구조로 바꾸면 이 호환 경로는 정리한다.
@Component
@RequiredArgsConstructor
public class GroupBookEntries {

    private final GroupBookEntryRepository groupBookEntryRepository;

    public GroupBookEntry getPrimaryEntry(Long groupId) {
        List<GroupBookEntry> entries = groupBookEntryRepository.findAllWithBookByGroupIdIn(List.of(groupId));
        if (entries.isEmpty()) {
            // 모임은 생성 시 최소 1권을 가지며 책 삭제는 지원하지 않으므로 정상 데이터에서는 일어나지 않는다.
            throw new IllegalStateException("모임에 등록된 책이 없습니다. groupId=" + groupId);
        }
        return entries.getFirst();
    }

}
