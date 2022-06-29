package com.start;

import com.start.intern.ClassScanner;
import com.start.performance.PerfTracker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {

	private static final String BASE_PACKAGE= "com.start";
	public static void main(String[] args) throws InterruptedException {

		PerfTracker.PerfStats perfStats = PerfTracker.start();
		PerfTracker.in("Main: SpringApplication.run");
		Thread.sleep(10L);
		ConfigurableApplicationContext apc = SpringApplication.run(Main.class, args);
		PerfTracker.out("Main: SpringApplication.run");
		System.out.println(perfStats.stopAndGetStacked());
		ClassScanner.findAllAnnotatedClassesInPackage(BASE_PACKAGE); // give the name of base package here .


	}
}

// http://localhost:5000/actuator/beans
// http://localhost:5000/actuator/health