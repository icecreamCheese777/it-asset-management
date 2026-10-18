package com.company.assetmgmt.itassetmanagement.infrastructure.db;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseManager {
    // 获取安全的用户数据目录
    public static String getDbPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String baseDir;

        if (os.contains("win")) {
            // Windows: 存在 C:\Users\用户名\AppData\Roaming\ITAssetMgmt
            baseDir = System.getenv("APPDATA") + File.separator + "ITAssetMgmt";
        } else if (os.contains("mac")) {
            // Mac: 存在 ~/Library/Application Support/ITAssetMgmt
            baseDir = System.getProperty("user.home") + "/Library/Application Support/ITAssetMgmt";
        } else {
            // Linux: 存在 ~/.itassetmgmt
            baseDir = System.getProperty("user.home") + "/.itassetmgmt";
        }

        // 确保目录存在
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return baseDir + File.separator + "asset.db";
    }

    // 检查并初始化数据库
    public static void initDatabase() {
        String dbPath = getDbPath();
        File dbFile = new File(dbPath);

        // 如果数据库文件不存在，说明是新电脑首次运行
        if (!dbFile.exists()) {
            System.out.println("未检测到数据库，正在自动初始化...");
            try {
                // 1. 从 resources 读取 V1_init.sql
                InputStream is = DatabaseManager.class.getResourceAsStream("/sql/V1_init.sql");
                if (is == null) {
                    throw new RuntimeException("找不到建表脚本：/sql/V1_init.sql");
                }
                Scanner scanner = new Scanner(is, "UTF-8").useDelimiter(";"); // 用分号分割SQL

                // 2. 连接到 SQLite（如果文件不存在会自动创建）
                String url = "jdbc:sqlite:" + dbPath;
                try (Connection conn = DriverManager.getConnection(url);
                     Statement stmt = conn.createStatement()) {

                    // 3. 逐条执行 SQL
                    while (scanner.hasNext()) {
                        String sql = scanner.next().trim();
                        if (!sql.isEmpty()) {
                            stmt.execute(sql);
                        }
                    }
                }
                System.out.println("数据库及基础表初始化成功！");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("数据库初始化失败！");
            }
        } else {
            System.out.println("数据库已存在，路径：" + dbPath);
        }
    }
}
