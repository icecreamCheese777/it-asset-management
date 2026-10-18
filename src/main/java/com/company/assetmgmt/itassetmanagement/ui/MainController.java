package com.company.assetmgmt.itassetmanagement.ui;

import com.company.assetmgmt.itassetmanagement.infrastructure.db.DatabaseManager;
import com.company.assetmgmt.itassetmanagement.infrastructure.db.MyBatisUtil;
import com.company.assetmgmt.itassetmanagement.mapper.AssetMapper;
import com.company.assetmgmt.itassetmanagement.mapper.CommonMapper;
import com.company.assetmgmt.itassetmanagement.mapper.DepartmentMapper;
import com.company.assetmgmt.itassetmanagement.model.Asset;
import com.company.assetmgmt.itassetmanagement.model.Category;
import com.company.assetmgmt.itassetmanagement.model.Department;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.ibatis.session.SqlSession;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainController {

    @FXML private TableView<Asset> assetTable;
    @FXML private TableColumn<Asset, String> colAssetNo;
    @FXML private TableColumn<Asset, String> colName;
    @FXML private TableColumn<Asset, String> colCategory;
    @FXML private TableColumn<Asset, String> colDept;
    @FXML private TableColumn<Asset, String> colStatus;
    @FXML private TableColumn<Asset, String> colParams;
    @FXML private TableColumn<Asset, LocalDateTime> colCreatedAt;
    @FXML private TableColumn<Asset, LocalDateTime> colUpdatedAt;
    @FXML private FlowPane deptFilterPane; // 新增的流式布局

    private Integer currentDeptId = null;  // 当前选中的部门ID，null表示“全部”
    private ToggleGroup deptToggleGroup;   // 按钮组，保证同一时间只能选中一个部门

    private ObservableList<Asset> assetList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colAssetNo.setCellValueFactory(new PropertyValueFactory<>("assetNo"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("deptName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status")); // 必须加！
        colParams.setCellValueFactory(new PropertyValueFactory<>("deviceParams"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        // 把状态列渲染成中文// 2. 状态中文翻译器
        colStatus.setCellFactory(column -> new TableCell<Asset, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "IN_USE": setText("使用中"); break;
                        case "IDLE": setText("闲置"); break;
                        case "TO_BE_SCRAPPED": setText("待报废"); break;
                        case "SCRAPPED": setText("报废"); break; // 把原来的“已报废”改成“报废”更精简
                        default: setText(item);
                    }
                }
            }
        });

        // 1. 用原始列表创建过滤列表
        filteredData = new FilteredList<>(assetList, p -> true); // 初始状态全显示
        // 2. 将过滤列表绑定到表格
        assetTable.setItems(filteredData);
        // 3. 监听搜索框的输入变化
        searchField.textProperty().addListener((observable, oldValue, newValue) -> performFilter());
        // 4. 加载数据
        loadDataFromDatabase();
        // 初始化部门按钮
        loadDeptButtons();

        // 格式化日期显示
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        colCreatedAt.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        colUpdatedAt.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        // 双击表格行触发修改
        assetTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && assetTable.getSelectionModel().getSelectedItem() != null) {
                handleEditAsset();
            }
        });
    }

    private void loadDataFromDatabase() {
        try (SqlSession session = MyBatisUtil.getSession()) {
            AssetMapper mapper = session.getMapper(AssetMapper.class);
            List<Asset> list = mapper.selectAll();

            // 注意：这里更新的是 assetList 基础列表
            assetList.clear();
            assetList.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 点击“新增资产”按钮触发的方法
    @FXML
    public void handleAddAsset() {
        showAssetDialog(null); // 传 null 代表新增
    }
    @FXML
    public void handleEditAsset() {
        // 获取选中的行
        Asset selectedAsset = assetTable.getSelectionModel().getSelectedItem();
        if (selectedAsset == null) {
            showAlert("提示", "请先在表格中选中要修改的资产！");
            return;
        }
        showAssetDialog(selectedAsset); // 传入选中对象代表修改
    }
    // 点击“删除”按钮触发的方法
    @FXML
    public void handleDeleteAsset() {
        // 1. 获取当前表格中被选中的那一行数据
        Asset selectedAsset = assetTable.getSelectionModel().getSelectedItem();

        // 2. 如果没有选中任何行，弹窗提示
        if (selectedAsset == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("请先在表格中选中要删除的资产！");
            alert.showAndWait();
            return;
        }

        // 3. 弹出确认框，防止用户误删
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要删除资产【" + selectedAsset.getName() + "】吗？\n删除后不可恢复！");

        // 4. 如果用户点击了“确定”按钮
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (SqlSession session = MyBatisUtil.getSession()) {
                    AssetMapper mapper = session.getMapper(AssetMapper.class);

                    // 执行删除
                    mapper.deleteById(selectedAsset.getId());
                    session.commit(); // 切记：增删改必须 commit

                    // 5. 从界面列表中移除该行（无需重新查库，提高性能）
                    assetList.remove(selectedAsset);

                    // 提示删除成功
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("资产删除成功！");
                    successAlert.showAndWait();

                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("数据库错误", "删除失败：" + e.getMessage());
                }
            }
        });
    }
    // 统一的弹窗方法：editAsset 为 null 是新增，不为 null 是修改
    private void showAssetDialog(Asset editAsset) {
        Dialog<Asset> dialog = new Dialog<>();
        dialog.setTitle(editAsset == null ? "新增资产" : "修改资产");
        dialog.setHeaderText(editAsset == null ? "请填写资产信息" : "请修改资产信息");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField assetNoField = new TextField();
        TextField nameField = new TextField();
        ComboBox<Category> categoryBox = new ComboBox<>();
        ComboBox<Department> deptBox = new ComboBox<>();
        ComboBox<String> statusBox = new ComboBox<>();
        TextField paramsField = new TextField();

        statusBox.getItems().addAll("IN_USE", "IDLE", "TO_BE_SCRAPPED", "SCRAPPED");
        statusBox.setValue("IN_USE");

        // 加载下拉框数据
        try (SqlSession session = MyBatisUtil.getSession()) {
            CommonMapper commonMapper = session.getMapper(CommonMapper.class);
            categoryBox.getItems().addAll(commonMapper.selectAllCategories());
            deptBox.getItems().addAll(commonMapper.selectAllDepartments());
        }

        // 设置下拉框显示文本
        categoryBox.setCellFactory(param -> new ListCell<>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        categoryBox.setButtonCell(categoryBox.getCellFactory().call(null));

        deptBox.setCellFactory(param -> new ListCell<>() {
            @Override protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        deptBox.setButtonCell(deptBox.getCellFactory().call(null));

        // ==========================================
        // 关键点：如果是修改，把旧数据回显到输入框里
        // ==========================================
        if (editAsset != null) {
            assetNoField.setText(editAsset.getAssetNo());
            nameField.setText(editAsset.getName());
            statusBox.setValue(editAsset.getStatus());
            paramsField.setText(editAsset.getDeviceParams());

            // 下拉框回显稍微麻烦点，需要遍历找到 ID 对应的对象
            for (Category c : categoryBox.getItems()) {
                if (c.getId().equals(editAsset.getCategoryId())) { categoryBox.setValue(c); break; }
            }
            for (Department d : deptBox.getItems()) {
                if (d.getId().equals(editAsset.getOwnerDeptId())) { deptBox.setValue(d); break; }
            }
        }

        grid.add(new Label("资产编号:"), 0, 0); grid.add(assetNoField, 1, 0);
        grid.add(new Label("资产名称:"), 0, 1); grid.add(nameField, 1, 1);
        grid.add(new Label("资产类别:"), 0, 2); grid.add(categoryBox, 1, 2);
        grid.add(new Label("所属部门:"), 0, 3); grid.add(deptBox, 1, 3);
        grid.add(new Label("状态:"), 0, 4); grid.add(statusBox, 1, 4);
        grid.add(new Label("设备参数:"), 0, 5); grid.add(paramsField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // 点击保存时的处理
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (assetNoField.getText().isEmpty() || nameField.getText().isEmpty()) {
                    showAlert("错误", "资产编号和名称不能为空！");
                    return null;
                }
                // 如果是新增，创建新对象；如果是修改，直接在原对象上改
                Asset resultAsset = (editAsset == null) ? new Asset() : editAsset;

                resultAsset.setAssetNo(assetNoField.getText());
                resultAsset.setName(nameField.getText());
                resultAsset.setCategoryId(categoryBox.getValue() != null ? categoryBox.getValue().getId() : null);
                resultAsset.setOwnerDeptId(deptBox.getValue() != null ? deptBox.getValue().getId() : null);
                resultAsset.setStatus(statusBox.getValue());
                resultAsset.setDeviceParams(paramsField.getText());
                return resultAsset;
            }
            return null;
        });

        // 执行数据库操作并刷新
        dialog.showAndWait().ifPresent(resultAsset -> {
            try (SqlSession session = MyBatisUtil.getSession()) {
                AssetMapper mapper = session.getMapper(AssetMapper.class);
                if (editAsset == null) {
                    mapper.insert(resultAsset); // 新增
                } else {
                    mapper.update(resultAsset); // 修改
                }
                session.commit();
                loadDataFromDatabase(); // 刷新表格
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("数据库错误", "保存失败：" + e.getMessage());
            }
        });
    }
    @FXML private TextField searchField;

    private FilteredList<Asset> filteredData; // 新增：过滤后的列表

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    @FXML
    public void handleManageDept() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("部门管理");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        TableView<Department> deptTable = new TableView<>();
        TableColumn<Department, String> colName = new TableColumn<>("部门名称");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);
        deptTable.getColumns().add(colName);
        deptTable.setPrefHeight(200);

        ObservableList<Department> deptList = FXCollections.observableArrayList();
        deptTable.setItems(deptList);

        HBox actionBox = new HBox(10);
        TextField deptNameField = new TextField();
        deptNameField.setPromptText("输入部门名称");
        Button btnAdd = new Button("新增");
        Button btnEdit = new Button("修改");
        Button btnDel = new Button("删除");
        actionBox.getChildren().addAll(deptNameField, btnAdd, btnEdit, btnDel);

        vbox.getChildren().addAll(deptTable, actionBox);
        dialog.getDialogPane().setContent(vbox);

        // 刷新列表方法
        Runnable refreshDept = () -> {
            try (SqlSession session = MyBatisUtil.getSession()) {
                deptList.setAll(session.getMapper(DepartmentMapper.class).selectAll());
            }
        };
        refreshDept.run(); // 初始加载

        // 表格点击事件回显
        deptTable.setOnMouseClicked(e -> {
            Department selected = deptTable.getSelectionModel().getSelectedItem();
            if (selected != null) deptNameField.setText(selected.getName());
        });

        // 新增
        btnAdd.setOnAction(e -> {
            String name = deptNameField.getText().trim();
            if (name.isEmpty()) return;
            try (SqlSession session = MyBatisUtil.getSession()) {
                Department d = new Department();
                d.setName(name);
                session.getMapper(DepartmentMapper.class).insert(d);
                session.commit();
                refreshDept.run();
                deptNameField.clear();
            } catch (Exception ex) { showAlert("错误", "该部门可能已存在！"); }
        });

        // 修改
        btnEdit.setOnAction(e -> {
            Department selected = deptTable.getSelectionModel().getSelectedItem();
            if (selected == null || deptNameField.getText().trim().isEmpty()) return;
            try (SqlSession session = MyBatisUtil.getSession()) {
                selected.setName(deptNameField.getText().trim());
                session.getMapper(DepartmentMapper.class).update(selected);
                session.commit();
                refreshDept.run();
                deptNameField.clear();
            } catch (Exception ex) { showAlert("错误", "修改失败"); }
        });

        // 删除
        btnDel.setOnAction(e -> {
            Department selected = deptTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try (SqlSession session = MyBatisUtil.getSession()) {
                int count = session.getMapper(DepartmentMapper.class).countAssetsByDeptId(selected.getId());
                if (count > 0) {
                    showAlert("警告", "该部门下还有 " + count + " 个资产，无法删除！");
                    return;
                }
                session.getMapper(DepartmentMapper.class).deleteById(selected.getId());
                session.commit();
                refreshDept.run();
                deptNameField.clear();
            }
        });

        dialog.showAndWait();
        // 弹窗关闭后，重新加载部门按钮和表格数据
        loadDeptButtons();
        loadDataFromDatabase();
    }

    @FXML
    public void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存资产报表");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        fileChooser.setInitialFileName("资产报表.xlsx");
        File file = fileChooser.showSaveDialog(assetTable.getScene().getWindow());
        if (file == null) return;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("资产数据");
            // 创建表头
            String[] headers = {"ID", "资产编号", "名称", "类别", "部门", "状态", "设备参数", "入库时间"};
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 遍历当前表格显示的数据（包含过滤后的数据）
            int rowNum = 1;
            for (Asset asset : filteredData) {
                XSSFRow row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(asset.getId());
                row.createCell(1).setCellValue(asset.getAssetNo());
                row.createCell(2).setCellValue(asset.getName());
                row.createCell(3).setCellValue(asset.getCategoryName());
                row.createCell(4).setCellValue(asset.getDeptName());
                row.createCell(5).setCellValue(asset.getStatus());
                row.createCell(6).setCellValue(asset.getDeviceParams() != null ? asset.getDeviceParams() : "");
                row.createCell(7).setCellValue(asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : "");
            }

            // 写入文件
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
            showAlert("成功", "报表导出成功！路径：" + file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "导出失败：" + e.getMessage());
        }
    }

    @FXML
    public void handleBackupDb() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("备份数据库");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite 数据库", "*.db"));
        fileChooser.setInitialFileName("asset_backup.db");
        File targetFile = fileChooser.showSaveDialog(assetTable.getScene().getWindow());

        if (targetFile != null) {
            try {
                Path source = Paths.get(DatabaseManager.getDbPath());
                Path target = targetFile.toPath();
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                showAlert("成功", "数据库已备份至：" + targetFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("错误", "备份失败：" + e.getMessage());
            }
        }
    }

    @FXML
    public void handleRestoreDb() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "恢复数据库会覆盖当前所有数据，且需要重启软件。确定继续吗？");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要恢复的数据库文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite 数据库", "*.db"));
        File sourceFile = fileChooser.showOpenDialog(assetTable.getScene().getWindow());

        if (sourceFile != null) {
            try {
                Path source = sourceFile.toPath();
                Path target = Paths.get(DatabaseManager.getDbPath());

                // 复制覆盖
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                // 恢复后必须提示用户重启，否则程序内存里的连接池还是旧的
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "数据库恢复成功！请手动关闭软件并重新打开。");
                alert.setHeaderText(null);
                alert.showAndWait();

                // 直接退出程序
                System.exit(0);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("错误", "恢复失败：" + e.getMessage());
            }
        }
    }
    @FXML
    public void handleExit() {
        // 1. 弹出确认框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出确认");
        alert.setHeaderText(null);
        alert.setContentText("退出前将自动备份数据库，确定要退出吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // ================= 第 1 步：执行备份 =================
                File backupDir = new File("backup");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                String timestamp = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File backupFile = new File(backupDir, "asset_" + timestamp + ".db");

                java.nio.file.Path source = java.nio.file.Paths.get(DatabaseManager.getDbPath());
                java.nio.file.Path target = backupFile.toPath();
                java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                System.out.println("✅ 数据库已自动备份至: " + backupFile.getAbsolutePath());

                // ================= 第 2 步：清理旧备份 (保留最近 10 个) =================
                // 获取备份目录下所有以 .db 结尾的文件
                File[] dbFiles = backupDir.listFiles((dir, name) -> name.endsWith(".db"));

                if (dbFiles != null && dbFiles.length > 10) {
                    // 按文件最后修改时间降序排序 (最新的排在最前面)
                    java.util.Arrays.sort(dbFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                    // 从第 11 个文件开始遍历（索引为 10），把它们删掉
                    for (int i = 10; i < dbFiles.length; i++) {
                        File oldFile = dbFiles[i];
                        if (oldFile.delete()) {
                            System.out.println("🗑️ 已清理过期备份: " + oldFile.getName());
                        } else {
                            System.err.println("⚠️ 无法删除旧备份 (文件可能被占用): " + oldFile.getName());
                        }
                    }
                }
                // =========================================================

            } catch (Exception e) {
                System.err.println("❌ 自动备份或清理数据库失败: " + e.getMessage());
                e.printStackTrace();
            }

            // 3. 关闭程序
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    // 动态加载部门按钮
    private void loadDeptButtons() {
        deptFilterPane.getChildren().clear();
        deptToggleGroup = new ToggleGroup();

        // 1. 先创建一个“全部”按钮
        ToggleButton allBtn = new ToggleButton("全部");
        allBtn.setToggleGroup(deptToggleGroup);
        allBtn.setUserData(null); // 用 null 代表不限制部门
        allBtn.setSelected(true); // 默认选中
        deptFilterPane.getChildren().add(allBtn);

        // 2. 从数据库读取所有部门，生成按钮
        try (SqlSession session = MyBatisUtil.getSession()) {
            List<Department> depts = session.getMapper(DepartmentMapper.class).selectAll();
            for (Department dept : depts) {
                ToggleButton btn = new ToggleButton(dept.getName());
                btn.setToggleGroup(deptToggleGroup);
                btn.setUserData(dept.getId()); // 把部门ID存入按钮
                deptFilterPane.getChildren().add(btn);
            }
        }

        // 3. 监听按钮点击事件
        deptToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // 防止用户再次点击已选中的按钮导致取消选中
                oldToggle.setSelected(true);
            } else {
                // 获取被选中按钮里的部门ID
                currentDeptId = (Integer) newToggle.getUserData();
                // 执行过滤
                performFilter();
            }
        });
    }

    // 统一的过滤逻辑（同时处理搜索词和部门）
    private void performFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        filteredData.setPredicate(asset -> {
            // 条件1：部门过滤
            if (currentDeptId != null) {
                if (asset.getOwnerDeptId() == null || !asset.getOwnerDeptId().equals(currentDeptId)) {
                    return false; // 如果不属于该部门，直接淘汰
                }
            }

            // 条件2：文本搜索过滤
            if (searchText.isEmpty()) {
                return true; // 搜索框为空，且部门条件已通过，保留
            }
            if (asset.getAssetNo() != null && asset.getAssetNo().toLowerCase().contains(searchText)) {
                return true;
            }
            if (asset.getName() != null && asset.getName().toLowerCase().contains(searchText)) {
                return true;
            }
            return false;
        });
    }
}