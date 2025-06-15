package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Function: 配置类，用于创建AliOssUtil对象
 * Author: Clementine
 * Date: 2025/6/13 23:45
 */
@Configuration
@Slf4j
public class OssConfiguration {

    /**
     * 创建一个AliOssUtil对象
     * Bean注解的作用是告诉Spring这个方法会返回一个对象，这个对象应该被注册为Spring应用上下文中的一个Bean
     * ConditionalOnMissingBean注解的作用是只有当Spring容器中不存在AliOssUtil类型的Bean时，才执行这个方法来创建Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云文件上传工具类对象：{}",aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
