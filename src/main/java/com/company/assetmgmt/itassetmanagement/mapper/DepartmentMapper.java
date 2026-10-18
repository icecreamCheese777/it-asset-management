package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.Department;
import java.util.List;

public interface DepartmentMapper {
    List<Department> selectAll();
    int insert(Department dept);
    int update(Department dept);
    int deleteById(Integer id);
    // 检查该部门下是否有资产，防止误删
    int countAssetsByDeptId(Integer deptId);
}