package com.nexters.palang.domain.opinion.infrastructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.api.Trigger;

// opinion_likes AFTER INSERT/DELETE 트리거(schema-h2.sql)의 H2 구현체.
// schema-mysql.sql의 MySQL 트리거와 동일하게 opinions.like_count를 증감시킨다 (backend-plan.md §5.4).
// H2는 트리거 본문에 순수 SQL을 쓸 수 없고 org.h2.api.Trigger를 구현한 컴파일된 클래스만 등록할 수 있다.
public class OpinionLikeCountH2Trigger implements Trigger {

    private int opinionIdColumnIndex;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName,
            boolean before, int type) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, schemaName, tableName, "OPINION_ID")) {
            if (!rs.next()) {
                throw new SQLException("opinion_id column not found on " + tableName);
            }
            opinionIdColumnIndex = rs.getInt("ORDINAL_POSITION") - 1;
        }
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        Object[] row = newRow != null ? newRow : oldRow;
        long opinionId = ((Number) row[opinionIdColumnIndex]).longValue();
        int delta = newRow != null ? 1 : -1;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE opinions SET like_count = like_count + ? WHERE id = ?")) {
            ps.setInt(1, delta);
            ps.setLong(2, opinionId);
            ps.executeUpdate();
        }
    }
}
