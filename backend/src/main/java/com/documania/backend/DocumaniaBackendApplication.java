package com.documania.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocumaniaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumaniaBackendApplication.class, args);
	}

}
