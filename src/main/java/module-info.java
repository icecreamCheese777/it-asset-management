module com.company.assetmgmt.itassetmanagement {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens com.company.assetmgmt.itassetmanagement to javafx.fxml;
    exports com.company.assetmgmt.itassetmanagement;
}