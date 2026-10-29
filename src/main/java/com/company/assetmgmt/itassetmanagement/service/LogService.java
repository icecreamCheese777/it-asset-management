package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.LogMapper;
import com.company.assetmgmt.itassetmanagement.model.OperationLog;

import java.util.List;

/**
 * 操作日志查询。日志的写入由对应业务 Service 在自己的事务中完成。
 */
public class LogService {

    public List<OperationLog> findAll() {
        return MyBatisUtil.query(session -> session.getMapper(LogMapper.class).selectAll());
    }
}
