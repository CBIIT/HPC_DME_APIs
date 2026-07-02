/**
 * HpcMetadataNormalizationLocatorTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.nih.nci.hpc.dao.HpcMetadataNormalizationDAO;
import gov.nih.nci.hpc.exception.HpcException;

@RunWith(MockitoJUnitRunner.class)
public class HpcMetadataNormalizationLocatorTest {

	@Mock
	private HpcMetadataNormalizationDAO metadataNormalizationDAO;

	@InjectMocks
	private HpcMetadataNormalizationLocator metadataNormalizationLocator;

	@Test
	public void testReload() throws HpcException {
		Map<String, String> mapping = new LinkedHashMap<>();
		mapping.put("alias_a", "canonical_a");
		mapping.put("alias_b", "canonical_b");
		when(metadataNormalizationDAO.getNormalizationMapping()).thenReturn(mapping);

		metadataNormalizationLocator.reload();

		Map<String, String> cachedMapping = metadataNormalizationLocator.getNormalizationMapping();
		assertEquals(2, cachedMapping.size());
		assertEquals("canonical_a", cachedMapping.get("alias_a"));
		assertTrue(cachedMapping.containsKey("alias_b"));
	}
}

