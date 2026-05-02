package com.travel.ai.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for all service unit tests.
 *
 * Uses MockitoExtension for fast unit tests without loading the Spring context.
 * Subclasses should use @Mock and @InjectMocks annotations to set up dependencies.
 *
 * For integration tests that need the Spring context, create a separate base class
 * with @SpringBootTest and @ActiveProfiles("test").
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    /**
     * Helper to create a mock of the given class.
     * Useful for one-off mocks that are not fields annotated with @Mock.
     *
     * @param clazz the class to mock
     * @param <T>   the type of the mock
     * @return a mock instance
     */
    protected <T> T createMock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }

    /**
     * Helper to assert that no unexpected interactions occurred on all given mocks.
     * Call this at the end of a test to verify no stray interactions happened.
     *
     * @param mocks the mocks to verify
     */
    protected void assertNoMoreInteractions(Object... mocks) {
        org.mockito.Mockito.verifyNoMoreInteractions(mocks);
    }
}
