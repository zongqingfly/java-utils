package com.fluckyeli;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;


/**
 * 纯 Java 实现的 Excel 工具类，不依赖 Spring
 */
public class ExcelUtils {

    /**
     * 解析 Excel 流
     *
     * @param inputStream 输入流 (调用者负责提供流，本方法会在 try-with-resources 中关闭 Workbook，流也会随之关闭)
     * @param clazz       映射的 Bean 类
     * @param startRow    数据起始行（0-based，例如表头在第0行，数据从第1行开始，则填1）
     * @param endRow      结束行（null 表示读到最后一行）
     * @param <T>         泛型
     * @return 解析后的对象列表
     */
    public static <T> List<T> parse(InputStream inputStream, Class<T> clazz, int startRow, Integer endRow) {
        List<T> resultList = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) { // 自动关闭资源

            Sheet sheet = workbook.getSheetAt(0); // 默认读取第一个 Sheet
            if (sheet == null) {
                return resultList;
            }

            int totalRows = sheet.getPhysicalNumberOfRows();
            int actualEndRow = (endRow == null || endRow > totalRows) ? totalRows : endRow;

            // 1. 解析表头 (假设第0行总是表头，用于建立映射关系)
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerMap = new HashMap<>();
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    headerMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
                }
            }

            // 2. 建立 字段 -> 列索引 的映射
            Map<Field, Integer> fieldColumnMap = new HashMap<>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                    String headerName = annotation.value();
                    if (headerMap.containsKey(headerName)) {
                        field.setAccessible(true); // 允许访问私有字段
                        fieldColumnMap.put(field, headerMap.get(headerName));
                    }
                }
            }

            // 3. 遍历数据行
            for (int i = startRow; i < actualEndRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                T instance = clazz.getDeclaredConstructor().newInstance();
                boolean hasData = false;

                for (Map.Entry<Field, Integer> entry : fieldColumnMap.entrySet()) {
                    Field field = entry.getKey();
                    Integer colIndex = entry.getValue();
                    Cell cell = row.getCell(colIndex);

                    if (cell != null) {
                        Object cellValue = convertCellValue(cell, field.getType());
                        if (cellValue != null) {
                            field.set(instance, cellValue);
                            hasData = true;
                        }
                    }
                }

                if (hasData) {
                    resultList.add(instance);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败", e);
        }

        return resultList;
    }

    /**
     * 单元格类型转换逻辑
     */
    private static Object convertCellValue(Cell cell, Class<?> fieldType) {
        DataFormatter formatter = new DataFormatter(); // POI 提供的格式化工具

        // 如果单元格为空，直接返回 null
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        // 0. Enum 枚举类型处理
        if (fieldType.isEnum()) {
            String cellString = formatter.formatCellValue(cell).trim().toUpperCase(); // 转换为大写，提高匹配容错性

            if (cellString.isEmpty()) {
                return null;
            }

            try {
                // 使用 Enum.valueOf() 进行反射转换。
                // ⚠️ 注意：这要求 Excel 中的字符串必须精确匹配（这里是匹配大写后）枚举常量的名称。
                return Enum.valueOf((Class<Enum>) fieldType, cellString);
            } catch (IllegalArgumentException e) {
                // 如果 Excel 单元格中的值在枚举中找不到，则忽略或抛出错误
                System.err.printf("警告: Excel值 '%s' 在枚举 %s 中找不到对应的常量，将返回 null.%n", cellString, fieldType.getName());
                return null;
            }
        }

        // 1. String
        if (fieldType == String.class) {
            return formatter.formatCellValue(cell);
        }

        // 2. Integer
        if (fieldType == Integer.class || fieldType == int.class) {
            String val = formatter.formatCellValue(cell);
            return (val == null || val.isEmpty()) ? null : Integer.parseInt(val);
        }

        // 3. Double
        if (fieldType == Double.class || fieldType == double.class) {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            String val = formatter.formatCellValue(cell);
            return (val == null || val.isEmpty()) ? null : Double.parseDouble(val);
        }

        // 4. BigDecimal
        if (fieldType == BigDecimal.class) {
            String val = formatter.formatCellValue(cell);
            return (val == null || val.isEmpty()) ? null : new BigDecimal(val);
        }

        // 5. Date
        if (fieldType == Date.class) {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }
        }

        return null;
    }

    /**
     * 将数据列表导出为 Workbook 对象
     *
     * @param dataList  要导出的数据列表
     * @param clazz     数据列表元素的 Class 类型
     * @param sheetName Sheet 页名称
     * @param <T>       泛型
     * @return 包含数据的 Workbook 对象
     */
    public static <T> Workbook toExcel(List<T> dataList, Class<T> clazz, String sheetName) {
        // 使用 XSSFWorkbook 支持 .xlsx 格式（更常用，最多支持 1048576 行）
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        // 1. 获取需要导出的字段和表头名称
        List<Field> annotatedFields = new ArrayList<>();
        List<String> headerNames = new ArrayList<>();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                field.setAccessible(true); // 允许访问私有字段
                annotatedFields.add(field);
                ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                headerNames.add(annotation.value());
            }
        }

        if (annotatedFields.isEmpty()) {
            throw new IllegalArgumentException("映射类 " + clazz.getName() + " 中没有找到带有 @ExcelColumn 注解的字段。");
        }

        // 2. 创建表头 (第 0 行)
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook); // 创建样式

        for (int i = 0; i < headerNames.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerNames.get(i));
            cell.setCellStyle(headerStyle);
        }

        // 3. 写入数据行
        CellStyle dateStyle = createDateStyle(workbook); // 创建日期样式
        int rowIndex = 1;

        for (T data : dataList) {
            Row dataRow = sheet.createRow(rowIndex++);

            for (int colIndex = 0; colIndex < annotatedFields.size(); colIndex++) {
                Field field = annotatedFields.get(colIndex);
                Cell cell = dataRow.createCell(colIndex);

                try {
                    Object value = field.get(data);
                    setCellValue(cell, value, dateStyle); // 设置单元格值
                } catch (IllegalAccessException e) {
                    // 通常不会发生，因为我们设置了 field.setAccessible(true)
                    cell.setCellValue("Error");
                    System.err.println("字段访问错误: " + e.getMessage());
                }
            }
        }

        // 4. 优化列宽 (可选)
        for (int i = 0; i < headerNames.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    /**
     * 根据值类型设置单元格的值
     */
    private static void setCellValue(Cell cell, Object value, CellStyle dateStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(dateStyle); // 应用日期样式
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Enum) {
            // 枚举通常导出其名称 (toString() 或 name())
            cell.setCellValue(((Enum<?>) value).name());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    // --- 辅助样式创建方法 ---
    /**
     * 创建表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * 创建日期样式
     */
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 设置日期格式为 YYYY-MM-DD
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

}