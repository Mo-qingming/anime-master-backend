package cn.luopan.animemasterbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.luopan.animemasterbackend.mapper") // 配置Mapper扫描
public class AnimeMasterBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimeMasterBackendApplication.class, args);
    }

}