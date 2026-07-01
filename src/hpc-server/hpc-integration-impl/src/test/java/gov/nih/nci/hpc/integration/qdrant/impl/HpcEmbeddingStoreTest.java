package gov.nih.nci.hpc.integration.qdrant.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

class HpcEmbeddingStoreTest {

    @Test
    void constructorShouldRejectBlankHost() {
        HpcException exception = assertThrows(HpcException.class,
                () -> new HpcEmbeddingStore(" ", 6334, "hpc-collections", false, null));

        assertEquals(HpcErrorType.SPRING_CONFIGURATION_ERROR, exception.getErrorType());
        assertEquals("Qdrant host must be configured", exception.getMessage());
    }

    @Test
    void constructorShouldRejectBlankCollectionName() {
        HpcException exception = assertThrows(HpcException.class,
                () -> new HpcEmbeddingStore("localhost", 6334, " ", false, null));

        assertEquals(HpcErrorType.SPRING_CONFIGURATION_ERROR, exception.getErrorType());
        assertEquals("Qdrant collection name must be configured", exception.getMessage());
    }

    @Test
    void defaultConstructorShouldBeDisabled() throws Exception {
        Constructor<HpcEmbeddingStore> constructor = HpcEmbeddingStore.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        HpcException cause = assertInstanceOf(HpcException.class, exception.getCause());

        assertEquals(HpcErrorType.SPRING_CONFIGURATION_ERROR, cause.getErrorType());
        assertEquals("HpcEmbeddingStore default constructor disabled", cause.getMessage());
    }
}
