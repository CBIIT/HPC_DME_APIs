/**
 * HpcMetadataNormalizationDAOImplTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import gov.nih.nci.hpc.exception.HpcException;

@RunWith(MockitoJUnitRunner.class)
public class HpcMetadataNormalizationDAOImplTest {

	private static final String SQL = "select SYNONYM_NAME, CANONICAL_NAME from HPC_METADATA_NORMALIZATION";

	@Mock
	private JdbcTemplate jdbcTemplate;

	@InjectMocks
	private HpcMetadataNormalizationDAOImpl metadataNormalizationDAO;

	@Test
	public void testGetNormalizationMapping() throws HpcException {
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(toRow("z_alias", "z_canonical"));
		rows.add(toRow("a_alias", "a_canonical"));
		when(jdbcTemplate.queryForList(SQL)).thenReturn(rows);

		Map<String, String> mapping = metadataNormalizationDAO.getNormalizationMapping();

		assertEquals(2, mapping.size());
		assertEquals("a_canonical", mapping.get("a_alias"));
		assertEquals("z_canonical", mapping.get("z_alias"));
		assertEquals("a_alias", mapping.keySet().iterator().next());
	}

	private Map<String, Object> toRow(String synonym, String canonical) {
		Map<String, Object> row = new HashMap<>();
		row.put("SYNONYM_NAME", synonym);
		row.put("CANONICAL_NAME", canonical);
		return row;
	}
}
