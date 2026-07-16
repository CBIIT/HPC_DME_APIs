/**
 * HpcMetadataServiceImplTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.METADATA_VECTOR_ADDED_ATTRIBUTE;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

/**
 * HPC Metadata Service Implementation Test.
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcMetadataServiceImplTest {

    private static final String COLLECTION_PATH = "/test/collection/path";
    private static final Object AUTH_TOKEN = new Object();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private HpcMetadataServiceImpl service;

    @Mock
    private HpcDataManagementProxy dataManagementProxy;

    @Mock
    private HpcDataManagementAuthenticator dataManagementAuthenticator;

    // --- setMetadataVectorAdded tests ---

    @Test
    public void testSetMetadataVectorAdded_NullPath_ThrowsException() throws HpcException {
        expectedException.expect(HpcException.class);
        expectedException.expectMessage("Invalid collection or object path");

        service.setMetadataVectorAdded(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetMetadataVectorAdded_ValidPath_CallsUpdateCollectionMetadataWithTrueEntry()
            throws HpcException {
        when(dataManagementAuthenticator.getAuthenticatedToken()).thenReturn(AUTH_TOKEN);

        service.setMetadataVectorAdded(COLLECTION_PATH);

        ArgumentCaptor<List<HpcMetadataEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataManagementProxy).updateCollectionMetadata(eq(AUTH_TOKEN), eq(COLLECTION_PATH),
                captor.capture());

        List<HpcMetadataEntry> capturedEntries = captor.getValue();
        assertTrue("Metadata entries should contain metadata_vector_added entry",
                capturedEntries.stream()
                        .anyMatch(e -> METADATA_VECTOR_ADDED_ATTRIBUTE.equals(e.getAttribute())
                                && Boolean.TRUE.toString().equals(e.getValue())));
    }

    // --- toSystemGeneratedMetadata tests ---

    @Test
    public void testToSystemGeneratedMetadata_WithMetadataVectorAddedTrue_SetsTrue()
            throws HpcException {
        List<HpcMetadataEntry> entries = Arrays.asList(
                toMetadataEntry(METADATA_VECTOR_ADDED_ATTRIBUTE, "true"));

        HpcSystemGeneratedMetadata result = service.toSystemGeneratedMetadata(entries);

        assertTrue("metadataVectorAdded should be true", Boolean.TRUE.equals(result.getMetadataVectorAdded()));
    }

    @Test
    public void testToSystemGeneratedMetadata_WithMetadataVectorAddedFalse_SetsFalse()
            throws HpcException {
        List<HpcMetadataEntry> entries = Arrays.asList(
                toMetadataEntry(METADATA_VECTOR_ADDED_ATTRIBUTE, "false"));

        HpcSystemGeneratedMetadata result = service.toSystemGeneratedMetadata(entries);

        assertTrue("metadataVectorAdded should be false", Boolean.FALSE.equals(result.getMetadataVectorAdded()));
    }

    @Test
    public void testToSystemGeneratedMetadata_WithoutMetadataVectorAdded_NullValue()
            throws HpcException {
        List<HpcMetadataEntry> entries = Collections.emptyList();

        HpcSystemGeneratedMetadata result = service.toSystemGeneratedMetadata(entries);

        assertNull("metadataVectorAdded should be null when absent", result.getMetadataVectorAdded());
    }

    private HpcMetadataEntry toMetadataEntry(String attribute, String value) {
        HpcMetadataEntry entry = new HpcMetadataEntry();
        entry.setAttribute(attribute);
        entry.setValue(value);
        return entry;
    }
}
