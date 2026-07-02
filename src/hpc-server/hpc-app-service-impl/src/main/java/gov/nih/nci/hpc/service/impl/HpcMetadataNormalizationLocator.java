/**
 * HpcMetadataNormalizationLocator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcMetadataNormalizationDAO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Metadata normalization cache locator.
 */
public class HpcMetadataNormalizationLocator {

	@Autowired
	private HpcMetadataNormalizationDAO metadataNormalizationDAO = null;

	private Map<String, String> normalizationMapping = new LinkedHashMap<>();

	/** Default constructor for Spring dependency injection. */
	private HpcMetadataNormalizationLocator() {
	}

	public void reload() throws HpcException {
		normalizationMapping = new LinkedHashMap<>(metadataNormalizationDAO.getNormalizationMapping());
	}

	public Map<String, String> getNormalizationMapping() {
		return Collections.unmodifiableMap(normalizationMapping);
	}
}

