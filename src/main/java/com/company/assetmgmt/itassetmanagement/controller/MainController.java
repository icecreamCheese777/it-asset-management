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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private TableColumn<Asset, BigDecimal> colRepairCost;
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
    private TableColumn<Category, String> colCatValue;

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
    @FXML
    private TableColumn<OperationLog, Void> colLogOperation;
    private ObservableList<OperationLog> logList = FXCollections.observableArrayList();

    // 维修记录
    @FXML
    private TableView<OperationLog> repairTable;
    @FXML
    private TableColumn<OperationLog, LocalDateTime> colRepairTime;
    @FXML
    private TableColumn<OperationLog, String> colRepairAsset, colRepairUser, colRepairDetails;
    private ObservableList<OperationLog> repairList = FXCollections.observableArrayList();

    // 导航栏按钮
    @FXML
    private Button btnNavAsset, btnNavLog, btnNavRepair, btnNavCategory, btnNavDept;
    // 视图容器
    @FXML
    private VBox assetListView;
    @FXML
    private VBox logListView;
    @FXML
    private VBox repairView;
    @FXML
    private VBox categoryView;

    // 顶部过滤区
    @FXML
    private FlowPane categoryFilterPane, statusFilterPane, deptFilterPane;


    // ================= 数据与状态变量 =================
    private ObservableList<Asset> assetList = FXCollections.observableArrayList();
    private FilteredList<Asset> filteredData;
    @FXML
    private Label assetCountLabel;

    // 当前选中的过滤条件
    private Integer currentCategoryId = null;
    private String currentStatus = null;
    private Integer currentDeptId = null;

    // ================= 初始化 =================
    private <T> void bindAssetColumn(TableColumn<Asset, T> column, String property) {
        if (column != null) {
            column.setCellValueFactory(new PropertyValueFactory<>(property));
        }
    }

    private String statusFill(String status) {
        return switch (status) {
            case "IN_USE" -> "#d4edda";
            case "REPAIR" -> "#ffe0b2";
            case "LENT" -> "#cce5ff";
            case "IDLE" -> "#e2e3e5";
            case "TO_BE_SCRAPPED" -> "#fff3cd";
            case "SCRAPPED" -> "#f8d7da";
            default -> "#f0f0f0";
        };
    }

    private String statusText(String status) {
        return switch (status) {
            case "IN_USE" -> "#155724";
            case "REPAIR" -> "#8a4b00";
            case "LENT" -> "#004085";
            case "IDLE" -> "#383d41";
            case "TO_BE_SCRAPPED" -> "#856404";
            case "SCRAPPED" -> "#721c24";
            default -> "#333333";
        };
    }

    @FXML
    public void initialize() {
        // 1. 绑定列数据。列在 FXML 中被注释掉时字段为 null，这里跳过即可。
        bindAssetColumn(colAssetNo, "assetNo");
        bindAssetColumn(colName, "name");
        bindAssetColumn(colCategory, "categoryName");
        bindAssetColumn(colDept, "deptName");
        bindAssetColumn(colStatus, "status");
        bindAssetColumn(colRepairCost, "repairCost");
        bindAssetColumn(colParams, "deviceParams");
        bindAssetColumn(colCreatedAt, "createdAt");
        bindAssetColumn(colUpdatedAt, "updatedAt");
        bindAssetColumn(colUserName, "userName");
        bindAssetColumn(colPortNumber, "portNumber");

        // 2. 状态列渲染为中文，并用不同底色填满单元格
        if (colStatus != null) {
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(AssetStatus.labelOf(item));
                        setStyle("-fx-background-color: " + statusFill(item)
                                + "; -fx-text-fill: " + statusText(item)
                                + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            });
        }

        if (colRepairCost != null) {
            colRepairCost.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null ? "-"
                            : item.setScale(2, RoundingMode.HALF_UP).toPlainString()));
                }
            });
        }

        // 3. 时间列格式化
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (colCreatedAt != null) {
            colCreatedAt.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.format(formatter));
                }
            });
        }
        if (colUpdatedAt != null) {
            colUpdatedAt.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.format(formatter));
                }
            });
        }

        // 4. 初始化列表与绑定
        filteredData = new FilteredList<>(assetList, p -> true);
        filteredData.addListener((ListChangeListener<Asset>) change -> updateAssetCount());
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
        colCatValue.setCellValueFactory(new PropertyValueFactory<>("value"));
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
        colLogAsset.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(logAssetLabel(cellData.getValue())));

        // 时间格式化Variable 'formatter' is already defined in the scope
        /*DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");*/
        colLogTime.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        // 每行日志末尾的删除按钮
        colLogOperation.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("删除");

            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                        + "-fx-font-size: 12px; -fx-padding: 2 8; -fx-cursor: hand;");
                deleteButton.setOnAction(event -> {
                    OperationLog log = getTableRow() == null ? null : getTableRow().getItem();
                    if (log != null) {
                        handleDeleteLog(log);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
        colLogOperation.setSortable(false);

        logTable.setItems(logList);

        // 维修记录表格
        colRepairTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colRepairAsset.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(logAssetLabel(cellData.getValue())));
        colRepairUser.setCellValueFactory(new PropertyValueFactory<>("targetUser"));
        colRepairDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colRepairTime.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });
        repairTable.setItems(repairList);

        updateAssetCount();
    }

    @FXML
    public void loadLogs() {
        try {
            logList.setAll(logService.findAll());
        } catch (RuntimeException e) {
            showAlert("错误", "加载操作日志失败：" + messageOf(e));
        }
    }

    @FXML
    public void loadRepairLogs() {
        try {
            repairList.setAll(logService.findRepairLogs());
        } catch (RuntimeException e) {
            showAlert("错误", "加载维修记录失败：" + messageOf(e));
        }
    }

    private void updateAssetCount() {
        if (assetCountLabel != null && filteredData != null) {
            assetCountLabel.setText("当前表格：" + filteredData.size() + " 条资产");
        }
    }

    private String logAssetLabel(OperationLog log) {
        if (log == null) {
            return "";
        }
        return (log.getAssetName() != null ? log.getAssetName() : "未知")
                + " (" + (log.getAssetNo() != null ? log.getAssetNo() : "-") + ")";
    }

    @FXML
    public void handleClearOldLogs() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确定要清除一年以前的所有操作日志吗？\n此操作不可恢复。");
        confirm.setTitle("清除历史日志");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            int deleted = logService.deleteOlderThanOneYear();
            loadLogs();
            showAlert(Alert.AlertType.INFORMATION, "清理完成",
                    deleted == 0 ? "没有需要清理的日志。" : "已删除 " + deleted + " 条一年以前的日志。");
        } catch (ServiceException e) {
            showAlert("清理失败", e.getMessage());
        }
    }

    private void handleDeleteLog(OperationLog log) {
        if (log == null || log.getId() == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确定要删除这条操作日志吗？\n" + log.getAction() + "：" + log.getDetails());
        confirm.setTitle("删除日志");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            logService.deleteById(log.getId());
            loadLogs();
        } catch (ServiceException e) {
            showAlert("删除失败", e.getMessage());
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
        showCategoryDialog(null);
    }

    @FXML
    public void handleEditCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请先选中要修改的分类！");
            return;
        }
        showCategoryDialog(selected);
    }

    private void showCategoryDialog(Category editCategory) {
        Dialog<Category> dialog = new Dialog<>();
        dialog.setTitle(editCategory == null ? "新增分类" : "修改分类");
        dialog.setHeaderText(editCategory == null ? "请输入分类名称和分类值" : "修改分类名称和分类值");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 80, 10, 10));

        TextField nameField = new TextField();
        TextField valueField = new TextField();
        valueField.setPromptText("如 lpc、dpc，用于生成资产编号");

        if (editCategory != null) {
            nameField.setText(editCategory.getName());
            valueField.setText(editCategory.getValue());
        }

        grid.add(new Label("分类名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("分类值:"), 0, 1);
        grid.add(valueField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String value = valueField.getText() == null ? "" : valueField.getText().trim();
            if (name.isEmpty()) {
                showAlert("保存失败", "分类名称不能为空！");
                return null;
            }
            if (value.isEmpty()) {
                showAlert("保存失败", "分类值不能为空！");
                return null;
            }
            Category category = new Category();
            category.setId(editCategory == null ? null : editCategory.getId());
            category.setName(name);
            category.setValue(value);
            return category;
        });

        dialog.showAndWait().ifPresent(category -> {
            try {
                if (editCategory == null) {
                    categoryService.create(category.getName(), category.getValue());
                } else {
                    categoryService.rename(category.getId(), category.getName(), category.getValue());
                }
                loadCategories();
                loadDataFromDatabase();
                initFilterPanes();
            } catch (ServiceException e) {
                showAlert("保存失败", e.getMessage());
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
    public void switchToRepair() {
        showView(repairView, btnNavRepair);
        loadRepairLogs();
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
        repairView.setVisible(false);
        repairView.setManaged(false);
        categoryView.setVisible(false);
        categoryView.setManaged(false);
        deptView.setVisible(false);
        deptView.setManaged(false);

        // 显示目标视图
        targetView.setVisible(true);
        targetView.setManaged(true);

        // 重置所有按钮样式
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-font-size: 13px; -fx-padding: 8;";
        btnNavAsset.setStyle(normalStyle);
        btnNavLog.setStyle(normalStyle);
        btnNavRepair.setStyle(normalStyle);
        btnNavCategory.setStyle(normalStyle);
        btnNavDept.setStyle(normalStyle);

        // 高亮当前选中的按钮
        activeBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8;");
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
            if (asset.getUserName() != null && asset.getUserName().toLowerCase().contains(searchText)) return true;
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editAsset == null ? "新增资产" : "修改资产");
        dialog.setHeaderText(editAsset == null ? "请填写资产信息" : "请修改资产信息");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField assetNoField = new TextField();
        assetNoField.setEditable(false);
        assetNoField.setFocusTraversable(false);
        assetNoField.setStyle("-fx-control-inner-background: #f2f2f2;");
        TextField nameField = new TextField();
        nameField.setPromptText("不填则保存为 null");
        TextField userNameField = new TextField();
        TextField portNumberField = new TextField();
        ComboBox<Category> categoryBox = new ComboBox<>();
        ComboBox<Department> deptBox = new ComboBox<>();
        ComboBox<String> statusBox = new ComboBox<>();
        TextField paramsField = new TextField();
        TextField repairCostField = new TextField();
        repairCostField.setPromptText("选择维修状态后自动填写");
        repairCostField.setDisable(true);

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
            repairCostField.setText(editAsset.getRepairCost() == null ? ""
                    : editAsset.getRepairCost().setScale(2, RoundingMode.HALF_UP).toPlainString());
            repairCostField.setDisable(!AssetStatus.REPAIR.code().equals(statusBox.getValue()));
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

        if (editAsset == null) {
            assetNoField.setText("保存后自动生成");
        }
        categoryBox.valueProperty().addListener((observable, oldCategory, newCategory) ->
                updateAssetNoPreview(assetNoField, newCategory, editAsset));
        updateAssetNoPreview(assetNoField, categoryBox.getValue(), editAsset);

        // 闲置状态没有部门。
        boolean initiallyIdle = AssetStatus.IDLE.code().equals(statusBox.getValue());
        if (initiallyIdle) {
            deptBox.setValue(null);
        }
        deptBox.setDisable(initiallyIdle);

        // 选择“维修”时立即弹出维修费用输入框；取消则退回原状态。
        statusBox.valueProperty().addListener((observable, oldStatus, newStatus) -> {
            if (AssetStatus.REPAIR.code().equals(newStatus)) {
                Optional<BigDecimal> cost = promptRepairCost(repairCostField.getText());
                if (cost.isPresent()) {
                    repairCostField.setText(cost.get().setScale(2, RoundingMode.HALF_UP).toPlainString());
                    repairCostField.setDisable(false);
                } else {
                    statusBox.setValue(oldStatus == null ? AssetStatus.IN_USE.code() : oldStatus);
                    repairCostField.setDisable(true);
                }
            } else {
                repairCostField.clear();
                repairCostField.setDisable(true);
            }
        });

        // 状态切换为闲置时立即清空并禁用部门；切回其它状态后恢复可选。
        statusBox.valueProperty().addListener((observable, oldStatus, newStatus) -> {
            boolean idle = AssetStatus.IDLE.code().equals(newStatus);
            if (idle) {
                deptBox.setValue(null);
            }
            deptBox.setDisable(idle);
        });

        grid.add(new Label("资产编号(自动):"), 0, 0);
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
        grid.add(new Label("维修费用:"), 0, 8);
        grid.add(repairCostField, 1, 8);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (categoryBox.getValue() == null) {
                showAlert("保存失败", "请选择资产类别！");
                event.consume();
                return;
            }

            String name = nameField.getText() == null ? "" : nameField.getText().trim();

            BigDecimal repairCost = null;
            if (AssetStatus.REPAIR.code().equals(statusBox.getValue())) {
                String costText = repairCostField.getText() == null
                        ? "" : repairCostField.getText().trim();
                if (costText.isEmpty()) {
                    showAlert("保存失败", "维修状态必须填写维修费用！");
                    event.consume();
                    return;
                }
                try {
                    repairCost = new BigDecimal(costText);
                    if (repairCost.signum() < 0) {
                        throw new NumberFormatException("negative");
                    }
                    repairCost = repairCost.setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException e) {
                    showAlert("保存失败", "请输入有效的非负维修费用，例如 120.50");
                    event.consume();
                    return;
                }
            }

            if (AssetStatus.LENT.code().equals(statusBox.getValue())) {
                String userName = userNameField.getText() == null
                        ? "" : userNameField.getText().trim();
                if (userName.isEmpty()) {
                    showAlert("保存失败", "出借状态必须填写使用人！");
                    userNameField.requestFocus();
                    event.consume();
                    return;
                }
            }

            Asset result = new Asset();
            if (editAsset != null) {
                result.setId(editAsset.getId());
            }
            result.setName(name.isEmpty() ? "null" : name);
            result.setCategoryId(categoryBox.getValue().getId());
            result.setStatus(statusBox.getValue());
            result.setDeviceParams(paramsField.getText());
            result.setRepairCost(repairCost);
            result.setUserName(userNameField.getText());
            result.setPortNumber(portNumberField.getText());
            result.setOwnerDeptId(AssetStatus.IDLE.code().equals(statusBox.getValue())
                    ? null
                    : (deptBox.getValue() == null ? null : deptBox.getValue().getId()));

            saveButton.setDisable(true);
            try {
                if (editAsset == null) {
                    assetService.create(result);
                } else {
                    assetService.update(result);
                }
                loadDataFromDatabase();
                loadLogs();
            } catch (ServiceException e) {
                event.consume();
                showAlert("保存失败", e.getMessage());
            } catch (RuntimeException e) {
                event.consume();
                showAlert("保存失败", messageOf(e));
            } finally {
                saveButton.setDisable(false);
            }
        });

        dialog.showAndWait();
    }

    private void updateAssetNoPreview(TextField field, Category category, Asset editAsset) {
        if (category == null) {
            field.setText(editAsset == null ? "保存后自动生成" : editAsset.getAssetNo());
            return;
        }
        String value = category.getValue() == null ? "" : category.getValue().trim();
        if (value.isEmpty()) {
            field.setText("该分类未配置分类值");
            return;
        }
        if (editAsset == null) {
            field.setText(value.toLowerCase() + "-（保存后生成）");
        } else {
            field.setText(value.toLowerCase() + "-" + editAsset.getId());
        }
    }

    private Optional<BigDecimal> promptRepairCost(String initial) {
        String value = initial == null ? "" : initial;
        while (true) {
            TextInputDialog dialog = new TextInputDialog(value);
            dialog.setTitle("维修费用");
            dialog.setHeaderText("请输入本次维修费用");
            dialog.setContentText("金额（元）:");

            Optional<String> input = dialog.showAndWait();
            if (input.isEmpty()) {
                return Optional.empty();
            }

            String text = input.get().trim();
            try {
                BigDecimal cost = new BigDecimal(text);
                if (cost.signum() < 0) {
                    throw new NumberFormatException("negative");
                }
                return Optional.of(cost.setScale(2, RoundingMode.HALF_UP));
            } catch (NumberFormatException e) {
                showAlert("输入错误", "请输入有效的非负金额，例如 120.50");
                value = text;
            }
        }
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

        // 以当前表格中显示的数据为准（已经应用分类、状态、部门筛选和搜索条件）。
        List<Asset> currentAssets = new ArrayList<>(assetTable.getItems());
        if (currentAssets.isEmpty()) {
            showAlert("提示", "当前表格没有可导出的资产。");
            return;
        }

        try {
            reportService.exportAssets(currentAssets, file.toPath());
            showAlert(Alert.AlertType.INFORMATION, "成功",
                    "已导出当前表格中的 " + currentAssets.size() + " 条资产。\n路径：" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("错误", "导出失败：" + messageOf(e));
        }
    }

    @FXML
    public void handleExportRepairExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存维修记录");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        fileChooser.setInitialFileName("维修记录.xlsx");
        File file = fileChooser.showSaveDialog(assetTable.getScene().getWindow());
        if (file == null) return;

        List<OperationLog> records = new ArrayList<>(repairTable.getItems());
        if (records.isEmpty()) {
            showAlert("提示", "当前没有可导出的维修记录。");
            return;
        }

        try {
            reportService.exportRepairLogs(records, file.toPath());
            showAlert(Alert.AlertType.INFORMATION, "成功",
                    "已导出 " + records.size() + " 条维修记录。\n路径：" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("错误", "导出维修记录失败：" + messageOf(e));
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