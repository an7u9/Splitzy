package org.splitzy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main application class for Expense Service
 * Handles expense creation, splitting, and balance calculations
 */
@SpringBootApplication
@EnableKafka
@ComponentScan(basePackages = {
        "org.splitzy.expense",
        "org.splitzy.common"})
public class ExpenseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseServiceApplication.class, args);
    }
}