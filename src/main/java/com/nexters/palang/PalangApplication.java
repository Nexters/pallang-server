package com.nexters.palang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PalangApplication {

	public static void main(String[] args) {
		SpringApplication.run(PalangApplication.class, args);
	}

}
