package com.company.assetmgmt.itassetmanagement;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.AssetMapper;
import com.company.assetmgmt.itassetmanagement.model.Asset;
import org.apache.ibatis.session.SqlSession;
import java.util.List;

public class TestQuery {
    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.getSession()) {
            AssetMapper mapper = session.getMapper(AssetMapper.class);
            List<Asset> assets = mapper.selectAll();
            System.out.println("查询成功，当前资产数量：" + assets.size());
            // 暂时是 0 条，因为我们还没写插入功能，但能看到这行字就说明底层全通了。
        }
    }
}