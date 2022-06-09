package com.start;

import com.start.intern.ClassScanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.lang.annotation.Annotation;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
//	   SpringApplication.run(Main.class, args);
		ClassScanner.findAllAnnotatedClassesInPackage("com.start", Annotation.class); // give the name of base package here .

//	   SprinklrProject.findClazz();
//	   SprinklrProject.printClassCollection();
	}
}

// http://localhost:5000/actuator/beans
// http://localhost:5000/actuator/health