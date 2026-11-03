package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.LogMapper;
import com.company.assetmgmt.itassetmanagement.model.OperationLog;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志查询与清理。日志的写入由对应业务 Service 在自己的事务中完成。
 */
public class LogService {

    /** SQLite CURRENT_TIMESTAMP 使用 UTC，格式固定为 yyyy-MM-dd HH:mm:ss。 */
    private static final DateTimeFormatter SQLITE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<OperationLog> findAll() {
        return MyBatisUtil.query(session -> session.getMapper(LogMapper.class).selectAll());
    }

    public List<OperationLog> findRepairLogs() {
        return MyBatisUtil.query(session -> session.getMapper(LogMapper.class).selectRepairLogs());
    }

    public void deleteById(Integer logId) {
        if (logId == null) {
            throw new ServiceException("缺少日志 ID，无法删除。");
        }
        try {
            int deleted = MyBatisUtil.inTransaction(session ->
                    session.getMapper(LogMapper.class).deleteById(logId));
            if (deleted == 0) {
                throw new ServiceException("日志不存在或已被删除。");
            }
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "删除日志");
        }
    }

    /**
     * 删除一年以前的日志。
     *
     * @return 实际删除的日志条数
     */
    public int deleteOlderThanOneYear() {
        String cutoff = LocalDateTime.now(ZoneOffset.UTC)
                .minusYears(1)
                .format(SQLITE_TIMESTAMP);
        try {
            return MyBatisUtil.inTransaction(session ->
                    session.getMapper(LogMapper.class).deleteOlderThan(cutoff));
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "清理历史日志");
        }
    }
}
