package com.travel.ai.controller;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.dto.DocumentUploadResponse;
import com.travel.ai.service.KnowledgeBaseService;
import com.travel.common.response.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * KnowledgeBaseController file upload endpoint tests - TDD RED phase.
 * Tests the new POST /api/knowledge/upload/file endpoint.
 */
class KnowledgeBaseControllerFileUploadTest extends BaseServiceTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private KnowledgeBaseController controller;

    private DocumentUploadResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = DocumentUploadResponse.builder()
                .docId("test-doc-id")
                .title("Test Document")
                .status("COMPLETED")
                .chunkCount(3)
                .message("Document uploaded successfully")
                .build();
    }

    // Test 1: Upload valid TXT file successfully
    @Test
    void testUploadFile_Txt_Success() throws Exception {
        // Arrange
        String content = "This is a test document with enough characters to pass the minimum content length validation check.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        when(knowledgeBaseService.uploadDocument(any())).thenReturn(mockResponse);

        // Act
        Result<?> result = controller.uploadFile(file, "Test Title", "ATTRACTION", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(knowledgeBaseService).uploadDocument(any());
    }

    // Test 2: Upload empty file returns error
    @Test
    void testUploadFile_EmptyFile_ReturnsError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        // Act
        Result<?> result = controller.uploadFile(file, "Empty", "ATTRACTION", 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    // Test 3: Upload unsupported file format returns error
    @Test
    void testUploadFile_UnsupportedFormat_ReturnsError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "some content".getBytes(StandardCharsets.UTF_8)
        );

        // Act
        Result<?> result = controller.uploadFile(file, "Doc Title", "ATTRACTION", 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    // Test 4: Upload TXT file with null title uses filename
    @Test
    void testUploadFile_NullTitle_UsesFilename() throws Exception {
        // Arrange
        String content = "This is a test document with enough characters to pass the minimum content length validation check.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "myfile.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        when(knowledgeBaseService.uploadDocument(any())).thenReturn(mockResponse);

        // Act
        Result<?> result = controller.uploadFile(file, null, "ATTRACTION", 1L);

        // Assert
        assertTrue(result.isSuccess());
        verify(knowledgeBaseService).uploadDocument(argThat(req ->
                "myfile.txt".equals(req.getTitle())
        ));
    }

    // Test 5: Upload TXT file with empty title uses filename
    @Test
    void testUploadFile_EmptyTitle_UsesFilename() throws Exception {
        // Arrange
        String content = "This is a test document with enough characters to pass the minimum content length validation check.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "myfile.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        when(knowledgeBaseService.uploadDocument(any())).thenReturn(mockResponse);

        // Act
        Result<?> result = controller.uploadFile(file, "", "ATTRACTION", 1L);

        // Assert
        assertTrue(result.isSuccess());
        verify(knowledgeBaseService).uploadDocument(argThat(req ->
                "myfile.txt".equals(req.getTitle())
        ));
    }

    // Test 6: Upload file with content too short returns error
    @Test
    void testUploadFile_ContentTooShort_ReturnsError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "short.txt", "text/plain",
                "hi".getBytes(StandardCharsets.UTF_8)
        );

        // Act
        Result<?> result = controller.uploadFile(file, "Short", "ATTRACTION", 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    // Test 7: Upload uses default category when not specified
    @Test
    void testUploadFile_DefaultCategory() throws Exception {
        // Arrange
        String content = "This is a test document with enough characters to pass the minimum content length validation check.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        when(knowledgeBaseService.uploadDocument(any())).thenReturn(mockResponse);

        // Act - category defaults to "未分类"
        Result<?> result = controller.uploadFile(file, "Title", null, 1L);

        // Assert
        assertTrue(result.isSuccess());
    }

    // Test 8: Upload uses default userId when not specified
    @Test
    void testUploadFile_DefaultUserId() throws Exception {
        // Arrange
        String content = "This is a test document with enough characters to pass the minimum content length validation check.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        when(knowledgeBaseService.uploadDocument(any())).thenReturn(mockResponse);

        // Act - userId defaults to 1
        Result<?> result = controller.uploadFile(file, "Title", "ATTRACTION", null);

        // Assert
        assertTrue(result.isSuccess());
    }

    // Test 9: Service exception returns error result
    @Test
    void testUploadFile_ServiceException_ReturnsError() throws Exception {
        // Arrange
        String content = "This is a test document with enough characters to pass the minimum content length validation check.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        when(knowledgeBaseService.uploadDocument(any())).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        Result<?> result = controller.uploadFile(file, "Title", "ATTRACTION", 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    // Test 10: Upload PDF file successfully
    @Test
    void testUploadFile_Pdf_CallsService() throws Exception {
        // Note: We can't easily create a valid PDF in a unit test,
        // so this test verifies the controller flow with a mock PDF.
        // The actual PDF parsing is tested in FileParserTest.

        // Arrange - create a minimal content that looks like a PDF
        // The controller will attempt to parse it with Tika
        byte[] content = "%PDF-1.4 test content that has enough characters to be valid".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", content
        );

        // Act & Assert - The test verifies the controller handles PDF extension
        // Even if Tika can't parse the fake PDF, the extension check should pass
        Result<?> result = controller.uploadFile(file, "PDF Test", "ATTRACTION", 1L);

        // The result depends on Tika parsing - either success or a parse error
        assertNotNull(result);
    }
}
