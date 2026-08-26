package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.user.domain.User;

public record AdminUserSearchResult(User user, long hostedGroupCount) {
}
