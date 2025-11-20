package com.vanespark.vertext.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Text Embedding Service using TF-IDF algorithm
 * Generates 384-dimensional embeddings for message semantic search
 *
 * Algorithm:
 * 1. Tokenization: Lowercase, remove punctuation, filter stop words and short words
 * 2. TF-IDF Calculation:
 *    - TF (Term Frequency): Count of word in message / total words
 *    - IDF (Inverse Document Frequency): ln((total_messages + 1) / (messages_with_word + 1)) + 1
 * 3. Word Hashing: Hash word to index (word.hashCode() % 384)
 * 4. Vector Construction: Accumulate TF-IDF scores at hashed indices
 * 5. Normalization: Normalize vector to unit length
 */
@Singleton
class TextEmbeddingService @Inject constructor() {

    companion object {
        const val EMBEDDING_DIMENSION = 384  // Standard dimension for embeddings
        const val MIN_WORD_LENGTH = 3  // Minimum word length to consider

        // Common English stop words to filter out
        val STOP_WORDS = setOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their",
            "what", "so", "up", "out", "if", "about", "who", "get", "which", "go",
            "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
            "take", "people", "into", "year", "your", "good", "some", "could", "them",
            "see", "other", "than", "then", "now", "look", "only", "come", "its", "over",
            "think", "also", "back", "after", "use", "two", "how", "our", "work",
            "first", "well", "way", "even", "new", "want", "because", "any", "these",
            "give", "day", "most", "us"
        )
    }

    // Document frequency map: word -> count of documents containing the word
    private val documentFrequency = mutableMapOf<String, Int>()
    private var totalDocuments = 0

    /**
     * Generate embedding for a single text
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        try {
            val tokens = tokenize(text)

            if (tokens.isEmpty()) {
                // Return zero vector for empty text
                return@withContext FloatArray(EMBEDDING_DIMENSION)
            }

            // Calculate term frequencies
            val termFrequency = mutableMapOf<String, Int>()
            tokens.forEach { word ->
                termFrequency[word] = termFrequency.getOrDefault(word, 0) + 1
            }

            // Build embedding vector
            val embedding = FloatArray(EMBEDDING_DIMENSION)

            for ((word, count) in termFrequency) {
                val tf = count.toFloat() / tokens.size  // Term frequency
                val idf = calculateIDF(word)  // Inverse document frequency
                val tfidf = tf * idf

                // Hash word to embedding dimension index
                val index = Math.floorMod(word.hashCode(), EMBEDDING_DIMENSION)
                embedding[index] += tfidf
            }

            // Normalize to unit vector
            normalizeVector(embedding); embedding
        } catch (e: Exception) {
            Timber.e(e, "Error generating embedding for text: ${text.take(50)}")
            FloatArray(EMBEDDING_DIMENSION)  // Return zero vector on error
        }
    }

    /**
     * Generate embeddings for multiple texts with progress callback
     */
    suspend fun generateEmbeddingsWithProgress(
        texts: List<String>,
        onProgress: ((Float, String) -> Unit)? = null
    ): List<FloatArray> = withContext(Dispatchers.Default) {
        val embeddings = mutableListOf<FloatArray>()
        val total = texts.size

        texts.forEachIndexed { index, text ->
            val embedding = generateEmbedding(text)
            embeddings.add(embedding)

            // Report progress
            val progress = (index + 1).toFloat() / total
            onProgress?.invoke(progress, "Generated ${index + 1} / $total embeddings")
        }

        embeddings
    }

    /**
     * Calculate cosine similarity between two embeddings
     * Returns value between 0 (orthogonal) and 1 (identical)
     */
    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) {
            "Embeddings must have the same dimension"
        }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)

        return if (denominator > 0f) {
            (dotProduct / denominator).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * Update corpus with new documents for IDF calculation
     */
    fun updateCorpus(documents: List<String>) {
        // Clear existing corpus stats
        documentFrequency.clear()
        totalDocuments = documents.size

        // Count document frequency for each word
        documents.forEach { document ->
            val uniqueWords = tokenize(document).toSet()
            uniqueWords.forEach { word ->
                documentFrequency[word] = documentFrequency.getOrDefault(word, 0) + 1
            }
        }

        Timber.d("Corpus updated: $totalDocuments documents, ${documentFrequency.size} unique words")
    }

    /**
     * Convert embedding to comma-separated string for database storage
     */
    fun embeddingToString(embedding: FloatArray): String {
        return embedding.joinToString(",")
    }

    /**
     * Convert comma-separated string back to embedding array
     */
    fun stringToEmbedding(embeddingString: String): FloatArray {
        return try {
            embeddingString.split(",")
                .map { it.toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing embedding string")
            FloatArray(EMBEDDING_DIMENSION)
        }
    }

    // === Private Helper Methods ===

    /**
     * Tokenize text into words
     * - Convert to lowercase
     * - Remove punctuation
     * - Filter stop words
     * - Filter short words
     */
    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")  // Remove punctuation
            .split(Regex("\\s+"))  // Split on whitespace
            .filter { word ->
                word.length >= MIN_WORD_LENGTH &&
                !STOP_WORDS.contains(word) &&
                !word.matches(Regex("\\d+"))  // Filter pure numbers
            }
    }

    /**
     * Calculate Inverse Document Frequency for a word
     * IDF = ln((total_docs + 1) / (docs_with_word + 1)) + 1
     */
    private fun calculateIDF(word: String): Float {
        if (totalDocuments == 0) {
            return 1f  // Default IDF if corpus not initialized
        }

        val docsWithWord = documentFrequency.getOrDefault(word, 0)
        return ln((totalDocuments + 1).toFloat() / (docsWithWord + 1).toFloat()) + 1f
    }

    /**
     * Normalize vector to unit length (L2 normalization)
     */
    private fun normalizeVector(vector: FloatArray) {
        var norm = 0f
        for (value in vector) {
            norm += value * value
        }
        norm = sqrt(norm)

        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }

    /**
     * Get corpus statistics for debugging/monitoring
     */
    fun getCorpusStats(): Map<String, Any> {
        return mapOf(
            "total_documents" to totalDocuments,
            "unique_words" to documentFrequency.size,
            "embedding_dimension" to EMBEDDING_DIMENSION,
            "min_word_length" to MIN_WORD_LENGTH,
            "stop_words_count" to STOP_WORDS.size
        )
    }
}
