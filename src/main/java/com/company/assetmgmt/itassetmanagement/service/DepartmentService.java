package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.DepartmentMapper;
import com.company.assetmgmt.itassetmanagement.model.Department;

import java.util.List;

/**
 * 部门业务。
 */
public class DepartmentService {

    public List<Department> findAll() {
        return MyBatisUtil.query(session -> session.getMapper(DepartmentMapper.class).selectAll());
    }

    public int countAssets(Integer departmentId) {
        if (departmentId == null) {
            return 0;
        }
        return MyBatisUtil.query(session ->
                session.getMapper(DepartmentMapper.class).countAssetsByDeptId(departmentId));
    }

    public Department create(String name) {
        String cleaned = ServiceSupport.requireText(name, "部门名称不能为空！");
        try {
            return MyBatisUtil.inTransaction(session -> {
                Department department = new Department();
                department.setName(cleaned);
                session.getMapper(DepartmentMapper.class).insert(department);
                return department;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, "部门名称已存在：" + cleaned, "保存部门");
        }
    }

    public Department rename(Integer departmentId, String name) {
        if (departmentId == null) {
            throw new ServiceException("缺少部门 ID，无法修改。");
        }
        String cleaned = ServiceSupport.requireText(name, "部门名称不能为空！");
        try {
            return MyBatisUtil.inTransaction(session -> {
                Department department = new Department();
                department.setId(departmentId);
                department.setName(cleaned);
                int updated = session.getMapper(DepartmentMapper.class).update(department);
                if (updated == 0) {
                    throw new ServiceException("部门不存在或已被删除。");
                }
                return department;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, "部门名称已存在：" + cleaned, "保存部门");
        }
    }

    public void delete(Integer departmentId) {
        if (departmentId == null) {
            throw new ServiceException("缺少部门 ID，无法删除。");
        }
        try {
            MyBatisUtil.inTransaction(session -> {
                DepartmentMapper mapper = session.getMapper(DepartmentMapper.class);
                int assetCount = mapper.countAssetsByDeptId(departmentId);
                if (assetCount > 0) {
                    throw new ServiceException("该部门下还有 " + assetCount + " 个资产，无法删除！");
                }
                int deleted = mapper.deleteById(departmentId);
                if (deleted == 0) {
                    throw new ServiceException("部门不存在或已被删除。");
                }
                return null;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "删除部门");
        }
    }
}
