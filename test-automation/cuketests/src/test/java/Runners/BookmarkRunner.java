package Runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;




@RunWith(Cucumber.class)
@CucumberOptions(
		 features="src/test/java/Bookmark/Features"
		, glue="Bookmark.Steps"
		, dryRun = false
		//, monochrome=true
//,format= {"pretty","html:test-output_1", "json:target/cucumber-reports/CucumberTestReport.json"}
,plugin = ("json:target/cucumber-reports/CucumberTestReport.json")
		)
public class BookmarkRunner {

}
