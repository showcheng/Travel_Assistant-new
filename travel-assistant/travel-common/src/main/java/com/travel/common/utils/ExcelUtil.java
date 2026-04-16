package com.travel.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel导出工具类
 */
@Slf4j
public class ExcelUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出Excel文件
     *
     * @param fileName     文件名
     * @param sheetName    工作表名
     * @param dataList     数据列表
     * @param response     HTTP响应
     */
    public static <T> void exportExcel(String fileName, String sheetName, List<T> dataList, HttpServletResponse response) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建数据样式
            CellStyle dataStyle = createDataStyle(workbook);

            // 如果有数据，创建表头
            if (dataList != null && !dataList.isEmpty()) {
                T firstItem = dataList.get(0);
                Field[] fields = firstItem.getClass().getDeclaredFields();

                // 创建表头行
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    field.setAccessible(true);

                    Cell headerCell = headerRow.createCell(i);
                    headerCell.setCellValue(getFieldDisplayName(field));
                    headerCell.setCellStyle(headerStyle);
                }

                // 填充数据
                for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
                    T item = dataList.get(rowIndex);
                    Row dataRow = sheet.createRow(rowIndex + 1);

                    for (int colIndex = 0; colIndex < fields.length; colIndex++) {
                        Field field = fields[colIndex];
                        field.setAccessible(true);

                        Cell dataCell = dataRow.createCell(colIndex);
                        Object value = field.get(item);
                        setCellValue(dataCell, value);
                        dataCell.setCellStyle(dataStyle);
                    }
                }

                // 自动调整列宽
                autoSizeColumns(sheet, fields.length);
            }

            // 设置响应头
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            // 写入响应流
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            log.info("Excel导出成功: fileName={}, dataSize={}", fileName, dataList != null ? dataList.size() : 0);
        } catch (Exception e) {
            log.error("Excel导出失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }

    /**
     * 创建表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // 边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 创建数据样式
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 对齐
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 设置单元格值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            if (value instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) value).doubleValue());
            } else {
                cell.setCellValue(((Number) value).doubleValue());
            }
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATE_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 获取字段显示名称
     */
    private static String getFieldDisplayName(Field field) {
        String fieldName = field.getName();
        // 简单的驼峰转下划线并转中文
        return camelToChinese(fieldName);
    }

    /**
     * 驼峰转中文
     */
    private static String camelToChinese(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append("_").append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 自动调整列宽
     */
    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // 设置最小和最大列宽
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth < 2000) {
                sheet.setColumnWidth(i, 2000);
            } else if (currentWidth > 6000) {
                sheet.setColumnWidth(i, 6000);
            }
        }
    }
}