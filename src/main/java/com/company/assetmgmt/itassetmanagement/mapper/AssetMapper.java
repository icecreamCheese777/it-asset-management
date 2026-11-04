package com.company.assetmgmt.itassetmanagement.mapper;

import com.company.assetmgmt.itassetmanagement.model.Asset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AssetMapper {

    /** 查询所有资产，并带上类别名称和部门名称。 */
    List<Asset> selectAll();

    /** 按 ID 查询，联表带出类别名称和部门名称。 */
    Asset findById(Integer id);

    /** 新增资产。 */
    int insert(Asset asset);

    /** 回填根据分类值和数据库 ID 生成的资产编号。 */
    int updateAssetNo(@Param("id") Integer id, @Param("assetNo") String assetNo);

    /** 根据 ID 删除资产。 */
    int deleteById(Integer id);

    /** 根据 ID 更新资产。 */
    int update(Asset asset);
}
