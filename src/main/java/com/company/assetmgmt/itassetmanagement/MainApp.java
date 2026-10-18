package com.company.assetmgmt.itassetmanagement;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.DatabaseManager;
import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void init() throws Exception {
        // 1. 确保数据库文件存在且建好了表（上一条回复的 DatabaseManager）
        DatabaseManager.initDatabase();

        // 2. 提前触发 MyBatisUtil 的静态代码块，完成 SqlSessionFactory 的初始化
        MyBatisUtil.getSession().close();
        System.out.println("MyBatis 初始化完成，准备启动 UI");
    }
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.initDatabase();
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(),  1024, 768);

        stage.setTitle("IT 资产管理系统");
        stage.setScene(scene);
        // 拦截窗口右上角的关闭按钮（红叉）
        stage.setOnCloseRequest(event -> {
            // 取消默认的关闭行为，改为调用我们在 Controller 里写的退出逻辑
            event.consume();

            // 触发菜单栏的退出逻辑
            // 因为 Controller 不好直接调用，我们可以通过事件去触发，或者这里直接再写一遍备份
            // 最优雅的方式是直接调用 controller
            MainController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.handleExit();
            }
        });
        stage.show();
    }

//    public static void main(String[] args) {
//        launch();
//    }
}