package com.mict.mictpicture;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.mict.mictpicture.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class MictPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(MictPictureApplication.class, args);
    }

}
