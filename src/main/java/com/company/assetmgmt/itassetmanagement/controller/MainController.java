package com.company.assetmgmt.itassetmanagement.controller;

import com.company.assetmgmt.itassetmanagement.model.Asset;
import com.company.assetmgmt.itassetmanagement.model.AssetStatus;
import com.company.assetmgmt.itassetmanagement.model.Category;
import com.company.assetmgmt.itassetmanagement.model.Department;
import com.company.assetmgmt.itassetmanagement.model.OperationLog;
import com.company.assetmgmt.itassetmanagement.service.AssetService;
import com.company.assetmgmt.itassetmanagement.service.BackupService;
import com.company.assetmgmt.itassetmanagement.service.CategoryService;
import com.company.assetmgmt.itassetmanagement.service.DepartmentService;
import com.company.assetmgmt.itassetmanagement.service.LogService;
import com.company.assetmgmt.itassetmanagement.service.ReportService;
import com.company.assetmgmt.itassetmanagement.service.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    // ================= 业务服务 =================
    private final AssetService assetService = new AssetService();
    private final CategoryService categoryService = new CategoryService();
    private final DepartmentService departmentService = new DepartmentService();
    private final LogService logService = new LogService();
    private final BackupService backupService = new BackupService();
    private final ReportService reportService = new ReportService();

    // ================= 视图组件注入 =================
    @FXML
    private TableView<Asset> assetTable;
    @FXML
    private TableColumn<Asset, String> colAssetNo;
    @FXML
    private TableColumn<Asset, String> colName;
    @FXML
    private TableColumn<Asset, String> colCategory;
    @FXML
    private TableColumn<Asset, String> colDept;
    @FXML
    private TableColumn<Asset, String> colStatus;
    @FXML
    private TableColumn<Asset, String> colParams;
    @FXML
    private TableColumn<Asset, LocalDateTime> colCreatedAt;
    @FXML
    private TableColumn<Asset, LocalDateTime> colUpdatedAt;
    @FXML
    private TableColumn<Asset, String> colUserName;
    @FXML
    private TableColumn<Asset, String> colPortNumber;

    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> colCatId;
    @FXML
    private TableColumn<Category, String> colCatName;

    @FXML
    private VBox deptView;
    @FXML
    private TableView<Department> deptTable;
    @FXML
    private TableColumn<Department, Integer> colDeptId;
    @FXML
    private TableColumn<Department, String> colDeptName;

    private ObservableList<Department> deptList = FXCollections.observableArrayList();
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    private TextField searchField;
    @FXML
    private TableView<OperationLog> logTable;
    @FXML
    private TableColumn<OperationLog, LocalDateTime> colLogTime;
    @FXML
    private TableColumn<OperationLog, String> colLogAction, colLogAsset, colLogUser, colLogDetails;
    private ObservableList<OperationLog> logList = FXCollections.observableArrayList();

    // 导航栏按钮
    @FXML
    private Button btnNavAsset, btnNavLog, btnNavCategory, btnNavDept;
    // 视图容器
    @FXML
    private VBox assetListView;
    @FXML
    private VBox logListView;
    @FXML
    private VBox categoryView;

    // 顶部过滤区
    @FXML
    private FlowPane categoryFilterPane, statusFilterPane, deptFilterPane;


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
        colUserName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colPortNumber.setCellValueFactory(new PropertyValueFactory<>("portNumber"));

        // 2. 状态列渲染为中文
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : AssetStatus.labelOf(item));
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

        // 初始化分类表格
        colCatId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCatName.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryTable.setItems(categoryList);
        loadCategories();

        // 双击分类表格触发修改
        categoryTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && categoryTable.getSelectionModel().getSelectedItem() != null) {
                handleEditCategory();
            }
        });

        colDeptId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDeptName.setCellValueFactory(new PropertyValueFactory<>("name"));
        deptTable.setItems(deptList);
        loadDepartments();

        deptTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && deptTable.getSelectionModel().getSelectedItem() != null) {
                handleEditDept();
            }
        });
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colLogAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colLogDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colLogUser.setCellValueFactory(new PropertyValueFactory<>("targetUser"));

        // 拼接“资产名称+编号”显示
        colLogAsset.setCellValueFactory(cellData -> {
            OperationLog log = cellData.getValue();
            String display = (log.getAssetName() != null ? log.getAssetName() : "未知")
                    + " (" + (log.getAssetNo() != null ? log.getAssetNo() : "-") + ")";
            return new javafx.beans.property.SimpleStringProperty(display);
        });

        // 时间格式化Variable 'formatter' is already defined in the scope
        /*DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");*/
        colLogTime.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });
        logTable.setItems(logList);
    }

    @FXML
    public void loadLogs() {
        try {
            logList.setAll(logService.findAll());
        } catch (RuntimeException e) {
            showAlert("错误", "加载操作日志失败：" + messageOf(e));
        }
    }

    private void loadCategories() {
        try {
            categoryList.setAll(categoryService.findAll());
        } catch (RuntimeException e) {
            showAlert("错误", "加载分类失败：" + messageOf(e));
        }
    }

    private void loadDepartments() {
        try {
            deptList.setAll(departmentService.findAll());
        } catch (RuntimeException e) {
            showAlert("错误", "加载部门失败：" + messageOf(e));
        }
    }

    @FXML
    public void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新增分类");
        dialog.setHeaderText("请输入新的分类名称");
        dialog.setContentText("名称:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            try {
                categoryService.create(name);
                loadCategories();
                initFilterPanes();
            } catch (ServiceException e) {
                showAlert("错误", e.getMessage());
            }
        });
    }

    @FXML
    public void handleEditCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选中要修改的分类！");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("修改分类");
        dialog.setHeaderText("修改分类名称");
        dialog.setContentText("名称:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            try {
                categoryService.rename(selected.getId(), name);
                loadCategories();
                loadDataFromDatabase();
                initFilterPanes();
            } catch (ServiceException e) {
                showAlert("错误", e.getMessage());
            }
        });
    }

    @FXML
    public void handleDeleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选中要删除的分类！");
            return;
        }
        try {
            int assetCount = categoryService.countAssets(selected.getId());
            if (assetCount > 0) {
                showAlert("警告", "该分类下还有 " + assetCount + " 个资产，无法删除！");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "确定要删除分类【" + selected.getName() + "】吗？");
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                categoryService.delete(selected.getId());
                loadCategories();
                initFilterPanes();
            }
        } catch (ServiceException e) {
            showAlert("错误", e.getMessage());
        }
    }

    // ================= 导航栏切换逻辑 =================
    @FXML
    public void switchToAssetList() {
        showView(assetListView, btnNavAsset);
    }

    @FXML
    public void switchToLogList() {
        showView(logListView, btnNavLog);
        loadLogs();
    }

    @FXML
    public void switchToCategory() {
        showView(categoryView, btnNavCategory);
    }

    @FXML
    public void switchToDept() {
        showView(deptView, btnNavDept);
    }

    private void showView(javafx.scene.Node targetView, Button activeBtn) {
        // 隐藏所有视图
        assetListView.setVisible(false);
        assetListView.setManaged(false);
        logListView.setVisible(false);
        logListView.setManaged(false);
        categoryView.setVisible(false);
        categoryView.setManaged(false);
        deptView.setVisible(false);
        deptView.setManaged(false);

        // 显示目标视图
        targetView.setVisible(true);
        targetView.setManaged(true);

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
        try {
            assetList.setAll(assetService.findAll());
        } catch (RuntimeException e) {
            showAlert("错误", "加载资产失败：" + messageOf(e));
        }
    }

    private void initFilterPanes() {
        // 重建过滤按钮时，旧的筛选条件已经失效，统一回到“全部”。
        currentCategoryId = null;
        currentStatus = null;
        currentDeptId = null;

        List<Category> categories;
        List<Department> departments;
        try {
            categories = categoryService.findAll();
            departments = departmentService.findAll();
        } catch (RuntimeException e) {
            showAlert("错误", "加载筛选条件失败：" + messageOf(e));
            return;
        }

        // 1. 初始化分类过滤
        categoryFilterPane.getChildren().clear();
        ToggleGroup categoryGroup = new ToggleGroup();
        addFilterButton(categoryFilterPane, categoryGroup, "全部", null);
        for (Category category : categories) {
            addFilterButton(categoryFilterPane, categoryGroup, category.getName(), category.getId());
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
        for (String code : AssetStatus.codes()) {
            addFilterButton(statusFilterPane, statusGroup, AssetStatus.labelOf(code), code);
        }
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
        for (Department department : departments) {
            addFilterButton(deptFilterPane, deptGroup, department.getName(), department.getId());
        }
        deptGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) oldV.setSelected(true);
            else {
                Object data = newV.getUserData();
                currentDeptId = (data instanceof Integer) ? (Integer) data : null;
                performFilter();
            }
        });

        performFilter();
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
                try {
                    assetService.delete(selectedAsset.getId());
                    loadDataFromDatabase();
                    loadLogs();
                    showAlert(Alert.AlertType.INFORMATION, "成功", "资产删除成功！");
                } catch (ServiceException e) {
                    showAlert("删除失败", e.getMessage());
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
        TextField userNameField = new TextField();
        TextField portNumberField = new TextField();
        ComboBox<Category> categoryBox = new ComboBox<>();
        ComboBox<Department> deptBox = new ComboBox<>();
        ComboBox<String> statusBox = new ComboBox<>();
        TextField paramsField = new TextField();

        statusBox.getItems().setAll(AssetStatus.codes());
        statusBox.setValue(AssetStatus.IN_USE.code());
        statusBox.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String object) {
                return AssetStatus.labelOf(object);
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });

        List<Category> categories;
        List<Department> departments;
        try {
            categories = categoryService.findAll();
            departments = departmentService.findAll();
        } catch (RuntimeException e) {
            showAlert("错误", "加载分类/部门失败：" + messageOf(e));
            return;
        }
        if (categories.isEmpty()) {
            showAlert("提示", "请先在“分类管理”中新增资产类别！");
            return;
        }
        categoryBox.getItems().setAll(categories);
        deptBox.getItems().setAll(departments);

        categoryBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        categoryBox.setButtonCell(categoryBox.getCellFactory().call(null));

        deptBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        deptBox.setButtonCell(deptBox.getCellFactory().call(null));

        if (editAsset != null) {
            assetNoField.setText(editAsset.getAssetNo());
            nameField.setText(editAsset.getName());
            statusBox.setValue(AssetStatus.isValid(editAsset.getStatus())
                    ? editAsset.getStatus() : AssetStatus.IN_USE.code());
            paramsField.setText(editAsset.getDeviceParams());
            userNameField.setText(editAsset.getUserName() != null ? editAsset.getUserName() : "");
            portNumberField.setText(editAsset.getPortNumber() != null ? editAsset.getPortNumber() : "");

            for (Category category : categoryBox.getItems()) {
                if (category.getId().equals(editAsset.getCategoryId())) {
                    categoryBox.setValue(category);
                    break;
                }
            }
            for (Department department : deptBox.getItems()) {
                if (department.getId().equals(editAsset.getOwnerDeptId())) {
                    deptBox.setValue(department);
                    break;
                }
            }
        }

        grid.add(new Label("资产编号:"), 0, 0);
        grid.add(assetNoField, 1, 0);
        grid.add(new Label("资产名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("资产类别:"), 0, 2);
        grid.add(categoryBox, 1, 2);
        grid.add(new Label("所属部门:"), 0, 3);
        grid.add(deptBox, 1, 3);
        grid.add(new Label("状态:"), 0, 4);
        grid.add(statusBox, 1, 4);
        grid.add(new Label("设备参数:"), 0, 5);
        grid.add(paramsField, 1, 5);
        grid.add(new Label("使用人:"), 0, 6);
        grid.add(userNameField, 1, 6);
        grid.add(new Label("端口号:"), 0, 7);
        grid.add(portNumberField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) {
                return null;
            }
            String assetNo = assetNoField.getText() == null ? "" : assetNoField.getText().trim();
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (assetNo.isEmpty() || name.isEmpty()) {
                showAlert("错误", "资产编号和名称不能为空！");
                return null;
            }
            if (categoryBox.getValue() == null) {
                showAlert("错误", "请选择资产类别！");
                return null;
            }

            Asset result = new Asset();
            if (editAsset != null) {
                result.setId(editAsset.getId());
            }
            result.setAssetNo(assetNo);
            result.setName(name);
            result.setCategoryId(categoryBox.getValue().getId());
            result.setOwnerDeptId(deptBox.getValue() == null ? null : deptBox.getValue().getId());
            result.setStatus(statusBox.getValue());
            result.setDeviceParams(paramsField.getText());
            result.setUserName(userNameField.getText());
            result.setPortNumber(portNumberField.getText());
            return result;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (editAsset == null) {
                    assetService.create(result);
                } else {
                    assetService.update(result);
                }
                loadDataFromDatabase();
                loadLogs();
            } catch (ServiceException e) {
                showAlert("保存失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleAddDept() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新增部门");
        dialog.setHeaderText("请输入新的部门名称");
        dialog.setContentText("名称:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            try {
                departmentService.create(name);
                loadDepartments();
                initFilterPanes();
            } catch (ServiceException e) {
                showAlert("错误", e.getMessage());
            }
        });
    }

    @FXML
    public void handleEditDept() {
        Department selected = deptTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选中要修改的部门！");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("修改部门");
        dialog.setHeaderText("修改部门名称");
        dialog.setContentText("名称:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            try {
                departmentService.rename(selected.getId(), name);
                loadDepartments();
                loadDataFromDatabase();
                initFilterPanes();
            } catch (ServiceException e) {
                showAlert("错误", e.getMessage());
            }
        });
    }

    @FXML
    public void handleDeleteDept() {
        Department selected = deptTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选中要删除的部门！");
            return;
        }
        try {
            int assetCount = departmentService.countAssets(selected.getId());
            if (assetCount > 0) {
                showAlert("警告", "该部门下还有 " + assetCount + " 个资产，无法删除！");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "确定要删除部门【" + selected.getName() + "】吗？");
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                departmentService.delete(selected.getId());
                loadDepartments();
                initFilterPanes();
            }
        } catch (ServiceException e) {
            showAlert("错误", e.getMessage());
        }
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

        try {
            reportService.exportAssets(filteredData, file.toPath());
            showAlert(Alert.AlertType.INFORMATION, "成功", "报表导出成功！路径：" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("错误", "导出失败：" + messageOf(e));
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
                backupService.backupTo(targetFile.toPath());
                showAlert(Alert.AlertType.INFORMATION, "成功", "数据库已备份至：" + targetFile.getAbsolutePath());
            } catch (Exception e) {
                showAlert("错误", "备份失败：" + messageOf(e));
            }
        }
    }

    @FXML
    public void handleRestoreDb() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "恢复数据库会覆盖当前所有数据，且软件需要重启。确定继续吗？");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要恢复的数据库文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite 数据库", "*.db"));
        File sourceFile = fileChooser.showOpenDialog(assetTable.getScene().getWindow());

        if (sourceFile != null) {
            try {
                Path safetyBackup = backupService.restoreFrom(sourceFile.toPath());
                showAlert(Alert.AlertType.INFORMATION, "提示",
                        "数据库恢复成功！\n原数据库已备份至：\n" + safetyBackup.toAbsolutePath()
                                + "\n软件将退出，请重新打开。");
                System.exit(0);
            } catch (Exception e) {
                showAlert("错误", "恢复失败：" + messageOf(e));
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
                backupService.autoBackup();
            } catch (Exception e) {
                System.err.println("自动备份失败: " + e.getMessage());
            }
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    // ================= 通用提示框 =================
    private String messageOf(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

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