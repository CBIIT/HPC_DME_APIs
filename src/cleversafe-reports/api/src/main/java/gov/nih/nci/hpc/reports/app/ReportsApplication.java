package gov.nih.nci.hpc.reports.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"gov.nih.nci.hpc.reports.*"})
public class ReportsApplication {

	public static void main(String[] args) throws Exception {

		DisableSSLCertificateCheckUtil.disableChecks();

		SpringApplication.run(ReportsApplication.class, args);
	}
}
