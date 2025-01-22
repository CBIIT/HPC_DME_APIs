package Runners;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
		 features="src/test/java/Security/Features/GroupManagement.feature"
		, glue="gov.nih.nci.hpc.test.Security.Steps"
		, dryRun = false
		, monochrome=true
		, plugin = {"pretty", "json:target/cucumber-reports/CucumberTestReport.json",
		 "html:target/cucumber-reports/cucumber-test-reports.html" }
		)

public class SecurityRunner {
    
}
