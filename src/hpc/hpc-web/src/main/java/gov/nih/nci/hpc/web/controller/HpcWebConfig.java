package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="gov")
public class HpcWebConfig {

	public HpcWebConfig()
	{
		
	}

	private List<String> hpcProps = new ArrayList<String>();
    
    public List<String> getHpcWebProperties() {
        return hpcProps;
    }

    public void setHpcWebProperties(List<String> props) {
        hpcProps = props;
    }

}