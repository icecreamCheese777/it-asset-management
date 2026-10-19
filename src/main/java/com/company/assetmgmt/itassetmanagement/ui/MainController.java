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
import javafx.collections.transformation.FilteredList;
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
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    // ================= 视图组件注入 =================
    @FXML private TableView<Asset> assetTable;
    @FXML private TableColumn<Asset, String> colAssetNo;
    @FXML private TableColumn<Asset, String> colName;
    @FXML private TableColumn<Asset, String> colCategory;
    @FXML private TableColumn<Asset, String> colDept;
    @FXML private TableColumn<Asset, String> colStatus;
    @FXML private TableColumn<Asset, String> colParams;
    @FXML private TableColumn<Asset, LocalDateTime> colCreatedAt;
    @FXML private TableColumn<Asset, LocalDateTime> colUpdatedAt;

    @FXML private TextField searchField;

    // 导航栏按钮
    @FXML private Button btnNavAsset, btnNavLog, btnNavCategory, btnNavDept;
    // 视图容器
    @FXML private VBox assetListView;
    @FXML private Label logListView, categoryView, deptView;

    // 顶部过滤区
    @FXML private FlowPane categoryFilterPane, statusFilterPane, deptFilterPane;

    // ================= 数据与状态变量 =================
    private ObservableList<Asset> assetList = FXCollections.observableArrayList();
    private FilteredList<Asset> filteredData;

    // 当前选中的过滤条件
    private Integer currentCategoryId = null;
    private String currentStatus = null;
    private Integer currentDeptId = null;

    // ================= 初始化 =================
    @FXML
    public void initialize() {
        // 1. 绑定列数据
        colAssetNo.setCellValueFactory(new PropertyValueFactory<>("assetNo"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("deptName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colParams.setCellValueFactory(new PropertyValueFactory<>("deviceParams"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        // 2. 状态列渲染为中文
        colStatus.setCellFactory(column -> new TableCell<>() {
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
                        case "SCRAPPED": setText("报废"); break;
                        default: setText(item);
                    }
                }
            }
        });

        // 3. 时间列格式化
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

        // 4. 初始化列表与绑定
        filteredData = new FilteredList<>(assetList, p -> true);
        assetTable.setItems(filteredData);

        // 5. 监听搜索框
        searchField.textProperty().addListener((observable, oldValue, newValue) -> performFilter());

        // 6. 初始化三个过滤栏
        initFilterPanes();

        // 7. 加载数据
        loadDataFromDatabase();

        // 8. 双击表格行触发修改
        assetTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && assetTable.getSelectionModel().getSelectedItem() != null) {
                handleEditAsset();
            }
        });
    }

    // ================= 导航栏切换逻辑 =================
    @FXML public void switchToAssetList() { showView(assetListView, btnNavAsset); }
    @FXML public void switchToLogList() { showView(logListView, btnNavLog); }
    @FXML public void switchToCategory() { showView(categoryView, btnNavCategory); }
    @FXML public void switchToDept() { showView(deptView, btnNavDept); }

    private void showView(javafx.scene.Node targetView, Button activeBtn) {
        // 隐藏所有视图
        assetListView.setVisible(false); assetListView.setManaged(false);
        logListView.setVisible(false); logListView.setManaged(false);
        categoryView.setVisible(false); categoryView.setManaged(false);
        deptView.setVisible(false); deptView.setManaged(false);

        // 显示目标视图
        targetView.setVisible(true); targetView.setManaged(true);

        // 重置所有按钮样式
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-font-size: 14px; -fx-padding: 10;";
        btnNavAsset.setStyle(normalStyle);
        btnNavLog.setStyle(normalStyle);
        btnNavCategory.setStyle(normalStyle);
        btnNavDept.setStyle(normalStyle);

        // 高亮当前选中的按钮
        activeBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
    }

    // ================= 数据加载与过滤 =================
    private void loadDataFromDatabase() {
        try (SqlSession session = MyBatisUtil.getSession()) {
            AssetMapper mapper = session.getMapper(AssetMapper.class);
            List<Asset> list = mapper.selectAll();
            assetList.clear();
            assetList.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFilterPanes() {
        // 1. 初始化分类过滤
        categoryFilterPane.getChildren().clear();
        ToggleGroup categoryGroup = new ToggleGroup();
        addFilterButton(categoryFilterPane, categoryGroup, "全部", null);
        try (SqlSession session = MyBatisUtil.getSession()) {
            List<Category> categories = session.getMapper(CommonMapper.class).selectAllCategories();
            for (Category c : categories) {
                addFilterButton(categoryFilterPane, categoryGroup, c.getName(), c.getId());
            }
        }
        categoryGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) oldV.setSelected(true);
            else {
                Object data = newV.getUserData();
                currentCategoryId = (data instanceof Integer) ? (Integer) data : null;
                performFilter();
            }
        });

        // 2. 初始化状态过滤
        statusFilterPane.getChildren().clear();
        ToggleGroup statusGroup = new ToggleGroup();
        addFilterButton(statusFilterPane, statusGroup, "全部", null);
        addFilterButton(statusFilterPane, statusGroup, "使用中", "IN_USE");
        addFilterButton(statusFilterPane, statusGroup, "闲置", "IDLE");
        addFilterButton(statusFilterPane, statusGroup, "待报废", "TO_BE_SCRAPPED");
        addFilterButton(statusFilterPane, statusGroup, "报废", "SCRAPPED");
        statusGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) oldV.setSelected(true);
            else {
                Object data = newV.getUserData();
                currentStatus = (data instanceof String) ? (String) data : null;
                performFilter();
            }
        });

        // 3. 初始化部门过滤
        deptFilterPane.getChildren().clear();
        ToggleGroup deptGroup = new ToggleGroup();
        addFilterButton(deptFilterPane, deptGroup, "全部", null);
        try (SqlSession session = MyBatisUtil.getSession()) {
            List<Department> depts = session.getMapper(DepartmentMapper.class).selectAll();
            for (Department d : depts) {
                addFilterButton(deptFilterPane, deptGroup, d.getName(), d.getId());
            }
        }
        deptGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) oldV.setSelected(true);
            else {
                Object data = newV.getUserData();
                currentDeptId = (data instanceof Integer) ? (Integer) data : null;
                performFilter();
            }
        });
    }

    private void addFilterButton(FlowPane pane, ToggleGroup group, String text, Object userData) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setUserData(userData);
        btn.setStyle("-fx-background-radius: 15; -fx-padding: 2 12; -fx-cursor: hand;");
        pane.getChildren().add(btn);
        if (userData == null && group.getSelectedToggle() == null) {
            btn.setSelected(true);
        }
    }

    private void performFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        filteredData.setPredicate(asset -> {
            if (currentCategoryId != null && !currentCategoryId.equals(asset.getCategoryId())) return false;
            if (currentStatus != null && !currentStatus.equals(asset.getStatus())) return false;
            if (currentDeptId != null && !currentDeptId.equals(asset.getOwnerDeptId())) return false;

            if (searchText.isEmpty()) return true;
            if (asset.getAssetNo() != null && asset.getAssetNo().toLowerCase().contains(searchText)) return true;
            if (asset.getName() != null && asset.getName().toLowerCase().contains(searchText)) return true;
            return false;
        });
    }

    // ================= 资产 CRUD =================
    @FXML
    public void handleAddAsset() {
        showAssetDialog(null);
    }

    @FXML
    public void handleEditAsset() {
        Asset selectedAsset = assetTable.getSelectionModel().getSelectedItem();
        if (selectedAsset == null) {
            showAlert("提示", "请先在表格中选中要修改的资产！");
            return;
        }
        showAssetDialog(selectedAsset);
    }

    @FXML
    public void handleDeleteAsset() {
        Asset selectedAsset = assetTable.getSelectionModel().getSelectedItem();
        if (selectedAsset == null) {
            showAlert("提示", "请先在表格中选中要删除的资产！");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要删除资产【" + selectedAsset.getName() + "】吗？\n删除后不可恢复！");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (SqlSession session = MyBatisUtil.getSession()) {
                    AssetMapper mapper = session.getMapper(AssetMapper.class);
                    mapper.deleteById(selectedAsset.getId());
                    session.commit();
                    assetList.remove(selectedAsset);
                    showAlert(Alert.AlertType.INFORMATION, "成功", "资产删除成功！");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("数据库错误", "删除失败：" + e.getMessage());
                }
            }
        });
    }

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

        try (SqlSession session = MyBatisUtil.getSession()) {
            CommonMapper commonMapper = session.getMapper(CommonMapper.class);
            categoryBox.getItems().addAll(commonMapper.selectAllCategories());
            deptBox.getItems().addAll(commonMapper.selectAllDepartments());
        }

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

        if (editAsset != null) {
            assetNoField.setText(editAsset.getAssetNo());
            nameField.setText(editAsset.getName());
            statusBox.setValue(editAsset.getStatus());
            paramsField.setText(editAsset.getDeviceParams());

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

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (assetNoField.getText().isEmpty() || nameField.getText().isEmpty()) {
                    showAlert("错误", "资产编号和名称不能为空！");
                    return null;
                }
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

        dialog.showAndWait().ifPresent(resultAsset -> {
            try (SqlSession session = MyBatisUtil.getSession()) {
                AssetMapper mapper = session.getMapper(AssetMapper.class);
                if (editAsset == null) {
                    mapper.insert(resultAsset);
                } else {
                    mapper.update(resultAsset);
                }
                session.commit();
                loadDataFromDatabase();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("数据库错误", "保存失败：" + e.getMessage());
            }
        });
    }

    // ================= 部门管理 =================
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

        Runnable refreshDept = () -> {
            try (SqlSession session = MyBatisUtil.getSession()) {
                deptList.setAll(session.getMapper(DepartmentMapper.class).selectAll());
            }
        };
        refreshDept.run();

        deptTable.setOnMouseClicked(e -> {
            Department selected = deptTable.getSelectionModel().getSelectedItem();
            if (selected != null) deptNameField.setText(selected.getName());
        });

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
        // 关闭弹窗后，刷新过滤栏和表格数据
        initFilterPanes();
        loadDataFromDatabase();
    }

    // ================= 导出与备份 =================
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
            String[] headers = {"ID", "资产编号", "名称", "类别", "部门", "状态", "设备参数", "入库时间"};
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

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

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
            showAlert(Alert.AlertType.INFORMATION, "成功", "报表导出成功！路径：" + file.getAbsolutePath());

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
                showAlert(Alert.AlertType.INFORMATION, "成功", "数据库已备份至：" + targetFile.getAbsolutePath());
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
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                showAlert(Alert.AlertType.INFORMATION, "提示", "数据库恢复成功！请手动关闭软件并重新打开。");
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("错误", "恢复失败：" + e.getMessage());
            }
        }
    }

    @FXML
    public void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出确认");
        alert.setHeaderText(null);
        alert.setContentText("退出前将自动备份数据库，确定要退出吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                File backupDir = new File("backup");
                if (!backupDir.exists()) backupDir.mkdirs();

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File backupFile = new File(backupDir, "asset_" + timestamp + ".db");

                Path source = Paths.get(DatabaseManager.getDbPath());
                Path target = backupFile.toPath();
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                File[] dbFiles = backupDir.listFiles((dir, name) -> name.endsWith(".db"));
                if (dbFiles != null && dbFiles.length > 10) {
                    java.util.Arrays.sort(dbFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    for (int i = 10; i < dbFiles.length; i++) {
                        dbFiles[i].delete();
                    }
                }
            } catch (Exception e) {
                System.err.println("自动备份失败: " + e.getMessage());
            }
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    // ================= 通用提示框 =================
    private void showAlert(String title, String content) {
        showAlert(Alert.AlertType.ERROR, title, content);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}