package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.group.domain.Group;

public record AdminGroupSearchResult(Group group, long memberCount) {
}
