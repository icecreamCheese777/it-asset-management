package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.Category;
import com.company.assetmgmt.itassetmanagement.model.Department;
import java.util.List;

public interface CommonMapper {
    List<Category> selectAllCategories();
    Category selectCategoryById(Integer id);
    List<Department> selectAllDepartments();
    // 新增分类
    int insertCategory(Category category);
    // 修改分类
    int updateCategory(Category category);
    // 删除分类
    int deleteCategory(Integer id);
    // 检查分类下是否有资产（防止误删）
    int countAssetsByCategoryId(Integer categoryId);
}