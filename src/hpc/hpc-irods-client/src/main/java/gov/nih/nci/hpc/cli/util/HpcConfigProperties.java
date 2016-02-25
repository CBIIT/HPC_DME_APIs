package gov.nih.nci.hpc.cli.util;

import java.io.File;
import java.io.IOException;
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
	private static final String COFIG_PROPS = "config.properties";
	private static final String HPC_PROPS = "hpc.properties";

	@PostConstruct
	private void init() {
		try {			
			/*
			String filePath = System.getProperty("user.home") + File.separator + ".hpcenv" + File.separator + COFIG_PROPS;
			File configDir = new File(filePath);
			if (!configDir.exists())
			{
				if (configDir.getParentFile().mkdir()) {
					System.out.println("ADDING THE FILE");
					configDir.createNewFile();
				}	
			}
			System.out.println("Loading the properties file: " + filePath);
			pConfig = new PropertiesConfiguration(filePath);
			
			
			//Reload the file when changed 
			FileChangedReloadingStrategy fileChangedReloadingStrategy = new FileChangedReloadingStrategy();
			fileChangedReloadingStrategy.setRefreshDelay(1000);
			pConfig.setReloadingStrategy(fileChangedReloadingStrategy);
			*/
			String properties = HPC_PROPS;
			String filePath = System.getProperty("hpc.client.properties");
			if(filePath != null)
				properties = filePath;
			
			System.out.println("filePath "+filePath);
			configuration = new CompositeConfiguration();
			//configuration.addConfiguration(pConfig);
			configuration.addConfiguration(
				    new PropertiesConfiguration(properties));
		} catch (ConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
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