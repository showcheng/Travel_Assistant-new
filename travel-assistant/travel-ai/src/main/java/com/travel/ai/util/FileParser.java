package com.travel.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * File parser utility for extracting text content from uploaded files.
 * Supports PDF and TXT file formats.
 * Uses Apache Tika for PDF parsing.
 */
@Slf4j
public class FileParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".pdf", ".txt");
    private static final int DEFAULT_MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MIN_CONTENT_LENGTH = 50;

    private final int maxSizeBytes;
    private final Tika tika;

    /**
     * Default constructor with 10MB max file size.
     */
    public FileParser() {
        this(DEFAULT_MAX_SIZE_BYTES);
    }

    /**
     * Constructor with custom max file size (for testing).
     *
     * @param maxSizeBytes maximum allowed file size in bytes
     */
    public FileParser(int maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
        this.tika = new Tika();
    }

    /**
     * Parse a file and extract its text content.
     *
     * @param filename     the original filename (used for extension detection)
     * @param inputStream  the file content stream
     * @return extracted text content (trimmed)
     * @throws IllegalArgumentException if file is empty, too large, unsupported format,
     *                                  or content is too short
     * @throws RuntimeException         if parsing fails due to IO or Tika errors
     */
    public String parse(String filename, InputStream inputStream) {
        // Validate extension
        if (!isSupported(filename)) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持PDF和TXT");
        }

        try {
            // Read content with size check
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int totalRead = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalRead += bytesRead;
                if (totalRead > maxSizeBytes) {
                    throw new IllegalArgumentException(
                            "文件大小不能超过" + (maxSizeBytes / 1024 / 1024) + "MB");
                }
                baos.write(buffer, 0, bytesRead);
            }

            byte[] bytes = baos.toByteArray();
            if (bytes.length == 0) {
                throw new IllegalArgumentException("文件内容为空");
            }

            // Parse based on file type
            String content;
            if (filename.toLowerCase().endsWith(".pdf")) {
                content = tika.parseToString(new ByteArrayInputStream(bytes));
            } else {
                content = new String(bytes, StandardCharsets.UTF_8);
            }

            // Validate content length
            String trimmed = content.trim();
            if (trimmed.length() < MIN_CONTENT_LENGTH) {
                throw new IllegalArgumentException(
                        "文件内容过少，至少需要" + MIN_CONTENT_LENGTH + "个字符");
            }

            return trimmed;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a filename has a supported extension.
     *
     * @param filename the filename to check (case-insensitive)
     * @return true if the extension is supported (.pdf or .txt)
     */
    public boolean isSupported(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
