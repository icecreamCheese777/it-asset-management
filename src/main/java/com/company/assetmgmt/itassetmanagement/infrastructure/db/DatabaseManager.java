package com.company.assetmgmt.itassetmanagement.infrastructure.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;

/**
 * 数据库文件定位与首次建库。
 */
public final class DatabaseManager {

    private static final String DATA_DIRECTORY = "data";
    private static final String DATABASE_FILE = "asset.db";
    private static final String INIT_SCRIPT = "sql/V1_init.sql";

    private DatabaseManager() {
    }

    /**
     * 数据库固定放在进程工作目录下的 data/asset.db。
     */
    public static String getDbPath() {
        Path directory = Paths.get(DATA_DIRECTORY);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建数据目录：" + directory.toAbsolutePath(), e);
        }
        return directory.resolve(DATABASE_FILE).toString();
    }

    /**
     * 首次运行时创建表结构与基础数据；数据库已存在时不做任何修改。
     */
    public static void initDatabase() {
        Path database = Paths.get(getDbPath());
        if (Files.exists(database)) {
            System.out.println("数据库已存在，路径：" + database.toAbsolutePath());
            return;
        }

        System.out.println("未检测到数据库，正在自动初始化...");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement();
             InputStream script = Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream(INIT_SCRIPT)) {

            if (script == null) {
                throw new IllegalStateException("在 classpath 中找不到建库脚本：" + INIT_SCRIPT);
            }

            try (Scanner scanner = new Scanner(script, StandardCharsets.UTF_8).useDelimiter(";")) {
                while (scanner.hasNext()) {
                    String sql = scanner.next().trim();
                    if (!sql.isEmpty()) {
                        statement.execute(sql);
                    }
                }
            }
            System.out.println("数据库及基础表初始化成功！");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "数据库初始化失败：" + database.toAbsolutePath(), e);
        }
    }
}
