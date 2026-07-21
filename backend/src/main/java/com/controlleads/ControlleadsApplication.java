package com.controlleads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ControlleadsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControlleadsApplication.class, args);
	}

}
