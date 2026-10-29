package com.company.assetmgmt.itassetmanagement.service;

import com.company.assetmgmt.itassetmanagement.model.Asset;
import com.company.assetmgmt.itassetmanagement.model.AssetStatus;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资产报表导出。
 */
public class ReportService {

    private static final String[] HEADERS = {
            "ID", "资产编号", "名称", "类别", "部门", "状态",
            "使用人", "端口号", "设备参数", "入库时间"
    };
    private static final int[] COLUMN_WIDTHS = {
            8, 18, 18, 14, 14, 10, 14, 14, 32, 18
    };
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void exportAssets(List<Asset> assets, Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("资产数据");

            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }

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
                row.createCell(9).setCellValue(
                        asset.getCreatedAt() == null ? "" : asset.getCreatedAt().format(DATE_FORMAT));
            }

            try (OutputStream out = Files.newOutputStream(target)) {
                workbook.write(out);
            }
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
