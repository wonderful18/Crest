package io.crest.dataset.utils;

import com.google.common.collect.ImmutableList;
import io.crest.exception.CrestException;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.apache.calcite.sql.SqlKind.*;

public class SqlUtils {
    public static Logger logger = LoggerFactory.getLogger(SqlUtils.class);

    public static String addSchema(String sql, String schema) {
        sql = sql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        // Lex.JAVA 下 AS '别名'（单引号字符串字面量）不能做别名，转成反引号标识符
        sql = convertSingleQuotedAliases(sql);

        SqlParser.Config config =
                SqlParser.config()
                        .withLex(Lex.JAVA)
                        .withIdentifierMaxLength(256);
        // 创建解析器
        SqlParser sqlParser = SqlParser
                .create(sql, config);
        // 生成 AST 语法树
        SqlNode sqlNode = null;
        try {
            sqlNode = sqlParser.parseStmt();
            addTableSchema(sqlNode, false, schema, config);
        } catch (SqlParseException e) {
            CrestException.throwException("使用 Calcite 进行语法分析发生了异常:" + e);
        }
        String sqlRender = sqlNode.toString();
        // 处理sql中多余的`都替换成1个
        sqlRender = sqlRender.replaceAll("(`+)", "`");
        // Calcite 的 toSqlString 会把非 ASCII 字符串标记为 _UTF-8'xxx'，MySQL 不认识，去掉前缀
        // 用字符串替换而非正则，避免特殊字符匹配问题
        logger.info("[addSchema-debug] before UTF-8 cleanup: {}", sqlRender);
        while (sqlRender.contains("_UTF-8'")) {
            sqlRender = sqlRender.replace("_UTF-8'", "'");
        }
        // 兜底：用正则匹配所有 _ 开头后跟字母数字到单引号的前缀（如 _UTF-8'、_UTF8' 等）
        sqlRender = sqlRender.replaceAll("_[A-Za-z0-9-]+'", "'");
        // Calcite 的 Lex.JAVA 会把 LIMIT N 转成 FETCH NEXT N ROWS ONLY，MySQL 不认识，转回 LIMIT
        sqlRender = sqlRender.replaceAll("FETCH NEXT (\\d+) ROWS ONLY", "LIMIT $1");
        logger.info("[addSchema-debug] after UTF-8 cleanup: {}", sqlRender);
        return sqlRender;
    }

    /**
     * 将 SQL 中 AS '别名'（单引号）和 AS "别名"（双引号）形式的别名
     * 统一转成反引号 `别名`，以兼容 Calcite Lex.JAVA
     * （单引号是字符串字面量不能做别名；双引号在 Lex.JAVA 下也不识别）。
     */
    public static String convertSingleQuotedAliases(String sql) {
        // AS 'xxx'（单引号）→ AS `xxx`
        sql = sql.replaceAll("(?i)\\bAS\\s+'([^']*)'", "AS `$1`");
        // AS "xxx"（双引号）→ AS `xxx`
        sql = sql.replaceAll("(?i)\\bAS\\s+\"([^\"]*)\"", "AS `$1`");
        return sql;
    }

    private static void addTableSchema(SqlNode sqlNode, Boolean fromOrJoin, String schema, SqlParser.Config config) {
        try {
            if (sqlNode.getKind() == JOIN) {
                SqlJoin sqlKind = (SqlJoin) sqlNode;
                addTableSchema(sqlKind.getLeft(), true, schema, config);
                addTableSchema(sqlKind.getRight(), true, schema, config);
            } else if (sqlNode.getKind() == IDENTIFIER) {
                if (fromOrJoin) {
                    // 获取表名
                    String tableName = sqlNode.toString();
                    SqlIdentifier sqlKind = (SqlIdentifier) sqlNode;
                    sqlKind.setNames(ImmutableList.of(schema + "`.`" + tableName), null);
                }
            } else if (sqlNode.getKind() == AS) {
                SqlBasicCall sqlKind = (SqlBasicCall) sqlNode;
                if (sqlKind.getOperandList().size() >= 2) {
                    addTableSchema(sqlKind.getOperandList().get(0), fromOrJoin, schema, config);
                }
            } else if (sqlNode.getKind() == SELECT) {
                SqlSelect sqlKind = (SqlSelect) sqlNode;

                // 解析from
                addTableSchema(sqlKind.getFrom(), true, schema, config);

                // 解析where
                SqlBasicCall where = (SqlBasicCall) sqlKind.getWhere();
                if (where != null && where.getOperandList().size() >= 2) {
                    for (int i = 0; i < where.getOperandList().size(); i++) {
                        addTableSchema(where.getOperandList().get(i), false, schema, config);
                    }
                }
            } else if (sqlNode.getKind() == UNION) {
                SqlBasicCall sqlKind = (SqlBasicCall) sqlNode;
                // 使用union，至少会有2个子SQL，否则语法不正确
                if (sqlKind.getOperandList().size() >= 2) {
                    for (int i = 0; i < sqlKind.getOperandList().size(); i++) {
                        addTableSchema(sqlKind.getOperandList().get(i), fromOrJoin, schema, config);
                    }
                }
            } else if (sqlNode.getKind() == ORDER_BY) {
                SqlOrderBy sqlKind = (SqlOrderBy) sqlNode;
                List<SqlNode> operandList = sqlKind.getOperandList();
                if (operandList.size() > 0) {
                    addTableSchema(operandList.get(0), fromOrJoin, schema, config);
                }
            } else if (sqlNode.getKind() == IN
                    || sqlNode.getKind() == NOT_IN
                    || sqlNode.getKind() == AND
                    || sqlNode.getKind() == OR
                    || sqlNode.getKind() == LESS_THAN
                    || sqlNode.getKind() == GREATER_THAN
                    || sqlNode.getKind() == LESS_THAN_OR_EQUAL
                    || sqlNode.getKind() == GREATER_THAN_OR_EQUAL
                    || sqlNode.getKind() == EQUALS
                    || sqlNode.getKind() == NOT_EQUALS) {
                SqlBasicCall where = (SqlBasicCall) sqlNode;
                if (where.getOperandList().size() >= 2) {
                    for (int i = 0; i < where.getOperandList().size(); i++) {
                        addTableSchema(where.getOperandList().get(i), fromOrJoin, schema, config);
                    }
                }
            }
        } catch (RuntimeException e) {
            CrestException.throwException("使用 Calcite 进行语法分析发生了异常:" + e);
        }
    }

}
