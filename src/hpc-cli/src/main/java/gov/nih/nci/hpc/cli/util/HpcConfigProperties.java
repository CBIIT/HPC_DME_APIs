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

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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
			Map<String, String> env = System.getenv();
			String basePath=env.get("HPC_DM_UTILS");
			String properties = HPC_PROPS;
			String filePath = System.getProperty("hpc.client.properties");
			if (filePath != null)
				properties = filePath;
			
			//properties = basePath + File.separator + properties;
			
			System.out.println("Reading properties from "+properties);
			configuration = new CompositeConfiguration();
			// configuration.addConfiguration(pConfig);
			configuration.addConfiguration(new PropertiesConfiguration(properties));
		} catch (ConfigurationException e) {
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
			System.out.println("Failed to save: "+e.getMessage());
		}
	}

}
