package io.crest.datasource.request;

import io.crest.datasource.dao.auto.entity.CoreEngine;
import lombok.Data;

import java.util.Locale;

@Data
@SuppressWarnings("deprecation")
public class EngineRequest {
    private final String REG_WITH_SQL_FRAGMENT = "((?i)WITH[\\s\\S]+(?i)AS?\\s*\\([\\s\\S]+\\))\\s*(?i)SELECT";
    protected String query;
    protected String table;
    protected CoreEngine engine;
    private Integer pageSize;
    private Integer page;
    private Integer realSize;
    private Integer fetchSize = 10000;
    private boolean pageable = false;
    private boolean previewData = false;
    private boolean totalPageFlag;

    public EngineRequest() {
    }

    public String getQuery() {
        return this.rebuildSqlWithFragment(this.query);
    }

    public void setQuery(String query) {
        this.query = query;
    }

    private String rebuildSqlWithFragment(String sql) {
        String lowerSql = sql.toLowerCase(Locale.ROOT);
        if (!lowerSql.startsWith("with")) {
            int withIndex = lowerSql.indexOf("with");
            int selectIndex = withIndex < 0 ? -1 : lowerSql.indexOf("select", withIndex + 4);
            if (withIndex >= 0 && selectIndex > withIndex) {
                String withFragment = sql.substring(withIndex, selectIndex);
                String withSelectFragment = sql.substring(withIndex, selectIndex + 6);
                sql = sql.replace(withSelectFragment, "SELECT");
                sql = (withFragment + " " + sql).replace("  ", " ");
            }
        }

        return sql;
    }

    public String getREG_WITH_SQL_FRAGMENT() {
        this.getClass();
        return "((?i)WITH[\\s\\S]+(?i)AS?\\s*\\([\\s\\S]+\\))\\s*(?i)SELECT";
    }
}
