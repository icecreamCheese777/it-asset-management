package com.company.assetmgmt.itassetmanagement.model;

public class Category {
    private Integer id;
    private String name;
    private String value;

    // 必须留一个空构造方法给 MyBatis
    public Category() {}

    // 生成 Getter 和 Setter (Alt+Insert / Cmd+N)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
