package gov.nih.nci.hpc.reports.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"gov.nih.nci.hpc.reports.*"})
public class ReportsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportsApplication.class, args);
	}
}
