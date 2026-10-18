package com.company.assetmgmt.itassetmanagement.infrastructure.db;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;

import java.io.InputStream;
public class MyBatisUtil {

    // 全局唯一的 SqlSessionFactory
    private static SqlSessionFactory sqlSessionFactory;

    // 静态代码块：类加载时只执行一次
    static {
        try {
            // 1. 读取 mybatis-config.xml
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

            // 2. 使用 XMLConfigBuilder 解析配置（为了能动态替换路径）
            XMLConfigBuilder parser = new XMLConfigBuilder(is);
            Configuration configuration = parser.parse();

            // 3. 动态获取当前电脑上的绝对数据库路径
            String dynamicDbUrl = "jdbc:sqlite:" + DatabaseManager.getDbPath();

            // 4. 将路径注入到 XML 中的 ${dbUrl} 占位符里
            configuration.getVariables().put("dbUrl", dynamicDbUrl);

            // 5. 构建 SqlSessionFactory
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("MyBatis 初始化失败！", e);
        }
    }

    /**
     * 获取 SqlSession（用完必须关闭）
     */
    public static SqlSession getSession() {
        // true 表示开启自动提交事务（桌面单机应用一般开启方便点，或者你可以手动提交）
        return sqlSessionFactory.openSession(true);
    }
}
