# Club Management - Backend

## 环境要求
- JDK 17
- Maven 3.8.6+（已在 pom 中通过 enforcer 限制）
- MySQL 8.0.37（Day 2 配置数据源）

## 快速开始（开发环境）
```bash
mvn spring-boot:run
```
启动后访问：
- 健康检查：GET http://localhost:8080/api/health
- 数据库连通性：GET http://localhost:8080/api/health/db
- 登录：POST http://localhost:8080/api/auth/login
- 当前用户：GET http://localhost:8080/api/auth/me

## 目录结构
```
server/
  ├─ src/main/java/com/example/club/
  │   ├─ ClubManagementApplication.java
  │   ├─ common/ApiResponse.java
  │   ├─ config/SecurityConfig.java
  │   ├─ security/
  │   │   ├─ JwtService.java
  │   │   └─ JwtAuthenticationFilter.java
  │   ├─ controller/HealthController.java
  │   ├─ controller/AuthController.java
  │   ├─ domain/User.java
  │   ├─ mapper/UserMapper.java
  │   ├─ service/AuthUserDetailsService.java
  │   └─ exception/
  │       ├─ BusinessException.java
  │       └─ GlobalExceptionHandler.java
  ├─ src/main/resources/
  │   ├─ application.yml
  │   ├─ application-dev.yml
  │   └─ mapper/
  │       └─ UserMapper.xml
  │   └─ logback-spring.xml
  └─ pom.xml
```

## 说明
- Day 1：完成项目骨架、统一返回体与全局异常处理、日志配置。
- Day 2：接入数据源、MyBatis 与分环境配置，提供数据库健康检查接口。
- Day 3：集成 Spring Security + JWT，提供登录与鉴权。

## 数据库配置（dev）
`src/main/resources/application-dev.yml`：
```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/club_management?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```
app:
  jwt:
    secret: <Base64Encoded256bitSecret>
    expireMinutes: 120
```
导入根目录下 `club_management_sql.sql` 后，再访问健康检查接口或认证接口验证连通性与鉴权。
