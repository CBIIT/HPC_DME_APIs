package gov.nih.nci.hpc.integration.qdrant.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.jupiter.api.Test;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

class HpcVectorStoreProxyImplTest {

    @Test
    void storeVectorShouldRejectNullOrEmptyVector() throws Exception {
        HpcVectorStoreProxyImpl proxy = newProxy();

        HpcException exception = assertThrows(HpcException.class, () -> proxy.storeVector(null, "collection-1"));

        assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exception.getErrorType());
        assertEquals("Embedding vector cannot be empty", exception.getMessage());
    }

    @Test
    void storeVectorShouldRejectBlankCollectionId() throws Exception {
        HpcVectorStoreProxyImpl proxy = newProxy();

        HpcException exception = assertThrows(HpcException.class, () -> proxy.storeVector(List.of(1.0f), " "));

        assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exception.getErrorType());
        assertEquals("Collection ID cannot be blank", exception.getMessage());
    }

    @Test
    void storeVectorShouldWrapUnexpectedErrors() throws Exception {
        HpcVectorStoreProxyImpl proxy = newProxy();

        HpcException exception = assertThrows(HpcException.class,
                () -> proxy.storeVector(List.of(1.0f, 2.0f), "collection-1"));

        assertEquals(HpcErrorType.UNEXPECTED_ERROR, exception.getErrorType());
        assertInstanceOf(NullPointerException.class, exception.getCause());
    }

    @Test
    void findCollectionIdsShouldRejectNullOrEmptyQueryVector() throws Exception {
        HpcVectorStoreProxyImpl proxy = newProxy();

        HpcException exception = assertThrows(HpcException.class, () -> proxy.findCollectionIds(null, 10));

        assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exception.getErrorType());
        assertEquals("Query vector cannot be empty", exception.getMessage());
    }

    @Test
    void findCollectionIdsShouldRejectNonPositiveMaxResults() throws Exception {
        HpcVectorStoreProxyImpl proxy = newProxy();

        HpcException exception = assertThrows(HpcException.class, () -> proxy.findCollectionIds(List.of(1.0f), 0));

        assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exception.getErrorType());
        assertEquals("maxResults must be greater than zero", exception.getMessage());
    }

    @Test
    void findCollectionIdsShouldWrapUnexpectedErrors() throws Exception {
        HpcVectorStoreProxyImpl proxy = newProxy();

        HpcException exception = assertThrows(HpcException.class,
                () -> proxy.findCollectionIds(List.of(1.0f, 2.0f), 5));

        assertEquals(HpcErrorType.UNEXPECTED_ERROR, exception.getErrorType());
        assertInstanceOf(NullPointerException.class, exception.getCause());
    }

    private HpcVectorStoreProxyImpl newProxy() throws Exception {
        Constructor<HpcVectorStoreProxyImpl> constructor = HpcVectorStoreProxyImpl.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
