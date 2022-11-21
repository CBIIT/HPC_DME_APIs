package Runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;




@RunWith(Cucumber.class)
@CucumberOptions(
		 features="src/test/java/Register/Features"
		, glue={"Register.Steps", "Auth"}
		, dryRun = false
		//, monochrome=true
,plugin = ("json:target/cucumber-reports/CucumberTestReport.json")
		)
public class RegisterRunner {

}
