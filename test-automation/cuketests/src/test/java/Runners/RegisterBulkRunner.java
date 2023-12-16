package Runners;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
		 features="src/test/java/Register/Features/RegisterBulk.feature"
		, glue="Register.Steps"
		, dryRun = false
		, monochrome=true
		, tags="@register"
		, plugin = {"pretty", "json:target/cucumber-reports/CucumberRegisterTestReport.json",
		 "html:target/cucumber-reports/cucumber-register-test-report.html" }
		)

public class RegisterBulkRunner {
    
}
