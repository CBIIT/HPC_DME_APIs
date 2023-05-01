package Register.Steps;


import java.util.concurrent.TimeUnit;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import managers.WebDriverManager;
import io.cucumber.java.en.And;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import context.Context;
import dataProviders.ConfigFileReader;
import pages.bulkPage;
import pages.loginPage;

public class LoginSteps {
  
  ConfigFileReader configFileReader= new ConfigFileReader();
  WebDriver driver = null;
  loginPage loginPage;
  bulkPage bulkPage;
  private Context context;
  
  
  public LoginSteps(Context context) {
    this.context = context;
  }
  
  
  @Given("Browser is open")
  public void browser_is_open() {
    //System.out.println("LoginSteps: Starting Browser is open.");
    
    WebDriverManager webDriverManager = new WebDriverManager();
    driver = webDriverManager.getDriver();
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/login");
    loginPage = new loginPage(driver);
    bulkPage = new bulkPage(driver);
    //System.out.println("LoginSteps: End Browser is open.");
  }
  
  @When("user enter in username box as {string}")
  public void user_enter_in_username_box_as(String username) {
    loginPage.enterUsername(username);
  }

  @When("user enters text on password box")
  public void user_enters_text_on_password_box() {
     String pass = System.getenv("CUKEPASSWORD");
     String decodedString = pass;//new String(Base64.decodeBase64(pass));
     //System.out.println("decoded string:" + decodedString);
    
    loginPage.enterPassword(decodedString);
   

  }
  
  @Then("user is on Bulk page")
  public void user_is_on_bulk_page() {
    //driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/addbulk?init");
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/addbulk?parent=/FNL_SF_Archive/PI_Bogus_Investigator/935-testing-project/935-testing-flowcell/Run_SRR479649&source=browse&init");
  }


  @Given("user enters <awspath> as {string}")
  public void user_enters_awspath_as(String awsToPath) {
      //String path = "https://fsdmel-dsapi01d.ncifcrf.gov/addbulk?parent=" + awsToPath + "&source=browse&init";
      //driver.navigate().to(path);

  }
  

  @Then("user chooses AWS")
  public void user_chooses_aws() {
    driver.findElement(By.id("bulkTypeS3")).click(); 
  }

  
  @Then("user is clicks Register AWS")
  public void user_is_clicks_register_aws() {
    //Select basePath = new Select(driver.findElement(By.name("basePath")));
    //basePath.selectByVisibleText("/FNL_SF_Archive");
    //bulkPage.enterPath("path");
    
    WebElement registerButton = driver.findElement(By.id("primaryCreateButton"));
    registerButton.click();
  }


  
  
  @Then("user enters <bucketName> as {string}")
  public void user_enters_bucket_name_as(String bucketName) {
    bulkPage.enterBucketName(bucketName);
  }

  @Then("user enters <s3bucket> as {string}")
  public void user_enters_s3bucket_as(String s3FileName) {
    bulkPage.enterS3Path(s3FileName);
  }

  @Then("user enters <s3File> as {string}")
  public void user_enters_s3file_as(String enterS3File) {
    bulkPage.enterS3File("on");
  }
  
  @Given("user enters AWS Access Key")
  public void user_enters_aws_access_key() {
    bulkPage.enterAccessKey(configFileReader.getAwsAccessKey());
  }

  @Given("user enters AWS Secret Key")
  public void user_enters_aws_secret_key() {
    bulkPage.enterSecretKey(configFileReader.getAwsSecretKey());
  }


  @Then("user enters <region> as {string}")
  public void user_enters_region_as(String region) {
    bulkPage.enterRegion(region);
  }

  
  @Then("user is navigated to the Search Results page")
  public void user_is_navigated_to_the_search_results_page() {
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/addbulk?init");
    //s3://dme-upload-bucket/google-com.pem
    driver.findElement(By.id("bulkTypeS3")).click(); 
    
    Select basePath = new Select(driver.findElement(By.name("basePath")));
    basePath.selectByVisibleText("/FNL_SF_Archive");
    
    //Select basePath = new Select(driver.findElement(By.name("basePath")));
    
    bulkPage.enterPath("path");
    
    WebElement registerButton = driver.findElement(By.id("primaryCreateButton"));
    registerButton.click();
    
    
  }

  //@After
  //public void browser_tearDown() {
  //  System.out.println("LoginSteps: browser_tearDown!!");
  //}
 

}
