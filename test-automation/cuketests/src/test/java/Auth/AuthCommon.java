package Auth;

import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.WebDriver;
import io.cucumber.java.Before;
import managers.WebDriverManager;
import pages.bulkPage;
import pages.loginPage;
import context.Context;

public class AuthCommon {
  
  WebDriver driver = null;
  loginPage loginPage;
  bulkPage bulkPage;
  
  private Context context;
  
  public AuthCommon(Context context) {
    this.context = context;
  }
 
  
  public void browser_setup() {
    
    System.out.println("AuthCommon: Starting Browser is open.");
    
    WebDriverManager webDriverManager = new WebDriverManager();
    driver = webDriverManager.getDriver();
    
    System.out.println("AuthCommon: End Browser is open.");
    
  }  
  
  
}
