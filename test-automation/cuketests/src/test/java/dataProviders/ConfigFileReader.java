package dataProviders;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import com.google.gson.Gson;

public class ConfigFileReader {
    
    private Properties properties;
    private final String propertyFilePath= System.getProperty("user.dir") + "/configs/Configuration.properties";

    
    public ConfigFileReader(){
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(propertyFilePath));
            properties = new Properties();
            try {
                properties.load(reader);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Configuration.properties not found at " + propertyFilePath);
        }       
    }
    
    public String getDriverPath(){
        String driverPath = properties.getProperty("driverPath");
        if(driverPath!= null) return driverPath;
        else throw new RuntimeException("driverPath not specified in the Configuration.properties file.");      
    }
    
    public long getImplicitlyWait() {       
        String implicitlyWait = properties.getProperty("implicitlyWait");
        if(implicitlyWait != null) return Long.parseLong(implicitlyWait);
        else throw new RuntimeException("implicitlyWait not specified in the Configuration.properties file.");      
    }
    
    public String getApplicationUrl() {
        String url = properties.getProperty("url");
        if(url != null) return url;
        else throw new RuntimeException("url not specified in the Configuration.properties file.");
    }
    
    public String getToken() {
      String token = properties.getProperty("token");
      if(token != null) return token;
      else throw new RuntimeException("token not specified in the Configuration.properties file.");
    }

    public String getGoogleCloudToken() {
      String refreshToken = properties.getProperty("refreshToken");
      RefreshTokenPojo refreshTokenObj = new RefreshTokenPojo();
      refreshTokenObj.client_id = properties.getProperty("googleClientId");
      refreshTokenObj.client_secret = properties.getProperty("googleClientSecret");
      refreshTokenObj.refresh_token = properties.getProperty("googleRefreshToken");  
      Gson gson = new Gson();
      String refreshTokenString = gson.toJson(refreshTokenObj);
      return refreshTokenString;
    }
    
    public String getAwsAccessKey() {
      String awsAccessKey = properties.getProperty("awsAccessKey");
      if(awsAccessKey != null) return awsAccessKey;
      else throw new RuntimeException("awsAccessKey not specified in the Configuration.properties file.");
    }

    public String getAwsSecretKey() {
      String awsSecretKey = properties.getProperty("awsSecretKey");
      if(awsSecretKey != null) return awsSecretKey;
      else throw new RuntimeException("awsSecretKey not specified in the Configuration.properties file.");
    }
}
