package com.company.assetmgmt.itassetmanagement.model;

import java.util.Arrays;
import java.util.List;

/**
 * 资产状态。数据库存储英文编码，界面统一显示中文，避免到处复制 switch。
 */
public enum AssetStatus {

    IN_USE("使用中"),
    REPAIR("维修"),
    LENT("出借"),
    IDLE("闲置"),
    TO_BE_SCRAPPED("待报废"),
    SCRAPPED("报废");

    private final String label;

    AssetStatus(String label) {
        this.label = label;
    }

    public String code() {
        return name();
    }

    public String label() {
        return label;
    }

    public static String labelOf(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        for (AssetStatus status : values()) {
            if (status.code().equals(code)) {
                return status.label;
            }
        }
        return code;
    }

    public static boolean isValid(String code) {
        return Arrays.stream(values()).anyMatch(status -> status.code().equals(code));
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(AssetStatus::code).toList();
    }
}
