package gov.nih.nci.hpc.test.dataProviders;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import com.google.gson.Gson;

import gov.nih.nci.hpc.test.common.UserRole;

public class ConfigFileReader {

	private Properties properties;
	private final String propertyFilePath = System.getProperty("user.dir") + "/configs/Configuration.properties";

	public ConfigFileReader() {
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

	public String getDriverPath() {
		String driverPath = properties.getProperty("driverPath");
		if (driverPath != null)
			return driverPath;
		else
			throw new RuntimeException("driverPath not specified in the Configuration.properties file.");
	}

	public long getImplicitlyWait() {
		String implicitlyWait = properties.getProperty("implicitlyWait");
		if (implicitlyWait != null)
			return Long.parseLong(implicitlyWait);
		else
			throw new RuntimeException("implicitlyWait not specified in the Configuration.properties file.");
	}

	public String getApplicationUrl() {
		String url = properties.getProperty("url");
		if (url != null)
			return url;
		else
			throw new RuntimeException("url not specified in the Configuration.properties file.");
	}

	public String getToken() {
		String token = properties.getProperty("token");
		if (token != null)
			return token;
		else
			throw new RuntimeException("token not specified in the Configuration.properties file.");
	}

	public String getTokenByRole(String role) {
		if (role == null || role.isEmpty()) {
			throw new RuntimeException("User Role is Empty. The Feature should define a User Role");
		}
		String token="";
		if(role.equals(UserRole.USER_ROLE)) {
			token = properties.getProperty("userToken");
		} else if (role.equals(UserRole.GROUP_ADMIN_ROLE)) {
			token = properties.getProperty("groupAdminToken");
		} else if (role.equals(UserRole.SYSTEM_ADMIN_ROLE)){
			token = properties.getProperty("systemAdminToken");
		} else {
			throw new RuntimeException("Unknown Role");
		}
		if (token != null)
			return token;
		if(role.equals(UserRole.USER_ROLE)) {
			throw new RuntimeException("userToken for Role USER not specified in the Configuration.properties file.");
		} else if (role.equals(UserRole.GROUP_ADMIN_ROLE)) {
			throw new RuntimeException("groupAdminToken for Role GROUP_ADMIN not specified in the Configuration.properties file.");
		} else if (role.equals(UserRole.SYSTEM_ADMIN_ROLE)){
			throw new RuntimeException("SystemAdminToken for Role SYSTEM_ADMIN not specified in the Configuration.properties file.");
		}
		return token;
	}

	public String getGoogleCloudToken() {
		String refreshToken = properties.getProperty("refreshToken");
		RefreshTokenPojo refreshTokenObj = new RefreshTokenPojo();
		refreshTokenObj.client_id = properties.getProperty("googleClientId");
		refreshTokenObj.client_secret = properties.getProperty("googleClientSecret");
		refreshTokenObj.refresh_token = properties.getProperty("googleRefreshToken");
		Gson gson = new Gson();
		String refreshTokenString = gson.toJson(refreshTokenObj);
		refreshTokenString = properties.getProperty("googleRefreshTokenBlock"); // TEMP CODE
		return refreshTokenString;
	}

	public String getAwsAccessKey() {
		String awsAccessKey = properties.getProperty("awsAccessKey");
		if (awsAccessKey != null)
			return awsAccessKey;
		else
			throw new RuntimeException("awsAccessKey not specified in the Configuration.properties file.");
	}

	public String getAwsSecretKey() {
		String awsSecretKey = properties.getProperty("awsSecretKey");
		if (awsSecretKey != null)
			return awsSecretKey;
		else
			throw new RuntimeException("awsSecretKey not specified in the Configuration.properties file.");
	}

	public String getAwsRegion() {
		String awsRegion = properties.getProperty("awsRegion");
		if (awsRegion != null)
			return awsRegion;
		else
			throw new RuntimeException("awsRegion not specified in the Configuration.properties file.");
	}

	public String getGoogleDriveAccessToken() {
		String googleDriveAccessToken = properties.getProperty("googleDriveAccessToken");
		if (googleDriveAccessToken != null)
			return googleDriveAccessToken;
		else
			throw new RuntimeException("googleDriveAccessToken not specified in the Configuration.properties file.");
	}

	public String getAsperaUser() {
		String dbgapUser = properties.getProperty("dbgapUser");
		if (dbgapUser != null)
			return dbgapUser;
		else
			throw new RuntimeException("dbgapUser not specified in the Configuration.properties file.");
	}

	public String getAsperaPassword() {
		String dbgapPassword = properties.getProperty("dbgapPassword");
		if (dbgapPassword != null)
			return dbgapPassword;
		else
			throw new RuntimeException("dbgapPassword not specified in the Configuration.properties file.");
	}

	public String getAsperaHost() {
		String dbgapHost = properties.getProperty("dbgapHost");
		if (dbgapHost != null)
			return dbgapHost;
		else
			throw new RuntimeException("dbgapHost not specified in the Configuration.properties file.");
	}

}
