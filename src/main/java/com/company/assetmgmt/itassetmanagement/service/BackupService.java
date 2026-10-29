package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.backup.BackupManager;
import com.company.assetmgmt.itassetmanagement.infrastructure.db.DatabaseManager;
import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 数据库备份与恢复。文件选择由 Controller 负责，本类只处理文件与数据校验。
 */
public class BackupService {

    private static final byte[] SQLITE_HEADER =
            "SQLite format 3\u0000".getBytes(StandardCharsets.US_ASCII);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** 退出时自动备份，并只保留最近 10 份。 */
    public Path autoBackup() throws IOException {
        return BackupManager.createBackup(
                databasePath(),
                Paths.get("backup"),
                BackupManager.DEFAULT_KEEP);
    }

    /** 手动备份到用户选择的位置，不参与自动备份的轮转。 */
    public void backupTo(Path target) throws IOException {
        Path source = databasePath();
        ensureDatabaseExists(source);

        if (Files.exists(target) && Files.isSameFile(source, target)) {
            throw new IOException("不能把数据库备份到它自己。");
        }

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 用指定文件覆盖当前数据库。
     *
     * <p>覆盖前会先校验 SQLite 文件头，并把当前数据库另存为
     * backup/pre_restore_时间戳.db，避免误选文件后无法挽回。</p>
     */
    public Path restoreFrom(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("请选择有效的数据库文件。");
        }
        if (!isSqliteDatabase(source)) {
            throw new IOException("所选文件不是有效的 SQLite 数据库。");
        }

        Path target = databasePath();
        ensureDatabaseExists(target);
        if (Files.isSameFile(source, target)) {
            throw new IOException("所选文件就是当前正在使用的数据库。");
        }

        // 先释放连接池，避免 Windows 上数据库文件被占用。
        MyBatisUtil.closePool();

        Path safetyBackup = Paths.get(
                "backup",
                "pre_restore_" + LocalDateTime.now().format(TIME_FORMAT) + ".db");
        Files.createDirectories(safetyBackup.getParent());
        Files.copy(target, safetyBackup, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        return safetyBackup;
    }

    private Path databasePath() {
        return Paths.get(DatabaseManager.getDbPath());
    }

    private void ensureDatabaseExists(Path database) throws IOException {
        if (!Files.isRegularFile(database)) {
            throw new IOException("数据库文件不存在：" + database.toAbsolutePath());
        }
    }

    private boolean isSqliteDatabase(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] header = in.readNBytes(SQLITE_HEADER.length);
            return Arrays.equals(header, SQLITE_HEADER);
        }
    }
}
