package com.company.assetmgmt.itassetmanagement.infrastructure.db;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

/**
 * MyBatis 基础设施：负责创建 SqlSession，并统一处理查询与事务边界。
 *
 * <p>Service 层只描述“做什么”，不再手写 try-with-resources、commit、rollback。
 * 所有写操作都通过 {@link #inTransaction(SessionWork)} 执行，保证多条 SQL
 * （例如“修改资产 + 写操作日志”）在同一个事务中提交或回滚。</p>
 */
public final class MyBatisUtil {

    private static final SqlSessionFactory SQL_SESSION_FACTORY;

    static {
        try (InputStream is = Resources.getResourceAsStream("mybatis-config.xml")) {
            Properties props = new Properties();
            props.setProperty("dbUrl", "jdbc:sqlite:" + DatabaseManager.getDbPath());

            XMLConfigBuilder parser = new XMLConfigBuilder(is, null, props);
            Configuration configuration = parser.parse();
            SQL_SESSION_FACTORY = new SqlSessionFactoryBuilder().build(configuration);
        } catch (Exception e) {
            throw new IllegalStateException("MyBatis 初始化失败！", e);
        }
    }

    private MyBatisUtil() {
    }

    /**
     * 在一个自动提交的会话中执行只读查询。
     */
    public static <T> T query(SessionWork<T> work) {
        try (SqlSession session = SQL_SESSION_FACTORY.openSession(true)) {
            return work.execute(session);
        }
    }

    /**
     * 在一个事务中执行写操作：正常结束提交，出现异常回滚。
     */
    public static <T> T inTransaction(SessionWork<T> work) {
        try (SqlSession session = SQL_SESSION_FACTORY.openSession(false)) {
            try {
                T result = work.execute(session);
                session.commit();
                return result;
            } catch (RuntimeException | Error e) {
                try {
                    session.rollback();
                } catch (RuntimeException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            }
        }
    }

    /**
     * 获取一个自动提交的会话。仅供调试或特殊场景使用，业务代码请优先使用
     * {@link #query(SessionWork)} / {@link #inTransaction(SessionWork)}。
     */
    public static SqlSession getSession() {
        return SQL_SESSION_FACTORY.openSession(true);
    }

    /**
     * 关闭连接池中的全部连接。恢复数据库文件前调用，避免 Windows 上文件被占用。
     * 之后仍可继续创建新连接。
     */
    public static void closePool() {
        DataSource dataSource = SQL_SESSION_FACTORY.getConfiguration()
                .getEnvironment()
                .getDataSource();
        if (dataSource instanceof PooledDataSource pooledDataSource) {
            pooledDataSource.forceCloseAll();
        }
    }

    @FunctionalInterface
    public interface SessionWork<T> {
        T execute(SqlSession session);
    }
}
