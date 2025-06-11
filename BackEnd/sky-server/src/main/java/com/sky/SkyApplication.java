package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@Slf4j
public class SkyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
        System.out.println(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("\nEEEE yyyy年M月d日 HH:mm:ss", java.util.Locale.SIMPLIFIED_CHINESE)));
        System.out.println("Spring Boot项目启动成功");
    }
}
