package com.start;

import com.start.intern.ClassScanner;
import com.start.notOfUse.ClassScannerAlt;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
//	   SpringApplication.run(Main.class, args);
//		ClassScannerAlt.findAllAnnotatedClassesInPackage("com.start", Annotation.class); // give the name of base package here .
		ClassScanner.findAllAnnotatedClassesInPackage("com.start"); // give the name of base package here .

//	   SprinklrProject.findClazz();
//	   SprinklrProject.printClassCollection();
	}
}

// http://localhost:5000/actuator/beans
// http://localhost:5000/actuator/health