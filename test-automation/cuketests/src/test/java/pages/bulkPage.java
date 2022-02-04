package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class bulkPage {
  
  protected WebDriver driver;
  
  private By txt_bulk_bucketName= By.name("bucketName");
  private By txt_bulk_s3Path = By.name("s3Path");
  private By txt_bulk_s3File = By.name("s3File");
  private By txt_bulk_accessKey = By.name("accessKey");
  private By txt_bulk_secretKey = By.name("secretKey");
  private By txt_bulk_region = By.name("region");
  private By txt_path = By.name("path");
  
  
  
  public bulkPage(WebDriver driver) {
    
    this.driver = driver;
    
//    if (!driver.getTitle().equals("Sign In - NIH Login")) {
//      throw new IllegalStateException("This is not Login Page. The current page is " + 
//          this.driver.getCurrentUrl());
//    }
  }
  
  public void enterBucketName(String bucket) {
    
    driver.findElement(txt_bulk_bucketName).sendKeys(bucket);
      
  }
  
  public void enterS3Path(String s3Path) {
    
    driver.findElement(txt_bulk_s3Path).sendKeys(s3Path);
  }
  
  public void enterSecretKey(String secretKey) {
    
    driver.findElement(txt_bulk_secretKey).sendKeys(secretKey);
  }
  
  public void enterRegion(String region) {
    
    driver.findElement(txt_bulk_region).sendKeys(region);
  }
  
  public void enterS3File(String s3File) {
    
    WebElement cb = driver.findElement(txt_bulk_s3File);
    if (s3File == "on") {
      cb.click();
    }
    System.out.println("Clicked Check Box..");
  }
  
  public void enterAccessKey(String accessKey) {
    
    driver.findElement(txt_bulk_accessKey).sendKeys(accessKey);
  }
  
  public void enterPath(String path) {
    
    driver.findElement(txt_path).sendKeys(path);
  }
  
  public void loginValidUser(String username, String password) {
    
    //driver.findElement(txt_login_username).sendKeys(username);
    //driver.findElement(txt_login_password).sendKeys(password);
    //driver.findElement(txt_login_password).sendKeys(Keys.ENTER);  
  }
}


