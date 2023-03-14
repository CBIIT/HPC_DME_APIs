package gov.nih.nci.hpc.web;

import java.util.HashSet;
import java.util.Set;

import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SwaggerWsEndpointsConfig {

	@Primary
	@Bean
	public Set<SwaggerUrl> apis(SwaggerUiConfigProperties swaggerUiConfig) {
		Set<SwaggerUrl> swaggerUrlSet = new HashSet<>();
		SwaggerUrl wsResource = new SwaggerUrl("Swagger", "/config/api-docs.json", "api-docs");
		swaggerUrlSet.add(wsResource);
		swaggerUiConfig.setUrls(swaggerUrlSet);
		return swaggerUrlSet;
	}
}