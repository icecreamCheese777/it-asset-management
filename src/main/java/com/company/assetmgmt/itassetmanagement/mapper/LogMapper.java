package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.OperationLog;

import java.util.List;

public interface LogMapper {
    List<OperationLog> selectAll();
    int insertLog(OperationLog log);
}
