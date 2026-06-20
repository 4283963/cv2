package com.payment.reconcile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ReconcileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconcileServiceApplication.class, args);
    }
}
