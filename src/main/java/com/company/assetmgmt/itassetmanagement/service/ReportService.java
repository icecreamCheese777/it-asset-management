package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.model.Asset;
import com.company.assetmgmt.itassetmanagement.model.AssetStatus;
import com.company.assetmgmt.itassetmanagement.model.OperationLog;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资产报表与维修记录导出。
 */
public class ReportService {

    private static final String[] ASSET_HEADERS = {
            "ID", "资产编号", "名称", "类别", "部门", "状态",
            "使用人", "端口号", "设备参数", "维修费用", "入库时间"
    };
    private static final int[] ASSET_WIDTHS = {
            8, 18, 18, 14, 14, 10, 14, 14, 28, 12, 18
    };

    private static final String[] REPAIR_HEADERS = {
            "发生时间", "资产编号", "资产名称", "使用人", "维修记录"
    };
    private static final int[] REPAIR_WIDTHS = {
            20, 18, 22, 14, 60
    };

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void exportAssets(List<Asset> assets, Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("资产数据");
            writeHeader(sheet, ASSET_HEADERS, ASSET_WIDTHS);

            int rowNum = 1;
            for (Asset asset : assets) {
                XSSFRow row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(asset.getId() == null ? 0 : asset.getId());
                row.createCell(1).setCellValue(text(asset.getAssetNo()));
                row.createCell(2).setCellValue(text(asset.getName()));
                row.createCell(3).setCellValue(text(asset.getCategoryName()));
                row.createCell(4).setCellValue(text(asset.getDeptName()));
                row.createCell(5).setCellValue(AssetStatus.labelOf(asset.getStatus()));
                row.createCell(6).setCellValue(text(asset.getUserName()));
                row.createCell(7).setCellValue(text(asset.getPortNumber()));
                row.createCell(8).setCellValue(text(asset.getDeviceParams()));
                row.createCell(9).setCellValue(money(asset.getRepairCost()));
                row.createCell(10).setCellValue(
                        asset.getCreatedAt() == null ? "" : asset.getCreatedAt().format(DATE_FORMAT));
            }

            writeWorkbook(workbook, target);
        }
    }

    public void exportRepairLogs(List<OperationLog> records, Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("维修记录");
            writeHeader(sheet, REPAIR_HEADERS, REPAIR_WIDTHS);

            int rowNum = 1;
            for (OperationLog log : records) {
                XSSFRow row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(
                        log.getCreatedAt() == null ? "" : log.getCreatedAt().format(DATE_FORMAT));
                row.createCell(1).setCellValue(text(log.getAssetNo()));
                row.createCell(2).setCellValue(text(log.getAssetName()));
                row.createCell(3).setCellValue(text(log.getTargetUser()));
                row.createCell(4).setCellValue(text(log.getDetails()));
            }

            writeWorkbook(workbook, target);
        }
    }

    private void writeHeader(XSSFSheet sheet, String[] headers, int[] widths) {
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private void writeWorkbook(XSSFWorkbook workbook, Path target) throws IOException {
        try (OutputStream out = Files.newOutputStream(target)) {
            workbook.write(out);
        }
    }

    private String money(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
