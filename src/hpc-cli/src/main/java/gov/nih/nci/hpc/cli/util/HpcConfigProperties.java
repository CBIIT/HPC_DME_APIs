/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.File;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.stereotype.Component;

@Component
public class HpcConfigProperties {

	private CompositeConfiguration configuration;
	private FileBasedConfigurationBuilder<PropertiesConfiguration> builder;
	private static final String COFIG_PROPS = "config.properties";
	private static final String HPC_PROPS = "hpc.properties";

	@PostConstruct
	private void init() {
		try {
			Map<String, String> env = System.getenv();
			String basePath=env.get("HPC_DM_UTILS");
			String properties = HPC_PROPS;
			String configProperties = COFIG_PROPS;
			String filePath = System.getProperty("hpc.client.properties");
			if (filePath != null)
				properties = filePath;
			
			String configFilePath = basePath + File.separator + configProperties;
			File configFile = new File(configFilePath);

	        if (configFile.exists()) {
	        	configProperties = configFilePath;
	        }
			
			System.out.println("Reading properties from "+properties);
			System.out.println("Reading config properties from "+configProperties);
			configuration = new CompositeConfiguration();
			// configuration.addConfiguration(pConfig);
			
			// First add the config properties for system configured properties
			// This ensures that the users cannot override the property configured internally.
			builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
					.configure(new Parameters().properties().setFileName(configProperties));
			configuration.addConfiguration(builder.getConfiguration());
						
			// Then add the user properties
			builder = new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
			    .configure(new Parameters().properties()
			        .setFileName(properties));
			configuration.addConfiguration(builder.getConfiguration());
			
		    
			System.out.println("Value of hpc.max.upload.filesize: " + getProperty("hpc.max.upload.filesize"));
		    
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getProperty(String key) {
		return (String) configuration.getProperty(key);
	}

	public void setProperty(String key, Object value) {
        configuration.setProperty(key, value);
	}

	public void save() {
		try {
			builder.save();
		} catch (ConfigurationException e) {
			System.out.println("Failed to save: "+e.getMessage());
		}
	}

}
