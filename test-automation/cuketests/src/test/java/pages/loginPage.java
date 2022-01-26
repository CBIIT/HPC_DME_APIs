package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

public class loginPage {
  
  protected WebDriver driver;
  
  private By txt_login_username = By.name("USER");
  private By txt_login_password = By.name("PASSWORD");
  
  
  public loginPage(WebDriver driver) {
    
    this.driver = driver;
    
    if (!driver.getTitle().equals("Sign In - NIH Login")) {
      throw new IllegalStateException("This is not Login Page. The current page is " + 
          this.driver.getCurrentUrl());
    }
  }
  
  public void enterUsername(String username) {
    
    driver.findElement(txt_login_username).sendKeys(username);
      
  }
  
  public void enterPassword(String password) {
    
    driver.findElement(txt_login_password).sendKeys(password);
    driver.findElement(txt_login_password).sendKeys(Keys.ENTER);
  }
  
  
  public void loginValidUser(String username, String password) {
    
    driver.findElement(txt_login_username).sendKeys(username);
    driver.findElement(txt_login_password).sendKeys(password);
    driver.findElement(txt_login_password).sendKeys(Keys.ENTER);  
  }
}


