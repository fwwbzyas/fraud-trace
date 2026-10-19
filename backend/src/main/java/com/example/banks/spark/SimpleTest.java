package com.example.banks.spark;

/**
 * 最简单的测试 - 验证基本功能
 */
public class SimpleTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 开始简单测试");
        
        try {
            // 测试 PostgreSQL 驱动
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ PostgreSQL 驱动加载成功");
            
            // 测试 Spark 类
            Class.forName("org.apache.spark.sql.SparkSession");
            System.out.println("✅ Spark 类加载成功");
            
            System.out.println("🎉 基本测试通过！");
            System.out.println("💡 现在可以尝试运行 Spring Boot 应用");
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ 类加载失败: " + e.getMessage());
            System.err.println("💡 请检查依赖是否正确");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
