/**
 * HpcMetadataValidatorTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.Test;

/**
 * HPC Metadata Validator Test.
 */
public class HpcMetadataValidatorTest {

    /**
     * Creates an HpcMetadataValidator instance via reflection (its constructor is private).
     */
    private HpcMetadataValidator createValidator() throws Exception {
        Constructor<HpcMetadataValidator> constructor =
                HpcMetadataValidator.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Test
    public void testSystemGeneratedMetadataAttributesContainsMetadataVectorAdded() throws Exception {
        HpcMetadataValidator validator = createValidator();
        assertTrue(
                "system generated metadata attributes should include metadata_vector_added",
                validator.getSystemGeneratedMetadataAttributes()
                        .contains(HpcMetadataValidator.METADATA_VECTOR_ADDED_ATTRIBUTE));
    }

    @Test
    public void testCollectionSystemGeneratedMetadataAttributeNamesContainsMetadataVectorAdded()
            throws Exception {
        HpcMetadataValidator validator = createValidator();
        assertTrue(
                "collection system generated metadata attribute names should include metadata_vector_added",
                validator.getCollectionSystemGeneratedMetadataAttributeNames()
                        .contains(HpcMetadataValidator.METADATA_VECTOR_ADDED_ATTRIBUTE));
    }
}
