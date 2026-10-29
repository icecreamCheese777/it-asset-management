package com.company.assetmgmt.itassetmanagement;

import com.company.assetmgmt.itassetmanagement.controller.MainController;
import com.company.assetmgmt.itassetmanagement.infrastructure.db.DatabaseManager;
import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void init() {
        // 先保证数据库文件与表结构存在，再初始化 MyBatis。
        DatabaseManager.initDatabase();
        MyBatisUtil.getSession().close();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);

        stage.setTitle("IT 资产管理系统");
        stage.setScene(scene);

        // 窗口右上角关闭时，走与“退出系统”按钮相同的备份确认流程。
        stage.setOnCloseRequest(event -> {
            event.consume();
            MainController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.handleExit();
            }
        });
        stage.show();
    }
}
