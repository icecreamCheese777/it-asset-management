module com.company.assetmgmt.itassetmanagement {
    // 导出你的 FXML 所在的包给 JavaFX
    exports com.company.assetmgmt.itassetmanagement;

    // 开放你的包给 JavaFX 反射机制 (FXML 注入需要)
    opens com.company.assetmgmt.itassetmanagement to javafx.fxml;

    // 开放 mapper 包给 MyBatis (如果你后续建了 mapper 接口包)
    opens com.company.assetmgmt.itassetmanagement.mapper to org.mybatis;

    // 声明需要的模块 (JavaFX)
    requires javafx.controls;
    requires javafx.fxml;

    // 声明需要的第三方模块 (注意：MyBatis 和 SQLite 驱动在模块化下可能需要使用自动模块名)
    requires java.sql;
    requires org.mybatis; // 或者 requires mybatis;
    requires org.xerial.sqlitejdbc; // 或者 requires sqlite.jdbc;
}