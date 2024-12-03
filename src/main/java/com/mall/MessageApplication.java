package com.mall;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * <b><u>MessageApplication功能说明：</u></b>
 * <p></p>
 *
 * 2024/11/7 09:29
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class);
    }
}
