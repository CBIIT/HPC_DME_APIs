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
import pages.bulkPage;
import pages.loginPage;

public class LoginSteps {
  
  WebDriver driver = null;
  loginPage loginPage;
  bulkPage bulkPage;
  private Context context;
  
  public LoginSteps(Context context) {
    this.context = context;
  }
  
  
  @Given("Browser is open")
  public void browser_is_open() {
    System.out.println("LoginSteps: Starting Browser is open.");
    
    WebDriverManager webDriverManager = new WebDriverManager();
    driver = webDriverManager.getDriver();
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/login");
    loginPage = new loginPage(driver);
    bulkPage = new bulkPage(driver);
    System.out.println("LoginSteps: End Browser is open.");
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
    driver.navigate().to("https://fsdmel-dsapi01d.ncifcrf.gov/addbulk?init");
  }



  

  @Then("user chooses AWS")
  public void user_chooses_aws() {
    driver.findElement(By.id("bulkTypeS3")).click(); 
  }


  
  @Then("user is navigated to the DME home page")
  public void user_is_navigated_to_the_dme_home_page() {
   
    bulkPage.enterBucketName("dme-upload-bucket");
    bulkPage.enterS3Path("dme_s3_path");
    bulkPage.enterS3File("on");
    bulkPage.enterAccessKey("dme_access_key");
    bulkPage.enterSecretKey("dme_secret_key");
    bulkPage.enterRegion("us-east-1");
    
    Select basePath = new Select(driver.findElement(By.name("basePath")));
    basePath.selectByVisibleText("/FNL_SF_Archive");
    bulkPage.enterPath("path");
    
    WebElement registerButton = driver.findElement(By.id("primaryCreateButton"));
    registerButton.click();
  
  }
  
  
  @Then("user enters <bucketName> as {string}")
  public void user_enters_bucket_name_as(String bucketName) {
    bulkPage.enterBucketName(bucketName);
  }

  @Then("user enters <s3bucket> as {string}")
  public void user_enters_s3bucket_as(String s3bucket) {
    bulkPage.enterS3Path("dme_s3_path");
  }

  @Then("user enters <s3File> as {string}")
  public void user_enters_s3file_as(String enterS3File) {
    bulkPage.enterS3File("on");
  }

  @Then("user enters <accessKey> as {string}")
  public void user_enters_access_key_as(String accessKey) {
    bulkPage.enterAccessKey(accessKey);
  }

  @Then("user enters <secretKey> as {string}")
  public void user_enters_secret_key_as(String secretKey) {
    bulkPage.enterSecretKey(secretKey);
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
    bulkPage = new bulkPage(driver);
    bulkPage.enterBucketName("dme-upload-bucket");
    bulkPage.enterS3Path("dme_s3_path");
    bulkPage.enterS3File("on");
    bulkPage.enterAccessKey("dme_access_key");
    bulkPage.enterSecretKey("dme_secret_key");
    bulkPage.enterRegion("us-east-1");
    
    Select basePath = new Select(driver.findElement(By.name("basePath")));
    basePath.selectByVisibleText("/FNL_SF_Archive");
    
    //Select basePath = new Select(driver.findElement(By.name("basePath")));
    
    bulkPage.enterPath("path");
    
    WebElement registerButton = driver.findElement(By.id("primaryCreateButton"));
    registerButton.click();
    
    //bulkPage.enterS3Path("dme_s3_path");
    //WebElement radioaws = radioaws.click();
    
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

  //@After
  //public void browser_tearDown() {
  //  System.out.println("LoginSteps: browser_tearDown!!");
  //}
 

}
