package Runners;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
		 features="src/test/java/Download/Features/DownloadBulk.feature"
		, glue={"Download.steps"}
		, dryRun = false
		, monochrome=true
		, tags="@download"
		, plugin = {"pretty", "json:target/cucumber-reports/CucumberDownloadTestReport.json",
		 "html:target/cucumber-reports/cucumber-download-test-reports.html" }
		)

public class DownloadRunner {
    
}
