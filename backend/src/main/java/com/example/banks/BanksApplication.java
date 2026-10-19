package com.example.banks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BanksApplication {

    public static void main(String[] args) {
        System.setProperty("hadoop.home.dir", "C:\\hadoop");
        SpringApplication.run(BanksApplication.class, args);
    }

}
