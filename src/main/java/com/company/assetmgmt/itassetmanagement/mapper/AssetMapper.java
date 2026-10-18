package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.Asset;
import java.util.List;

public interface AssetMapper {
    // 查询所有资产，并带上类别名称和部门名称
    List<Asset> selectAll();
    // 新增资产
    int insert(Asset asset);
    // 根据 ID 删除资产
    int deleteById(Integer id);
    // 根据 ID 更新资产
    int update(Asset asset);
}