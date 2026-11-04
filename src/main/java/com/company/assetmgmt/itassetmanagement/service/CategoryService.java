package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.CommonMapper;
import com.company.assetmgmt.itassetmanagement.model.Category;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 资产分类业务。分类值用于生成资产编号，例如 lpc-12。
 */
public class CategoryService {

    private static final Pattern VALUE_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]*$");

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

    public Category create(String name, String value) {
        String cleanedName = ServiceSupport.requireText(name, "分类名称不能为空！");
        String cleanedValue = normalizeValue(value);
        try {
            return MyBatisUtil.inTransaction(session -> {
                Category category = new Category();
                category.setName(cleanedName);
                category.setValue(cleanedValue);
                session.getMapper(CommonMapper.class).insertCategory(category);
                return category;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e,
                    "分类名称或分类值已存在：" + cleanedName + " / " + cleanedValue, "保存分类");
        }
    }

    public Category rename(Integer categoryId, String name, String value) {
        if (categoryId == null) {
            throw new ServiceException("缺少分类 ID，无法修改。");
        }
        String cleanedName = ServiceSupport.requireText(name, "分类名称不能为空！");
        String cleanedValue = normalizeValue(value);

        try {
            return MyBatisUtil.inTransaction(session -> {
                CommonMapper mapper = session.getMapper(CommonMapper.class);
                Category existing = mapper.selectCategoryById(categoryId);
                if (existing == null) {
                    throw new ServiceException("分类不存在或已被删除。");
                }
                if (!Objects.equals(existing.getValue(), cleanedValue)
                        && mapper.countAssetsByCategoryId(categoryId) > 0) {
                    throw new ServiceException("该分类下已有资产，分类值不能修改！");
                }

                Category category = new Category();
                category.setId(categoryId);
                category.setName(cleanedName);
                category.setValue(cleanedValue);
                int updated = mapper.updateCategory(category);
                if (updated == 0) {
                    throw new ServiceException("分类不存在或已被删除。");
                }
                return category;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e,
                    "分类名称或分类值已存在：" + cleanedName + " / " + cleanedValue, "保存分类");
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

    private String normalizeValue(String value) {
        String cleaned = ServiceSupport.requireText(value, "分类值不能为空！")
                .toLowerCase(Locale.ROOT);
        if (!VALUE_PATTERN.matcher(cleaned).matches()) {
            throw new ServiceException("分类值只能使用小写英文、数字、- 或 _。");
        }
        return cleaned;
    }
}
