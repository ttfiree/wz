package com.lyc.wangzhan;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.lyc.wangzhan")
@MapperScan("com.lyc.wangzhan.mapper")
public class WangzhanApplication {

    public static void main(String[] args) {
        SpringApplication.run(WangzhanApplication.class, args);
    }

}
