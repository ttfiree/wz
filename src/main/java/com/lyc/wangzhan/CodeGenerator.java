package com.lyc.wangzhan;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.sql.Types;
import java.util.Collections;

public class CodeGenerator {

    public static void main(String[] args) {
        // 调试输出
        System.out.println("Connecting to database: jdbc:sqlite:D:/wz/bilibili.db");

        FastAutoGenerator.create("jdbc:sqlite:D:/wz/bilibili.db", null, null)
                .globalConfig(builder -> {
                    builder.author("lyc") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .outputDir("D://"); // 指定输出目录
                })
                .packageConfig(builder ->
                        builder.parent("com.lyc") // 设置父包名
                                .moduleName("wangzhan") // 设置父包模块名
                                .pathInfo(Collections.singletonMap(OutputFile.xml, "D:\\yituhua\\wangzhan")) // 设置mapperXml生成路径
                )
                .strategyConfig(builder ->
                        builder.addInclude("video") // 设置需要生成的表名

                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }

}
