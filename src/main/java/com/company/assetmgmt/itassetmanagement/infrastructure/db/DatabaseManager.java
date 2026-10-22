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
/*        String os = System.getProperty("os.name").toLowerCase();
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

        return baseDir + File.separator + "asset.db";*/

        // 直接指定在当前项目根目录下的 data 文件夹
        String baseDir = "data";

        // 确保目录存在
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 返回相对路径 (如 data\asset.db)
        return baseDir + File.separator + "asset.db";
    }

    // 检查并初始化数据库
    // 初始化数据库
    public static void initDatabase() {
        String dbPath = getDbPath();
        File dbFile = new File(dbPath);

        // 如果数据库文件不存在，说明是首次运行
        if (!dbFile.exists()) {
            System.out.println("未检测到数据库，正在自动初始化...");
            String url = "jdbc:sqlite:" + dbPath;

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {

                // --- 调试代码开始 ---
                java.net.URL urlRes = Thread.currentThread().getContextClassLoader().getResource("sql/V1_init.sql");
                if (urlRes == null) {
                    System.out.println("============== 调试信息 ==============");
                    System.out.println("当前 Classpath 路径: " + System.getProperty("java.class.path"));
                    System.out.println("尝试读取的路径: sql/V1_init.sql");
                    System.out.println("找不到文件！请检查 target/classes 下是否有这个文件。");
                    System.out.println("======================================");
                    throw new RuntimeException("建表脚本丢失");
                } else {
                    System.out.println("成功找到 SQL 文件，真实路径在: " + urlRes.getPath());
                }
                // --- 调试代码结束 ---

                // ==========================================
                // 核心修复：使用 ClassLoader 读取 resources 下的文件
                // ==========================================
                java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sql/V1_init.sql");

                if (is == null) {
                    throw new RuntimeException("致命错误：在 classpath 中找不到 sql/V1_init.sql，请确保它放在 src/main/resources/sql/ 下，并执行了 mvn clean package！");
                }

                // 使用 Scanner 读取 InputStream 里的内容，并用分号分割多条 SQL
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter(";");
                while (scanner.hasNext()) {
                    String sql = scanner.next().trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                scanner.close();
                is.close();

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
