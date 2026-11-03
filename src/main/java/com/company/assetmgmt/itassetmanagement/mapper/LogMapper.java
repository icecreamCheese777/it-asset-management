package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.OperationLog;

import java.util.List;

public interface LogMapper {

    List<OperationLog> selectAll();

    /** 只查询维修类日志，用于维修记录视图。 */
    List<OperationLog> selectRepairLogs();

    int insertLog(OperationLog log);

    /** 删除单条日志。 */
    int deleteById(Integer id);

    /** 删除早于指定时间（yyyy-MM-dd HH:mm:ss）的日志。 */
    int deleteOlderThan(String cutoff);
}
