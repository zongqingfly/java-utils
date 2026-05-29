package com.fly.useDemo;

import com.fly.ExcelColumn;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Product {
    
    @ExcelColumn("商品名称")
    private String name;

    @ExcelColumn("商品类别")
    private ProductCategory category;

    @ExcelColumn("价格")
    private Double price;

    @ExcelColumn("库存")
    private Integer stock;
    
    // Excel中没有这列，测试是否兼容
    private String remark; 
}

enum ProductCategory {
    ELECTRONICS,
    CLOTHING,
    FOOD,
    BOOKS
}