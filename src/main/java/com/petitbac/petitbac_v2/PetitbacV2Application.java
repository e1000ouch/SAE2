package com.petitbac.petitbac_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetitbacV2Application {

	public 	static void main(String[] args) {
		SpringApplication.run(PetitbacV2Application.class, args);
	}

}
