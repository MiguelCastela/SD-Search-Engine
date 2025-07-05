package search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * The main application class for the Meta2 application.
 * This class serves as the entry point for the Spring Boot application.
 * It is responsible for bootstrapping the application and starting the Spring context.
 */
@SpringBootApplication
@ComponentScan(basePackages = "search")
public class Meta2Application {

    public static void main(String[] args) {
		SpringApplication.run(Meta2Application.class, args);
	}

    
}
