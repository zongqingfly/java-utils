package com.fluckyeli.useDemo;

import com.fluckyeli.ExcelUtils;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LocalFileDemo {

    public static void main(String[] args) {
        String filePath = "assets/products.xlsx"; // 假设文件在项目根目录下

        File file = new File(filePath);

        // 检查文件是否存在
        if (!file.exists()) {
            System.err.println("错误：找不到文件 -> " + file.getAbsolutePath());
            System.out.println("请在项目根目录下创建一个名为 products.xlsx 的文件再试。");
            return;
        }

        // ================================1、解析Excel文件================================================//
        System.out.println("开始解析文件: " + file.getAbsolutePath());
        List<Product> productList = new ArrayList<>();
        // 2. 使用 try-with-resources 自动关闭流
        try (FileInputStream fis = new FileInputStream(file)) {

            // 3. 调用工具类
            // 假设第1行是表头(index=0)，数据从第2行开始(index=1)
            productList = ExcelUtils.parse(fis, Product.class, 1, null);

            // 4. 输出结果
            System.out.println("解析成功，共获取 " + productList.size() + " 条数据：");
            for (Product p : productList) {
                System.out.println(p);
            }

        } catch (FileNotFoundException e) {
            System.err.println("文件未找到");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ================================2、写入Excel文件================================================//
        String outputFileName = "assets/products_output.xlsx";
        File outputFile = new File(outputFileName);
        System.out.println("开始生成 Excel 文件...");
        // 使用 try-with-resources 确保流和 Workbook 资源被正确关闭
        try (
                // 1. 调用工具类生成 Workbook
                Workbook workbook = ExcelUtils.toExcel(productList, Product.class, "Sheet1");
                // 2. 创建文件输出流
                FileOutputStream fileOut = new FileOutputStream(outputFile)
        ) {
            // 3. 将 Workbook 内容写入输出流
            workbook.write(fileOut);

            System.out.println("✅ 文件导出成功！");
            System.out.println("文件路径: " + outputFile.getAbsolutePath());
            System.out.println("数据行数: " + productList.size());

        } catch (IOException e) {
            System.err.println("文件写入失败！");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Excel 生成过程中发生错误！");
            e.printStackTrace();
        }

    }
}