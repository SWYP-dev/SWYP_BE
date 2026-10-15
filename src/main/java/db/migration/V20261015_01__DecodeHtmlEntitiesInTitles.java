package db.migration;

import com.chwihap.server.global.util.HtmlEntityUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 외부 API 수집 공고의 title/company_name에 남아 있는 HTML 엔티티를 원래 문자로 되돌린다. (#175)
 * 신규 수집분과 결과가 같도록 SQL REPLACE 대신 수집 로직과 동일한 {@link HtmlEntityUtils#decode(String)}를 사용한다.
 * job_postings/application_postings는 사용자가 직접 입력한 공고(DIRECT)를 제외한다.
 */
public class V20261015_01__DecodeHtmlEntitiesInTitles extends BaseJavaMigration {

    private static final List<Target> TARGETS = List.of(
            new Target("job_feeds", "title", false),
            new Target("job_feeds", "company_name", false),
            new Target("job_postings", "title", true),
            new Target("job_postings", "company_name", true),
            new Target("application_postings", "title", true),
            new Target("application_postings", "company_name", true));

    private static final int BATCH_SIZE = 500;

    /**
     * 대상 테이블/컬럼마다 엔티티가 포함된 행만 골라 디코딩 결과로 갱신합니다.
     *
     * @param context Flyway 마이그레이션 컨텍스트
     * @throws SQLException 조회 또는 갱신에 실패한 경우
     */
    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        for (Target target : TARGETS) {
            decodeColumn(connection, target);
        }
    }

    private void decodeColumn(Connection connection, Target target) throws SQLException {
        String select = "SELECT id, " + target.column() + " FROM " + target.table()
                + " WHERE " + target.column() + " LIKE '%&%;%'"
                + (target.excludeDirect() ? " AND platform <> 'DIRECT'" : "");
        String update = "UPDATE " + target.table() + " SET " + target.column() + " = ? WHERE id = ?";

        try (PreparedStatement selectStatement = connection.prepareStatement(select);
             ResultSet rows = selectStatement.executeQuery();
             PreparedStatement updateStatement = connection.prepareStatement(update)) {
            int pending = 0;
            while (rows.next()) {
                String original = rows.getString(2);
                String decoded = HtmlEntityUtils.decode(original);
                if (decoded.equals(original)) {
                    continue;
                }
                updateStatement.setString(1, decoded);
                updateStatement.setLong(2, rows.getLong(1));
                updateStatement.addBatch();
                if (++pending % BATCH_SIZE == 0) {
                    updateStatement.executeBatch();
                }
            }
            updateStatement.executeBatch();
        }
    }

    private record Target(String table, String column, boolean excludeDirect) {
    }
}
