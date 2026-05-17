
package com.example.club;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 社团管理系统应用程序入口类
 * 该类使用@SpringBootApplication注解标记，是Spring Boot应用程序的主入口点，
 * 负责启动整个社团管理系统的后端服务，自动配置Spring应用上下文和启用组件扫描。
 */
@SpringBootApplication
public class ClubManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClubManagementApplication.class, args);
        // 打印启动成功信息，方便调试和确认服务已正常启动
        System.out.println("项目启动成功");
    }

}


