package com.travel.ai.util;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义感知的自适应文本分块工具类
 * 融合句子边界检测与相邻句子语义相似度评估的双层分块决策机制
 */
@Slf4j
@Component
public class TextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP_SIZE = 50;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;

    private static final char[] SENTENCE_DELIMITERS = {'。', '？', '！', '\n'};

    private final EmbeddingModel embeddingModel;

    public TextChunker(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 使用默认参数进行语义感知的自适应文本分块
     */
    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    /**
     * 语义感知的自适应文本分块
     * 第一层决策：句子边界检测（句号、问号、感叹号、换行符）
     * 第二层决策：相邻句子语义相似度评估，低相似度表示语义断裂点
     *
     * @param text        原始文本
     * @param chunkSize   分块大小
     * @param overlapSize 重叠大小
     * @return 分块列表
     */
    public List<String> chunkText(String text, int chunkSize, int overlapSize) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            return new ArrayList<>();
        }

        // 预计算所有句子的向量（批量处理，减少API调用次数）
        float[][] sentenceEmbeddings = computeSentenceEmbeddings(sentences);

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;
        int sentenceIdx = 0;

        while (sentenceIdx < sentences.size()) {
            String sentence = sentences.get(sentenceIdx);
            int sentenceLen = sentence.length();

            // 检查是否需要开始新分块
            if (currentLength + sentenceLen > chunkSize && currentLength > 0) {
                // 第二层决策：检查当前句子与上一句子的语义相似度
                boolean shouldSplit = shouldSplitAtBoundary(
                        sentenceEmbeddings, sentenceIdx, currentLength, sentenceLen, chunkSize);

                if (shouldSplit) {
                    // 在语义断裂点切分
                    chunks.add(currentChunk.toString().trim());
                    // 保留重叠部分
                    String overlap = extractOverlap(currentChunk.toString(), overlapSize);
                    currentChunk = new StringBuilder(overlap);
                    currentLength = overlap.length();
                }
            }

            currentChunk.append(sentence);
            currentLength += sentenceLen;
            sentenceIdx++;
        }

        // 添加最后一个分块
        if (currentLength > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.debug("语义感知分块完成: 总长度={}, 句子数={}, 分块数={}",
                text.length(), sentences.size(), chunks.size());
        return chunks;
    }

    /**
     * 按段落分割文本（保留原有功能）
     */
    public List<String> chunkByParagraph(String text) {
        List<String> paragraphs = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return paragraphs;
        }
        String[] parts = text.split("\\n\\n+");
        for (String paragraph : parts) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        log.debug("按段落分割完成: 段落数={}", paragraphs.size());
        return paragraphs;
    }

    /**
     * 将文本按句子边界分割为句子列表
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            if (isSentenceDelimiter(text.charAt(i))) {
                String sentence = text.substring(start, i + 1).trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                start = i + 1;
            }
        }

        // 处理末尾无标点的文本
        if (start < text.length()) {
            String remaining = text.substring(start).trim();
            if (!remaining.isEmpty()) {
                sentences.add(remaining);
            }
        }

        return sentences;
    }

    private boolean isSentenceDelimiter(char c) {
        for (char delimiter : SENTENCE_DELIMITERS) {
            if (c == delimiter) return true;
        }
        return false;
    }

    /**
     * 批量计算所有句子的向量表示
     */
    private float[][] computeSentenceEmbeddings(List<String> sentences) {
        float[][] embeddings = new float[sentences.size()][];
        for (int i = 0; i < sentences.size(); i++) {
            try {
                Embedding embedding = embeddingModel.embed(sentences.get(i)).content();
                float[] vector = new float[embedding.vector().length];
                for (int j = 0; j < vector.length; j++) {
                    vector[j] = (float) embedding.vector()[j];
                }
                embeddings[i] = vector;
            } catch (Exception e) {
                log.warn("句子向量化失败, 句子索引={}: {}", i, e.getMessage());
                embeddings[i] = null;
            }
        }
        return embeddings;
    }

    /**
     * 判断是否应在当前边界处切分
     * 综合考虑分块长度、句子边界和语义相似度
     */
    private boolean shouldSplitAtBoundary(float[][] embeddings, int sentenceIdx,
                                           int currentLength, int sentenceLen, int chunkSize) {
        // 如果当前句子是第一句，不切分
        if (sentenceIdx == 0) {
            return false;
        }

        // 如果添加该句后仍不超过chunkSize的1.5倍，检查语义相似度再决定
        boolean exceedsHardLimit = currentLength + sentenceLen > chunkSize * 1.5;

        // 计算相邻句子的语义相似度
        double similarity = computeCosineSimilarity(
                embeddings[sentenceIdx - 1], embeddings[sentenceIdx]);

        // 语义断裂点：相似度低于阈值，说明话题发生转换
        if (similarity < DEFAULT_SIMILARITY_THRESHOLD) {
            log.debug("语义断裂点: 句子索引={}, 相似度={}", sentenceIdx, String.format("%.3f", similarity));
            return true;
        }

        // 即使语义连贯，超过硬限制也需要切分
        return exceedsHardLimit;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double computeCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 1.0; // 无法计算时假设语义连贯
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 从当前分块末尾提取重叠文本
     */
    private String extractOverlap(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        String overlap = text.substring(text.length() - overlapSize);
        // 尝试在句子边界处开始重叠
        for (char delimiter : SENTENCE_DELIMITERS) {
            int idx = overlap.indexOf(delimiter);
            if (idx >= 0 && idx < overlap.length() - 1) {
                return overlap.substring(idx + 1);
            }
        }
        return overlap;
    }
}
