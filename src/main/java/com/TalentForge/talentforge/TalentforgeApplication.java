package com.TalentForge.talentforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TalentforgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TalentforgeApplication.class, args);
	}

}
