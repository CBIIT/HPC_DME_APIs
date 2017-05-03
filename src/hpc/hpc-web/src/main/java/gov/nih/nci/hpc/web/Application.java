package gov.nih.nci.hpc.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Application extends SpringBootServletInitializer{

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
  
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
      return application.sources(applicationClass);
  }

  private static Class<Application> applicationClass = Application.class;  
}
