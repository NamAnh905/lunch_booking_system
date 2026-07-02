package vn.vnpost.lunchorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LunchOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(LunchOrderApplication.class, args);
    }
}