package com.group12.ciserver;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CiServerApplication.class, args);
	}

}
