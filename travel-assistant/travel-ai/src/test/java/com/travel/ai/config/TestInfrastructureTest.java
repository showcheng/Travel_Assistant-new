package com.travel.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke test to verify that the test infrastructure (JUnit 5 + Mockito) is properly configured.
 * This test does NOT load the Spring context -- it is a pure unit test.
 */
class TestInfrastructureTest extends BaseServiceTest {

    @Test
    @DisplayName("JUnit 5 lifecycle works")
    void junitLifecycleWorks() {
        // Simple assertion to confirm JUnit is running
        assertTrue(true, "Basic assertion should pass");
    }

    @Test
    @DisplayName("Mockito can mock and stub a List")
    void mockitoCanMockAndStub() {
        // Arrange
        List<String> mockList = mock(List.class);
        when(mockList.size()).thenReturn(3);
        when(mockList.get(0)).thenReturn("travel-ai");

        // Act
        int size = mockList.size();
        String first = mockList.get(0);

        // Assert
        assertEquals(3, size, "Mocked list size should be 3");
        assertEquals("travel-ai", first, "Mocked first element should be 'travel-ai'");
        verify(mockList).size();
        verify(mockList).get(0);
    }

    @Test
    @DisplayName("BaseServiceTest helper methods work")
    void baseServiceTestHelpersWork() {
        // Use the helper from BaseServiceTest
        List<String> mockList = createMock(List.class);
        assertNotNull(mockList, "createMock should return a non-null mock");

        when(mockList.isEmpty()).thenReturn(true);
        assertTrue(mockList.isEmpty(), "Stubbed mock should return true");

        assertNoMoreInteractions(mockList);
    }
}
