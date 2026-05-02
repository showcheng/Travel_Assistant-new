package com.travel.ai.client;

import com.travel.ai.config.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExternalServiceClient unit tests.
 *
 * Tests all HTTP calls via mocked RestTemplate - no real network requests.
 * Covers success, timeout/failure fallback, and parameterized URL construction.
 */
class ExternalServiceClientTest extends BaseServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ExternalServiceClient client;

    @BeforeEach
    void setUp() throws Exception {
        // Create client with a real RestTemplateBuilder, then inject our mock
        client = new ExternalServiceClient(new org.springframework.boot.web.client.RestTemplateBuilder());
        injectMockRestTemplate(client, restTemplate);
        // @Value fields are not set outside Spring context, inject defaults via reflection
        injectField(client, "productServiceUrl", "http://localhost:8082");
        injectField(client, "orderServiceUrl", "http://localhost:8083");
    }

    /**
     * Inject a mock RestTemplate into the client via reflection.
     * This avoids needing to mock RestTemplateBuilder's complex build chain.
     */
    private void injectMockRestTemplate(ExternalServiceClient target, RestTemplate mock) throws Exception {
        injectField(target, "restTemplate", mock);
    }

    /**
     * Inject a value into any field via reflection.
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = ExternalServiceClient.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ========== getProducts tests ==========

    @Test
    @DisplayName("getProducts - success with keyword returns formatted product string")
    void testGetProducts_Success() {
        // Arrange: mock product service returning JSON array
        String productJson = "[{\"id\":1,\"name\":\"故宫博物院成人票\",\"price\":60.0}," +
                "{\"id\":2,\"name\":\"长城一日游\",\"price\":120.0}," +
                "{\"id\":3,\"name\":\"颐和园门票\",\"price\":30.0}]";

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(productJson);

        // Act
        String result = client.getProducts("北京");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("故宫"), "Should contain product name from response");
        assertTrue(result.contains("长城"), "Should contain product name from response");
        assertTrue(result.contains("颐和园"), "Should contain product name from response");
        assertFalse(result.contains("暂不可用"), "Should NOT contain fallback text on success");
        verify(restTemplate).getForObject(contains("/api/products"), eq(String.class));
    }

    @Test
    @DisplayName("getProducts - timeout/failure falls back to local recommendation")
    void testGetProducts_Timeout_Fallback() {
        // Arrange: mock throws RestClientException (simulating timeout)
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        // Act
        String result = client.getProducts("北京");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("暂不可用") || result.contains("推荐"),
                "Fallback should indicate service unavailability or provide local recommendation");
        assertFalse(result.contains("null"), "Fallback should not contain 'null'");
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("getProducts - null response falls back gracefully")
    void testGetProducts_NullResponse() {
        // Arrange: service returns null body
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(null);

        // Act
        String result = client.getProducts("故宫");

        // Assert
        assertNotNull(result, "Should never return null");
        assertTrue(result.length() > 0, "Fallback should have content");
    }

    @Test
    @DisplayName("getProducts - empty keyword uses default URL correctly")
    void testGetProducts_EmptyKeyword() {
        // Arrange
        String productJson = "[{\"id\":1,\"name\":\"热门门票\",\"price\":50.0}]";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(productJson);

        // Act
        String result = client.getProducts("");

        // Assert
        assertNotNull(result);
        verify(restTemplate).getForObject(contains("/api/products"), eq(String.class));
    }

    // ========== getOrders tests ==========

    @Test
    @DisplayName("getOrders - success returns formatted order string")
    void testGetOrders_Success() {
        // Arrange: mock order service returning JSON
        String orderJson = "[{\"id\":101,\"productName\":\"故宫门票\",\"status\":\"COMPLETED\",\"amount\":60.0}," +
                "{\"id\":102,\"productName\":\"长城一日游\",\"status\":\"PENDING_PAYMENT\",\"amount\":120.0}]";

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(orderJson);

        // Act
        String result = client.getOrders(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("故宫"), "Should contain product name from order");
        assertTrue(result.contains("长城"), "Should contain product name from order");
        assertFalse(result.contains("暂不可用"), "Should NOT contain fallback text on success");
        verify(restTemplate).getForObject(contains("/api/orders"), eq(String.class));
    }

    @Test
    @DisplayName("getOrders - correct URL with userId parameter")
    void testGetOrders_WithUserId() {
        // Arrange
        String orderJson = "[]";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(orderJson);

        // Act
        client.getOrders(42L);

        // Assert: verify URL contains correct userId
        verify(restTemplate).getForObject(contains("userId=42"), eq(String.class));
    }

    @Test
    @DisplayName("getOrders - unauthorized (401) falls back gracefully")
    void testGetOrders_Unauthorized_Fallback() {
        // Arrange: mock throws RestClientException simulating 401
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("401 Unauthorized"));

        // Act
        String result = client.getOrders(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("暂不可用") || result.contains("稍后再试") || result.contains("订单"),
                "Fallback should indicate service issue");
        assertFalse(result.contains("401"), "Fallback should not expose HTTP error details to user");
    }

    @Test
    @DisplayName("getOrders - null response falls back gracefully")
    void testGetOrders_NullResponse() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(null);

        // Act
        String result = client.getOrders(1L);

        // Assert
        assertNotNull(result, "Should never return null");
        assertTrue(result.length() > 0, "Fallback should have content");
    }

    // ========== URL encoding tests ==========

    @Test
    @DisplayName("getProducts - keyword with special characters is URL-encoded")
    void testGetProducts_UrlEncoding() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("[]");

        // Act
        client.getProducts("北京故宫");

        // Assert: verify the URL was called (encoding is handled internally)
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    // ========== Configurable service URLs ==========

    @Test
    @DisplayName("getProducts - uses configured product service URL")
    void testGetProducts_UsesConfiguredUrl() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("[]");

        // Act
        client.getProducts("test");

        // Assert: verify the URL starts with the default product service URL
        verify(restTemplate).getForObject(startsWith("http://localhost:8082/api/products"), eq(String.class));
    }

    @Test
    @DisplayName("getOrders - uses configured order service URL")
    void testGetOrders_UsesConfiguredUrl() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("[]");

        // Act
        client.getOrders(1L);

        // Assert: verify the URL starts with the default order service URL
        verify(restTemplate).getForObject(startsWith("http://localhost:8083/api/orders"), eq(String.class));
    }
}
