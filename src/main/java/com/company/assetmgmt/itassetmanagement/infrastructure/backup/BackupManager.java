package com.company.assetmgmt.itassetmanagement.infrastructure.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 数据库备份与“只保留最近 N 份”的清理逻辑。
 *
 * <p>排序依据是文件名里的时间戳，而不是文件修改时间。文件修改时间可能因为
 * 复制、解压、同步工具或文件系统精度而完全相同，此时排序结果不稳定，会误删
 * 最新的备份。文件名里的时间戳由程序生成，能稳定表达备份的先后顺序。</p>
 */
public final class BackupManager {

    /** 默认保留的备份份数。 */
    public static final int DEFAULT_KEEP = 10;

    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter PARSE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 支持两种文件名：
     * asset_20261029_213000.db        （同一秒内的第一份）
     * asset_20261029_213000_1.db      （同一秒内的后续备份，避免覆盖）
     */
    private static final Pattern BACKUP_NAME =
            Pattern.compile("^asset_(\\d{8})_(\\d{6})(?:_(\\d+))?\\.db$");

    private BackupManager() {
    }

    /**
     * 备份数据库，并清理超出保留份数的旧备份。
     *
     * @param sourceDb  当前数据库文件
     * @param backupDir 备份目录
     * @param keep      最多保留多少份（包含本次新备份）
     * @return 本次新建的备份文件
     */
    public static Path createBackup(Path sourceDb, Path backupDir, int keep) throws IOException {
        if (keep < 1) {
            throw new IllegalArgumentException("保留份数必须大于 0");
        }
        if (!Files.isRegularFile(sourceDb)) {
            throw new IOException("数据库文件不存在：" + sourceDb.toAbsolutePath());
        }

        Files.createDirectories(backupDir);

        // 以秒为精度命名；同一秒内多次备份时追加序号，避免互相覆盖。
        String baseName = "asset_" + LocalDateTime.now().format(FILE_TIME_FORMAT);
        Path target = backupDir.resolve(baseName + ".db");
        int sequence = 1;
        while (Files.exists(target)) {
            target = backupDir.resolve(baseName + "_" + sequence++ + ".db");
        }

        Files.copy(sourceDb, target);
        System.out.println("数据库已自动备份至: " + target.toAbsolutePath());

        try {
            prune(backupDir, keep);
        } catch (IOException e) {
            // 备份本身已经成功，清理失败不应让调用方误以为备份失败。
            System.err.println("备份已创建，但清理旧备份失败: " + e.getMessage());
        }
        return target;
    }

    /**
     * 删除超出保留份数的旧备份，保留文件名时间戳最新的 {@code keep} 份。
     *
     * @return 实际删除的文件；删除失败的旧备份会保留并输出错误信息
     */
    public static List<Path> prune(Path backupDir, int keep) throws IOException {
        if (keep < 1) {
            throw new IllegalArgumentException("保留份数必须大于 0");
        }
        if (!Files.isDirectory(backupDir)) {
            return List.of();
        }

        List<BackupFile> backups;
        try (Stream<Path> stream = Files.list(backupDir)) {
            backups = stream
                    .filter(Files::isRegularFile)
                    .map(BackupManager::parseBackupFile)
                    .flatMap(Optional::stream)
                    .sorted(BackupFile.NEWEST_FIRST)
                    .toList();
        }

        List<Path> deleted = new ArrayList<>();
        for (int i = keep; i < backups.size(); i++) {
            Path oldBackup = backups.get(i).path();
            if (delete(oldBackup)) {
                deleted.add(oldBackup);
                System.out.println("已清理过期备份: " + oldBackup.getFileName());
            }
        }
        return deleted;
    }

    private static boolean delete(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException firstFailure) {
            // Windows 上只读文件无法直接删除，去掉只读属性后重试一次。
            try {
                path.toFile().setWritable(true);
                Files.deleteIfExists(path);
                return true;
            } catch (IOException secondFailure) {
                System.err.println("无法删除旧备份: " + path + " (" + secondFailure.getMessage() + ")");
                return false;
            }
        }
    }

    private static Optional<BackupFile> parseBackupFile(Path path) {
        Matcher matcher = BACKUP_NAME.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            LocalDateTime time = LocalDateTime.parse(
                    matcher.group(1) + matcher.group(2), PARSE_TIME_FORMAT);
            int sequence = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            return Optional.of(new BackupFile(path, time, sequence));
        } catch (DateTimeParseException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private record BackupFile(Path path, LocalDateTime time, int sequence) {

        private static final Comparator<BackupFile> NEWEST_FIRST =
                Comparator.comparing(BackupFile::time)
                        .thenComparingInt(BackupFile::sequence)
                        .thenComparing(backup -> backup.path().getFileName().toString())
                        .reversed();
    }
}
