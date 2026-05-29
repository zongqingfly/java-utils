/*

package com.fluckyeli.useDemo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/excel")
public class ControllerDemo {

    @PostMapping("/upload")
    public String uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "请上传文件";
        }

        try {
            // 核心步骤：将 MultipartFile 转为 InputStream
            InputStream inputStream = file.getInputStream();

            // 调用纯净的 Utils
            // 假设表头在第0行，数据从第1行开始，读取所有数据(endRow=null)
            List<Employee> employees = ExcelUtils.parse(inputStream, Employee.class, 1, null);

            // 业务逻辑处理...
            employees.forEach(System.out::println);

            return "成功解析 " + employees.size() + " 行数据";

        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败: " + e.getMessage();
        }
    }
}

 */