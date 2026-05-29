package com.fly.demo;

import com.fly.JsonUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtilsDemo {

    public static void main(String[] args) {
        // 示例1：简单对象转JSON
        demoSimpleObject();
        
        // 示例2：复杂对象转JSON
        demoComplexObject();
        
        // 示例3：集合转JSON
        demoCollection();
        
        // 示例4：Map转JSON
        demoMap();
        
        // 示例5：JSON字符串转对象
        demoJsonToObject();
        
        // 示例6：JSON字符串转集合
        demoJsonToList();
        
        // 示例7：打印JSON
        demoPrintJson();
    }
    
    /**
     * 示例1：简单对象转JSON
     */
    private static void demoSimpleObject() {
        System.out.println("========== 示例1：简单对象转JSON ==========");
        User user = new User("张三", 25, "zhangsan@example.com");
        String json = JsonUtils.obj2Json(user);
        System.out.println("转换后的JSON: " + json);
        System.out.println();
    }
    
    /**
     * 示例2：复杂对象转JSON
     */
    private static void demoComplexObject() {
        System.out.println("========== 示例2：复杂对象转JSON ==========");
        Address address = new Address("北京市", "朝阳区", "100000");
        User user = new User("李四", 30, "lisi@example.com");
        user.setAddress(address);
        
        String json = JsonUtils.obj2Json(user);
        System.out.println("复杂对象转换后的JSON: " + json);
        System.out.println();
    }
    
    /**
     * 示例3：集合转JSON
     */
    private static void demoCollection() {
        System.out.println("========== 示例3：集合转JSON ==========");
        List<String> names = Arrays.asList("张三", "李四", "王五");
        String json = JsonUtils.obj2Json(names);
        System.out.println("集合转JSON: " + json);
        System.out.println();
    }
    
    /**
     * 示例4：Map转JSON
     */
    private static void demoMap() {
        System.out.println("========== 示例4：Map转JSON ==========");
        Map<String, Object> map = new HashMap<>();
        map.put("name", "赵六");
        map.put("age", 28);
        map.put("city", "上海");
        
        String json = JsonUtils.obj2Json(map);
        System.out.println("Map转JSON: " + json);
        System.out.println();
    }
    
    /**
     * 示例5：JSON字符串转对象
     */
    private static void demoJsonToObject() {
        System.out.println("========== 示例5：JSON字符串转对象 ==========");
        String json = "{\"name\":\"王小明\",\"age\":22,\"email\":\"wangxm@example.com\"}";
        User user = JsonUtils.json2Obj(json, User.class);
        System.out.println("JSON转对象成功:");
        System.out.println("用户名: " + user.getName());
        System.out.println("年龄: " + user.getAge());
        System.out.println("邮箱: " + user.getEmail());
        System.out.println();
    }
    
    /**
     * 示例6：JSON字符串转集合
     */
    private static void demoJsonToList() {
        System.out.println("========== 示例6：JSON字符串转集合 ==========");
        // 注意：转List需要借助TypeReference，这里为了演示，使用数组方式
        String json = "[\"苹果\", \"香蕉\", \"橙子\"]";
        try {
            // Jackson的TypeReference方式（如果JsonUtils需要扩展）
            com.fasterxml.jackson.core.type.TypeReference<List<String>> typeRef = 
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {};
            List<String> fruits = JsonUtils.json2Obj(json, List.class);
            System.out.println("JSON转集合成功: " + fruits);
        } catch (Exception e) {
            System.out.println("注意：直接转List会有类型擦除问题");
            System.out.println("建议扩展JsonUtils支持TypeReference");
        }
        System.out.println();
    }
    
    /**
     * 示例7：打印JSON
     */
    private static void demoPrintJson() {
        System.out.println("========== 示例7：打印JSON ==========");
        User user = new User("测试用户", 18, "test@example.com");
        System.out.print("printJson方法输出: ");
        JsonUtils.printObjByJson(user);
        System.out.println();
    }
    
    // ==================== 内部测试类 ====================
    
    /**
     * 用户实体类
     */
    static class User {
        private String name;
        private int age;
        private String email;
        private Address address;
        
        public User() {}
        
        public User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
        
        // Getter 和 Setter 方法
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
    }
    
    /**
     * 地址实体类
     */
    static class Address {
        private String province;
        private String city;
        private String zipCode;
        
        public Address() {}
        
        public Address(String province, String city, String zipCode) {
            this.province = province;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        // Getter 和 Setter 方法
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    }
}