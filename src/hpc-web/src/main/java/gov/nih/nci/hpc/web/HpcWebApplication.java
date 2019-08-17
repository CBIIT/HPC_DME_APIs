/**
 * HpcWebApplication.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;

/**
 * <p>
 * HPC Web Application
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcWebApplication.java
 */

@SpringBootApplication
@EnableCaching
public class HpcWebApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(HpcWebApplication.class, args);
	}
}
