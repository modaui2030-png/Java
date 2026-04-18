package com.ruoyi.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 程序注解配置
 *
 * @author ruoyi
 */
@Configuration
// 表示通过aop框架暴露该代理对象,AopContext能够访问
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径（分开扫描以避免同名Bean冲突）
@MapperScans({
    @MapperScan("com.ruoyi.**.mapper"),
    @MapperScan(value = "com.jsh.erp.datasource.mappers", nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
})
// 扫描ERP模块组件
@ComponentScan(basePackages = {"com.ruoyi", "com.jsh.erp"})
public class ApplicationConfig
{
}
