package com.company.assetmgmt.itassetmanagement.model;

import java.time.LocalDateTime;

public class Asset {
    private Integer id;
    private String assetNo;
    private String name;
    private Integer categoryId;
    private String status;
    private Integer ownerDeptId;
    private String deviceParams;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 下面这两个不是数据库字段，是联表查询出来给界面展示用的
    private String categoryName;
    private String deptName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAssetNo() {
        return assetNo;
    }

    public void setAssetNo(String assetNo) {
        this.assetNo = assetNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOwnerDeptId() {
        return ownerDeptId;
    }

    public void setOwnerDeptId(Integer ownerDeptId) {
        this.ownerDeptId = ownerDeptId;
    }

    public String getDeviceParams() {
        return deviceParams;
    }

    public void setDeviceParams(String deviceParams) {
        this.deviceParams = deviceParams;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
