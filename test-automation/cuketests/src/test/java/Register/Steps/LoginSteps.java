package Register.Steps;

import java.util.concurrent.TimeUnit;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.en.And;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.Select;

public class LoginSteps {
  
  WebDriver driver = null;
  
  @Given("Browser is open")
  public void browser_is_open() {
    System.out.println("LoginSteps: Starting Browser is open.");
    
    String projectPath = System.getProperty("user.dir");
    System.out.println("Projectpath is: " + projectPath);
    System.setProperty("webdriver.chrome.driver", projectPath + "/src/test/java/drivers/chromedriver");
    driver = new ChromeDriver();
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
    
    System.out.println("LoginSteps: End Browser is open.");
  }

  @And("user is on google search page")
  public void user_is_on_google_search_page() {
    
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/login");
    //driver.navigate().to("https://www.google.com");
      
  }

  @When("user enters text on google search box")
  public void user_enters_text_on_google_search_box() {
    
    driver.findElement(By.name("USER")).sendKeys("schintal");
    //driver.findElement(By.name("q")).sendKeys("Sarada Chintala");
      
  }

  @When("hits Enter")
  public void hits_enter() {
    //driver.findElement(By.name("USER")).sendKeys(Keys.TABS);
    //driver.findElement(By.name("q")).sendKeys(Keys.ENTER);
  }

  @When("user enters text on password box")
  public void user_enters_text_on_password_box() {
    driver.findElement(By.name("PASSWORD")).sendKeys("xyz");
    driver.findElement(By.name("PASSWORD")).sendKeys(Keys.ENTER);
  }

  
  @Then("user is navigated to the Search Results page")
  public void user_is_navigated_to_the_search_results_page() {
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/addbulk?init");
    
    
      //WebElement radio1 = driver.findElement(By.id("vfb-7-1"));                         
      //  WebElement radio2 = driver.findElement(By.id("vfb-7-2"));                           
                
        //Radio Button1 is selected     
      //  radio1.click();         
      //  System.out.println("Radio Button Option 1 Selected"); 
     
    
    //driver.getPageSource().contains("Sarada Chintala");
    //driver.findElement(By.name("PASSWORD")).sendKeys(Keys.TABS);
    //driver.findElement(By.name("q")).sendKeys(Keys.ENTER);
    //driver.close();
    //driver.quit();
    //Select drpCountry = new Select(driver.findElement(By.name("country")));
    //drpCountry.selectByVisibleText("ANTARCTICA");
  }


}
