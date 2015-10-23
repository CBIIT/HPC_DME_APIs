package gov.nih.nci.hpc.cli.util;

import java.io.File;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.stereotype.Component;


@Component
public class HpcConfigProperties {


	private CompositeConfiguration configuration;
	private PropertiesConfiguration pConfig;

	@PostConstruct
	private void init() {
		try {			
			String filePath = System.getProperty("user.home") + File.separator + ".hpcenv";
			File configDir = new File(filePath);
			if (!configDir.exists())
				configDir.mkdirs();
			System.out.println("Loading the properties file: " + filePath);
			pConfig = new PropertiesConfiguration(filePath+File.separator+"config.properties");
			
			
			//Reload the file when changed 
			FileChangedReloadingStrategy fileChangedReloadingStrategy = new FileChangedReloadingStrategy();
			fileChangedReloadingStrategy.setRefreshDelay(1000);
			pConfig.setReloadingStrategy(fileChangedReloadingStrategy);
			
			configuration = new CompositeConfiguration();
			configuration.addConfiguration(pConfig);
			configuration.addConfiguration(
				    new PropertiesConfiguration("hpc.properties"));
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public String getProperty(String key) {
		return (String) configuration.getProperty(key);
	}
	
	public void setProperty(String key, Object value) {
		pConfig.setProperty(key, value);
	}
	
	public void save() {
		try {
			pConfig.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

}