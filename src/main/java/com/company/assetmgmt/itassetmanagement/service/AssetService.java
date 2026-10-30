package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.AssetMapper;
import com.company.assetmgmt.itassetmanagement.mapper.LogMapper;
import com.company.assetmgmt.itassetmanagement.model.Asset;
import com.company.assetmgmt.itassetmanagement.model.AssetStatus;
import com.company.assetmgmt.itassetmanagement.model.OperationLog;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.Objects;

/**
 * 资产业务：校验、增删改、变更日志。所有写操作都在一个事务内完成。
 */
public class AssetService {

    public List<Asset> findAll() {
        return MyBatisUtil.query(session -> session.getMapper(AssetMapper.class).selectAll());
    }

    public Asset create(Asset asset) {
        normalize(asset);
        validate(asset);

        try {
            return MyBatisUtil.inTransaction(session -> {
                AssetMapper assetMapper = session.getMapper(AssetMapper.class);
                assetMapper.insert(asset);

                Asset saved = assetMapper.findById(asset.getId());
                if (saved == null) {
                    throw new ServiceException("资产保存后读取失败，请重试。");
                }
                writeLog(session, "新增", saved,
                        "新建资产，初始状态：" + AssetStatus.labelOf(saved.getStatus()));
                return saved;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, "资产编号已存在：" + asset.getAssetNo(), "保存资产");
        }
    }

    public Asset update(Asset asset) {
        if (asset.getId() == null) {
            throw new ServiceException("缺少资产 ID，无法修改。");
        }
        normalize(asset);
        validate(asset);

        try {
            return MyBatisUtil.inTransaction(session -> {
                AssetMapper assetMapper = session.getMapper(AssetMapper.class);
                Asset before = assetMapper.findById(asset.getId());
                if (before == null) {
                    throw new ServiceException("资产不存在或已被删除。");
                }

                assetMapper.update(asset);
                Asset after = assetMapper.findById(asset.getId());

                String details = describeChanges(before, after);
                if (!details.isEmpty()) {
                    writeLog(session, "修改", after, details);
                }
                return after;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, "资产编号已存在：" + asset.getAssetNo(), "保存资产");
        }
    }

    public void delete(Integer assetId) {
        if (assetId == null) {
            throw new ServiceException("缺少资产 ID，无法删除。");
        }

        try {
            MyBatisUtil.inTransaction(session -> {
                AssetMapper assetMapper = session.getMapper(AssetMapper.class);
                Asset before = assetMapper.findById(assetId);
                if (before == null) {
                    throw new ServiceException("资产不存在或已被删除。");
                }

                assetMapper.deleteById(assetId);
                writeLog(session, "删除", before, "资产被移出系统。");
                return null;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "删除资产");
        }
    }

    private void normalize(Asset asset) {
        asset.setAssetNo(ServiceSupport.trimToNull(asset.getAssetNo()));
        // 资产名称允许为空；为空时按业务约定保存为字符串 "null"。
        String name = ServiceSupport.trimToNull(asset.getName());
        asset.setName(name == null ? "null" : name);
        asset.setUserName(ServiceSupport.trimToNull(asset.getUserName()));
        asset.setPortNumber(ServiceSupport.trimToNull(asset.getPortNumber()));
        asset.setDeviceParams(ServiceSupport.trimToNull(asset.getDeviceParams()));
        asset.setStatus(ServiceSupport.trimToNull(asset.getStatus()));
    }

    private void validate(Asset asset) {
        if (asset.getAssetNo() == null) {
            throw new ServiceException("资产编号不能为空！");
        }
        if (asset.getCategoryId() == null) {
            throw new ServiceException("请选择资产类别！");
        }
        if (!AssetStatus.isValid(asset.getStatus())) {
            throw new ServiceException("无效的资产状态：" + asset.getStatus());
        }
    }

    private String describeChanges(Asset before, Asset after) {
        StringBuilder details = new StringBuilder();
        appendChange(details, "资产编号", before.getAssetNo(), after.getAssetNo());
        appendChange(details, "资产名称", before.getName(), after.getName());
        appendChange(details, "类别", categoryLabel(before), categoryLabel(after));
        appendChange(details, "部门", departmentLabel(before), departmentLabel(after));
        appendChange(details, "状态",
                AssetStatus.labelOf(before.getStatus()), AssetStatus.labelOf(after.getStatus()));
        appendChange(details, "使用人", before.getUserName(), after.getUserName());
        appendChange(details, "端口号", before.getPortNumber(), after.getPortNumber());
        appendChange(details, "设备参数", before.getDeviceParams(), after.getDeviceParams());
        return details.toString();
    }

    private void appendChange(StringBuilder details, String field, String before, String after) {
        if (!Objects.equals(before, after)) {
            details.append(field)
                    .append("从[").append(display(before)).append("]")
                    .append("变更为[").append(display(after)).append("]。");
        }
    }

    private String categoryLabel(Asset asset) {
        if (asset.getCategoryName() != null) {
            return asset.getCategoryName();
        }
        return asset.getCategoryId() == null ? null : "ID:" + asset.getCategoryId();
    }

    private String departmentLabel(Asset asset) {
        if (asset.getDeptName() != null) {
            return asset.getDeptName();
        }
        return asset.getOwnerDeptId() == null ? null : "ID:" + asset.getOwnerDeptId();
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }

    private void writeLog(SqlSession session, String action, Asset asset, String details) {
        OperationLog log = new OperationLog();
        log.setAction(action);
        log.setEntity("ASSET");
        log.setEntityId(asset.getId());
        log.setAssetNo(asset.getAssetNo());
        log.setAssetName(asset.getName());
        log.setTargetUser(asset.getUserName());
        log.setDetails(details);
        session.getMapper(LogMapper.class).insertLog(log);
    }
}
