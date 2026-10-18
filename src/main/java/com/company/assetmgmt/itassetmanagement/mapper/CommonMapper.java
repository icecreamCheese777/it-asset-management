package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.Category;
import com.company.assetmgmt.itassetmanagement.model.Department;
import java.util.List;

public interface CommonMapper {
    List<Category> selectAllCategories();
    List<Department> selectAllDepartments();
}