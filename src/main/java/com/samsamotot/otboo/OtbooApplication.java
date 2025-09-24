package com.samsamotot.otboo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtbooApplication {

	public static void main(String[] args) {
		SpringApplication.run(OtbooApplication.class, args);
	}

}
