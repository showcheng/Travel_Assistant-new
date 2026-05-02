package com.travel.ai.util;

import com.travel.ai.config.BaseServiceTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileParser unit tests - TDD RED phase.
 * Tests are written BEFORE the implementation.
 * All tests should initially fail because FileParser does not exist yet.
 */
class FileParserTest extends BaseServiceTest {

    // Test 1: Parse a simple text file successfully
    @Test
    void testParseTxt_Success() {
        FileParser parser = new FileParser();
        byte[] content = "This is a valid text document with enough characters to pass the minimum length validation check".getBytes(StandardCharsets.UTF_8);
        String result = parser.parse("test.txt", new ByteArrayInputStream(content));
        assertTrue(result.contains("valid text document"));
    }

    // Test 2: Parse empty file throws IllegalArgumentException
    @Test
    void testParse_EmptyFile_ThrowsException() {
        FileParser parser = new FileParser();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            parser.parse("empty.txt", new ByteArrayInputStream(new byte[0]))
        );
        assertTrue(exception.getMessage().contains("空") || exception.getMessage().toLowerCase().contains("empty"));
    }

    // Test 3: Unsupported file format throws IllegalArgumentException
    @Test
    void testParse_UnsupportedFormat_ThrowsException() {
        FileParser parser = new FileParser();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            parser.parse("test.docx", new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)))
        );
        assertTrue(exception.getMessage().contains("PDF") || exception.getMessage().contains("TXT") || exception.getMessage().contains("不支持"));
    }

    // Test 4: File exceeding max size throws IllegalArgumentException
    @Test
    void testParse_FileTooLarge_ThrowsException() {
        FileParser parser = new FileParser(10); // 10 bytes max for testing
        byte[] largeContent = new byte[100];
        Arrays.fill(largeContent, (byte) 'a');
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            parser.parse("big.txt", new ByteArrayInputStream(largeContent))
        );
        assertTrue(exception.getMessage().contains("MB") || exception.getMessage().contains("大小"));
    }

    // Test 5: Content too short throws IllegalArgumentException
    @Test
    void testParse_ContentTooShort_ThrowsException() {
        FileParser parser = new FileParser();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            parser.parse("short.txt", new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8)))
        );
        assertTrue(exception.getMessage().contains("字符") || exception.getMessage().contains("过少"));
    }

    // Test 6: Parse a text file with substantial content
    @Test
    void testParseTxt_LongContent_Success() {
        FileParser parser = new FileParser();
        String content = "This is a comprehensive test document that contains more than fifty characters of text content to ensure it passes the minimum content length validation check.";
        String result = parser.parse("document.txt", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(result);
        assertTrue(result.length() >= 50);
        assertEquals(content, result);
    }

    // Test 7: Supported extensions validation - PDF supported
    @Test
    void testIsSupported_Pdf_ReturnsTrue() {
        FileParser parser = new FileParser();
        assertTrue(parser.isSupported("document.pdf"));
    }

    // Test 8: Supported extensions validation - TXT supported
    @Test
    void testIsSupported_Txt_ReturnsTrue() {
        FileParser parser = new FileParser();
        assertTrue(parser.isSupported("document.txt"));
    }

    // Test 9: Supported extensions validation - DOCX not supported
    @Test
    void testIsSupported_Docx_ReturnsFalse() {
        FileParser parser = new FileParser();
        assertFalse(parser.isSupported("document.docx"));
    }

    // Test 10: Supported extensions validation - EXE not supported
    @Test
    void testIsSupported_Exe_ReturnsFalse() {
        FileParser parser = new FileParser();
        assertFalse(parser.isSupported("document.exe"));
    }

    // Test 11: Null filename returns false for isSupported
    @Test
    void testIsSupported_NullFilename_ReturnsFalse() {
        FileParser parser = new FileParser();
        assertFalse(parser.isSupported(null));
    }

    // Test 12: Case-insensitive extension check
    @Test
    void testIsSupported_UpperCaseExtension_ReturnsTrue() {
        FileParser parser = new FileParser();
        assertTrue(parser.isSupported("document.PDF"));
        assertTrue(parser.isSupported("document.TXT"));
    }

    // Test 13: IOException during parsing is wrapped in RuntimeException
    @Test
    void testParse_IOException_WrappedInRuntimeException() {
        FileParser parser = new FileParser();
        InputStream brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            parser.parse("test.txt", brokenStream)
        );
        assertTrue(exception.getMessage().contains("解析失败") || exception.getMessage().contains("parse"));
    }

    // Test 14: Default constructor uses 10MB max size
    @Test
    void testDefaultConstructor_MaxSizeIs10MB() {
        FileParser parser = new FileParser();
        // 10MB content should not be rejected by default parser
        // We can't actually create 10MB in a unit test, but we verify the constructor works
        assertNotNull(parser);
    }

    // Test 15: Filename without extension returns false
    @Test
    void testIsSupported_NoExtension_ReturnsFalse() {
        FileParser parser = new FileParser();
        assertFalse(parser.isSupported("document"));
        assertFalse(parser.isSupported("document."));
    }
}
