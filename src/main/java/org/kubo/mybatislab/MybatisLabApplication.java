package org.kubo.mybatislab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MybatisLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisLabApplication.class, args);
    }

}
