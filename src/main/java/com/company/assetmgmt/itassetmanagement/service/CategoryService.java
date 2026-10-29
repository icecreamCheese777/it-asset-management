package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.CommonMapper;
import com.company.assetmgmt.itassetmanagement.model.Category;

import java.util.List;

/**
 * 资产分类业务。
 */
public class CategoryService {

    public List<Category> findAll() {
        return MyBatisUtil.query(session -> session.getMapper(CommonMapper.class).selectAllCategories());
    }

    public int countAssets(Integer categoryId) {
        if (categoryId == null) {
            return 0;
        }
        return MyBatisUtil.query(session ->
                session.getMapper(CommonMapper.class).countAssetsByCategoryId(categoryId));
    }

    public Category create(String name) {
        String cleaned = ServiceSupport.requireText(name, "分类名称不能为空！");
        try {
            return MyBatisUtil.inTransaction(session -> {
                Category category = new Category();
                category.setName(cleaned);
                session.getMapper(CommonMapper.class).insertCategory(category);
                return category;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, "分类名称已存在：" + cleaned, "保存分类");
        }
    }

    public Category rename(Integer categoryId, String name) {
        if (categoryId == null) {
            throw new ServiceException("缺少分类 ID，无法修改。");
        }
        String cleaned = ServiceSupport.requireText(name, "分类名称不能为空！");
        try {
            return MyBatisUtil.inTransaction(session -> {
                Category category = new Category();
                category.setId(categoryId);
                category.setName(cleaned);
                int updated = session.getMapper(CommonMapper.class).updateCategory(category);
                if (updated == 0) {
                    throw new ServiceException("分类不存在或已被删除。");
                }
                return category;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, "分类名称已存在：" + cleaned, "保存分类");
        }
    }

    public void delete(Integer categoryId) {
        if (categoryId == null) {
            throw new ServiceException("缺少分类 ID，无法删除。");
        }
        try {
            MyBatisUtil.inTransaction(session -> {
                CommonMapper mapper = session.getMapper(CommonMapper.class);
                int assetCount = mapper.countAssetsByCategoryId(categoryId);
                if (assetCount > 0) {
                    throw new ServiceException("该分类下还有 " + assetCount + " 个资产，无法删除！");
                }
                int deleted = mapper.deleteCategory(categoryId);
                if (deleted == 0) {
                    throw new ServiceException("分类不存在或已被删除。");
                }
                return null;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "删除分类");
        }
    }
}
