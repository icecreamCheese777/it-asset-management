package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.AssetMapper;
import com.company.assetmgmt.itassetmanagement.mapper.CommonMapper;
import com.company.assetmgmt.itassetmanagement.mapper.LogMapper;
import com.company.assetmgmt.itassetmanagement.model.Asset;
import com.company.assetmgmt.itassetmanagement.model.AssetStatus;
import com.company.assetmgmt.itassetmanagement.model.Category;
import com.company.assetmgmt.itassetmanagement.model.OperationLog;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

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
                CommonMapper commonMapper = session.getMapper(CommonMapper.class);

                Category category = commonMapper.selectCategoryById(asset.getCategoryId());
                String prefix = categoryPrefix(category);

                // 先插入一个唯一占位编号，拿到自增 ID 后再生成正式编号。
                asset.setAssetNo("__pending__" + UUID.randomUUID());
                assetMapper.insert(asset);

                String assetNo = prefix + "-" + asset.getId();
                int updated = assetMapper.updateAssetNo(asset.getId(), assetNo);
                if (updated == 0) {
                    throw new ServiceException("资产编号生成失败，请重试。");
                }

                Asset saved = assetMapper.findById(asset.getId());
                if (saved == null) {
                    throw new ServiceException("资产保存后读取失败，请重试。");
                }
                writeLog(session, "新增", saved,
                        "新建资产，初始状态：" + AssetStatus.labelOf(saved.getStatus()));
                if (AssetStatus.REPAIR.code().equals(saved.getStatus())) {
                    writeLog(session, "维修", saved,
                            "资产送修，维修费用：" + money(saved.getRepairCost()) + "。");
                }
                return saved;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "保存资产");
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
                CommonMapper commonMapper = session.getMapper(CommonMapper.class);
                Asset before = assetMapper.findById(asset.getId());
                if (before == null) {
                    throw new ServiceException("资产不存在或已被删除。");
                }

                Category category = commonMapper.selectCategoryById(asset.getCategoryId());
                String prefix = categoryPrefix(category);
                asset.setAssetNo(prefix + "-" + asset.getId());

                assetMapper.update(asset);
                Asset after = assetMapper.findById(asset.getId());

                String details = describeChanges(before, after);
                if (!details.isEmpty()) {
                    writeLog(session, "修改", after, details);
                }
                writeRepairLogIfNeeded(session, before, after);
                return after;
            });
        } catch (RuntimeException e) {
            throw ServiceSupport.translate(e, null, "保存资产");
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

    private String categoryPrefix(Category category) {
        if (category == null) {
            throw new ServiceException("资产类别不存在或已被删除。");
        }
        String value = ServiceSupport.trimToNull(category.getValue());
        if (value == null) {
            throw new ServiceException("该分类未配置分类值，无法生成资产编号！");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private void normalize(Asset asset) {
        // 资产编号由分类值和数据库 ID 自动生成，这里不再处理。
        // 资产名称允许为空；为空时按业务约定保存为字符串 "null"。
        String name = ServiceSupport.trimToNull(asset.getName());
        asset.setName(name == null ? "null" : name);
        asset.setUserName(ServiceSupport.trimToNull(asset.getUserName()));
        asset.setPortNumber(ServiceSupport.trimToNull(asset.getPortNumber()));
        asset.setDeviceParams(ServiceSupport.trimToNull(asset.getDeviceParams()));
        asset.setStatus(ServiceSupport.trimToNull(asset.getStatus()));

        if (AssetStatus.IDLE.code().equals(asset.getStatus())) {
            // 闲置资产不归属任何部门。
            asset.setOwnerDeptId(null);
        }

        if (AssetStatus.REPAIR.code().equals(asset.getStatus())) {
            if (asset.getRepairCost() != null) {
                asset.setRepairCost(asset.getRepairCost().setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            // 只有维修状态才保留维修费用，其他状态一律清空。
            asset.setRepairCost(null);
        }
    }

    private void validate(Asset asset) {
        if (asset.getCategoryId() == null) {
            throw new ServiceException("请选择资产类别！");
        }
        if (!AssetStatus.isValid(asset.getStatus())) {
            throw new ServiceException("无效的资产状态：" + asset.getStatus());
        }
        if (AssetStatus.REPAIR.code().equals(asset.getStatus())) {
            if (asset.getRepairCost() == null) {
                throw new ServiceException("请填写维修费用！");
            }
            if (asset.getRepairCost().signum() < 0) {
                throw new ServiceException("维修费用不能为负数！");
            }
        }
        if (AssetStatus.LENT.code().equals(asset.getStatus()) && asset.getUserName() == null) {
            throw new ServiceException("出借状态必须填写使用人！");
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
        appendCostChange(details, "维修费用", before.getRepairCost(), after.getRepairCost());
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

    private void appendCostChange(StringBuilder details, String field,
                                  BigDecimal before, BigDecimal after) {
        if (!sameMoney(before, after)) {
            details.append(field)
                    .append("从[").append(moneyOrNone(before)).append("]")
                    .append("变更为[").append(moneyOrNone(after)).append("]。");
        }
    }

    private boolean sameMoney(BigDecimal before, BigDecimal after) {
        if (before == null || after == null) {
            return before == after;
        }
        return before.compareTo(after) == 0;
    }

    private String money(BigDecimal value) {
        return value == null ? "未填写"
                : value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " 元";
    }

    private String moneyOrNone(BigDecimal value) {
        return value == null ? "无" : money(value);
    }

    private void writeRepairLogIfNeeded(SqlSession session, Asset before, Asset after) {
        boolean wasRepair = AssetStatus.REPAIR.code().equals(before.getStatus());
        boolean isRepair = AssetStatus.REPAIR.code().equals(after.getStatus());

        if (!wasRepair && isRepair) {
            writeLog(session, "维修", after,
                    "资产送修，维修费用：" + money(after.getRepairCost()) + "。");
        } else if (wasRepair && isRepair
                && !sameMoney(before.getRepairCost(), after.getRepairCost())) {
            writeLog(session, "维修", after,
                    "维修费用从[" + moneyOrNone(before.getRepairCost()) + "]变更为["
                            + moneyOrNone(after.getRepairCost()) + "]。");
        } else if (wasRepair && !isRepair) {
            writeLog(session, "维修", after,
                    "维修结束，状态变更为[" + AssetStatus.labelOf(after.getStatus())
                            + "]，维修费用：" + money(before.getRepairCost()) + "。");
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
