/**
 * HpcWebConfig.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 * Hpc Web Config to add Interceptor
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */
@Configuration
public class HpcWebConfig implements WebMvcConfigurer {

	@Value("${gov.nih.nci.hpc.web.frontend.server:}")
	private String frontendServer;
	
	private static final String[] EXCLUDE_PATTERNS = { "/login", "/css/**", "/fonts/**", "/img/**", "/js/**",
			"/ng-table/**" };
	/**
	 * The User Interceptor.
	 */
	@Autowired
	HpcUserInterceptor userInterceptor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.web.servlet.config.annotation.WebMvcConfigurer#
	 * addInterceptors(org.springframework.web.servlet.config.annotation.
	 * InterceptorRegistry)
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// Disable interceptor for static resources
		registry.addInterceptor(userInterceptor).excludePathPatterns(EXCLUDE_PATTERNS);
	}

	 @Override
     public void addFormatters(FormatterRegistry registry) {
		registry.removeConvertible(String.class, String[].class);
        registry.addConverter(String.class, String[].class, noCommaSplitStringToArrayConverter());
     }

	 @Override
     public void addResourceHandlers(ResourceHandlerRegistry registry) {
	   registry.addResourceHandler("/config/api-docs.json").
  		addResourceLocations("classpath:/config");
     }
	 
	  @Override
	  public void addCorsMappings(CorsRegistry registry) {

		if(StringUtils.isNotEmpty(frontendServer))
			registry.addMapping("/api/**").allowedOrigins(frontendServer).allowedMethods("*")
				.allowedHeaders("*")
				.allowCredentials(true);
	  }

	 @Bean
     public Converter<String, String[]> noCommaSplitStringToArrayConverter() {
        return new Converter<String, String[]>() {
            @Override
            public String[] convert(String source) {
                String[] arrayWithOneElement = {source};
                return arrayWithOneElement;
            }
        };
     }
}
