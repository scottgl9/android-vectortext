# AI Assistant Architecture & Improvement Guide

## Overview

The AI Assistant in VectorText is a **rule-based natural language interface** that allows users to query their messages using conversational language. It uses the Model Context Protocol (MCP) architecture to provide a structured, extensible tool-calling system.

**Key Characteristics:**
- 🤖 **Rule-based intent detection** (not using an LLM)
- 🔧 **MCP-based tool architecture** for extensibility
- 📱 **Fully on-device** - no cloud dependencies
- 💬 **Chat-style UI** with conversation history
- ⚡ **Fast response times** using pattern matching

---

## Current Architecture

### 1. Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    User Interface Layer                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AIAssistantBottomSheet.kt                           │  │
│  │  - Modal bottom sheet (90% screen height)            │  │
│  │  - Chat-style message list                           │  │
│  │  - Text input with send button                       │  │
│  │  - Empty state with suggestions                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   ViewModel Layer                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AIAssistantViewModel.kt                             │  │
│  │  - Intent detection via regex patterns               │  │
│  │  - Query preprocessing                               │  │
│  │  - Response formatting                               │  │
│  │  - Message state management                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    MCP Server Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  BuiltInMcpServer.kt                                 │  │
│  │  - JSON-RPC 2.0 protocol handler                     │  │
│  │  - Tool registration & routing                       │  │
│  │  - Parameter validation                              │  │
│  │  - Error handling                                    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      Tool Layer                              │
│  ┌────────────────┬────────────────┬────────────────────┐  │
│  │ SearchMessages │  ListThreads   │  ListMessages      │  │
│  │     Tool       │      Tool      │      Tool          │  │
│  ├────────────────┼────────────────┼────────────────────┤  │
│  │ GetThreadSumm  │  SendMessage   │                    │  │
│  │     Tool       │      Tool      │   (Extensible)     │  │
│  └────────────────┴────────────────┴────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Data Sources                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  - SmsProviderService (SMS/MMS ContentProvider)      │  │
│  │  - ThreadRepository (Thread metadata)                │  │
│  │  - MessageRetrievalService (Semantic search)         │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. How It Works

### Intent Detection (AIAssistantViewModel.kt)

The assistant uses **simple keyword matching** to determine user intent:

```kotlin
private suspend fun processQuery(query: String): AssistantResponse {
    val lowerQuery = query.lowercase()

    return when {
        // Search intent
        lowerQuery.contains("search") || lowerQuery.contains("find") -> {
            searchMessages(extractSearchQuery(query))
        }

        // List threads intent
        lowerQuery.contains("list") &&
        (lowerQuery.contains("conversation") || lowerQuery.contains("thread")) -> {
            listThreads()
        }

        // Summary intent
        lowerQuery.contains("summar") -> {
            // Asks for clarification
        }

        // Recent messages intent
        lowerQuery.contains("recent") || lowerQuery.contains("latest") -> {
            listRecentMessages()
        }

        // Default: treat as search
        else -> searchMessages(query)
    }
}
```

**Supported Intents:**
1. **Search** - "search for messages about dinner", "find texts from mom"
2. **List threads** - "list my conversations", "show all threads"
3. **Recent messages** - "show recent messages", "latest texts"
4. **Summary** - "summarize conversation" (placeholder - not fully implemented)

### Query Processing Flow

```
User Input: "Search for messages about dinner"
     ↓
[Intent Detection] → Detected: SEARCH
     ↓
[Extract Keywords] → "dinner" (removed "search", "messages", "about")
     ↓
[Call MCP Tool] → search_messages(query="dinner", max_results=10)
     ↓
[Semantic Search] → TF-IDF embedding similarity
     ↓
[Format Results] → "Found 5 messages matching 'dinner':\n\n1. From..."
     ↓
[Display Response] → Show in chat bubble
```

---

## 3. MCP Tools

### Available Tools

#### 1. `search_messages` (SearchMessagesTool.kt)
**Purpose:** Semantic search using TF-IDF embeddings

**Parameters:**
- `query` (string, required) - Search query
- `max_results` (int, default: 10) - Max results to return
- `similarity_threshold` (float, default: 0.15) - Minimum similarity score

**Example:**
```json
{
  "name": "search_messages",
  "arguments": {
    "query": "dinner plans",
    "max_results": 10,
    "similarity_threshold": 0.15
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "results": [
      {
        "id": 123,
        "body": "Let's have dinner at 7pm",
        "sender": "+1234567890",
        "similarity": 0.85
      }
    ]
  }
}
```

#### 2. `list_threads` (ListThreadsTool.kt)
**Purpose:** List all conversation threads

**Parameters:**
- `limit` (int, default: 20) - Max threads to return

**Response:** List of threads with recipient, message count, unread count

#### 3. `list_messages` (ListMessagesTool.kt)
**Purpose:** List messages from a thread or all messages

**Parameters:**
- `thread_id` (long, optional) - Specific thread ID
- `limit` (int, default: 20) - Max messages to return

**Response:** List of messages with body, sender, type

#### 4. `get_thread_summary` (GetThreadSummaryTool.kt)
**Purpose:** Get statistical summary of a conversation

**Parameters:**
- `thread_id` (long, required) - Thread to summarize
- `max_messages` (int, default: 50) - Messages to analyze
- `include_excerpts` (bool, default: true) - Include message excerpts

**Response:** Message counts, date range, excerpts

#### 5. `send_message` (SendMessageTool.kt)
**Purpose:** Send an SMS message

**Parameters:**
- `phone_number` (string, required) - Recipient phone number
- `text` (string, required) - Message body

**Response:** Success/failure status

---

## 4. Limitations & Problems

### Current Issues

#### 1. **No Real Natural Language Understanding**
- Uses simple keyword matching (contains "search", contains "find")
- Cannot handle complex queries like:
  - "Who did I text the most last week?"
  - "What time did Sarah say we're meeting?"
  - "Did anyone mention the project deadline?"
- Misinterprets ambiguous queries
- Cannot extract entities (names, dates, locations)

#### 2. **Poor Intent Accuracy**
- **Keyword collisions:** "I can't find my search results" → triggers search
- **No context awareness:** Cannot track conversation context
- **No disambiguation:** Cannot ask clarifying questions effectively
- **Fallback to search:** Defaults everything to search, even non-search queries

#### 3. **Limited Conversation Ability**
- Cannot maintain context across turns
- No memory of previous queries
- Cannot do multi-step reasoning
- Cannot combine multiple tools (e.g., search + filter + summarize)

#### 4. **No Parameter Extraction**
- Cannot extract:
  - Contact names → thread IDs
  - Relative dates ("last week", "yesterday")
  - Number ranges ("show 5 messages")
  - Filters ("only from Sarah", "unread only")

#### 5. **Formatting Issues**
- Response formatting is hardcoded
- No markdown support
- Cannot generate different response styles
- Limited to predefined templates

#### 6. **No Learning or Personalization**
- Cannot learn from user corrections
- No query suggestions based on history
- No personalized shortcuts
- Cannot adapt to user's communication style

---

## 5. Improvement Strategies

### Option A: Enhanced Rule-Based System ⚡ (Quick Win)

**Keep current architecture, improve pattern matching:**

#### 1. **Better Intent Classification**
```kotlin
enum class UserIntent {
    SEARCH,           // Find specific messages
    LIST_THREADS,     // Show conversations
    LIST_MESSAGES,    // Show recent messages
    SUMMARIZE,        // Get summary
    FILTER,           // Filter by criteria
    COUNT,            // Count messages
    SEND,             // Send message
    UNKNOWN
}

// Multi-pattern matching with confidence scores
data class IntentMatch(val intent: UserIntent, val confidence: Double)

private fun detectIntent(query: String): IntentMatch {
    val patterns = mapOf(
        UserIntent.SEARCH to listOf(
            "search.*for" to 0.9,
            "find.*message" to 0.85,
            "look for" to 0.8,
            "where.*said" to 0.75
        ),
        UserIntent.FILTER to listOf(
            "from\\s+\\w+" to 0.9,
            "unread" to 0.85,
            "last\\s+(week|month|day)" to 0.8
        )
        // ... more patterns
    )

    // Return best match with confidence
}
```

#### 2. **Entity Extraction**
```kotlin
data class QueryEntities(
    val contactNames: List<String>,
    val dateRanges: List<DateRange>,
    val keywords: List<String>,
    val filters: Map<String, String>
)

private fun extractEntities(query: String): QueryEntities {
    val contactPattern = Regex("(?:from|to|with)\\s+(\\w+)")
    val datePattern = Regex("(yesterday|today|last\\s+\\w+)")

    return QueryEntities(
        contactNames = contactPattern.findAll(query).map { it.groups[1]!!.value }.toList(),
        dateRanges = extractDateRanges(query),
        keywords = extractKeywords(query),
        filters = extractFilters(query)
    )
}
```

#### 3. **Context Tracking**
```kotlin
data class ConversationContext(
    val lastIntent: UserIntent?,
    val lastEntities: QueryEntities?,
    val lastResults: List<Any>?,
    val threadInFocus: Long?
)

// Use context for follow-up queries:
// User: "Search for dinner plans"
// Assistant: [shows results]
// User: "Show me the latest one" <- knows "one" refers to previous search results
```

#### 4. **Smart Query Expansion**
```kotlin
private fun expandQuery(query: String, entities: QueryEntities): String {
    // Add synonyms
    // Add common misspellings
    // Expand abbreviations (tmr → tomorrow)
    // Include related terms
}
```

**Pros:**
- ✅ Fast to implement (1-2 days)
- ✅ No external dependencies
- ✅ Works offline
- ✅ Predictable behavior
- ✅ Low resource usage

**Cons:**
- ❌ Still limited understanding
- ❌ Brittle pattern matching
- ❌ Hard to maintain as complexity grows
- ❌ Cannot handle truly novel queries

---

### Option B: Lightweight On-Device LLM 🧠 (Medium Effort)

**Integrate small language model for better NLU:**

#### Recommended Models:
1. **TinyLlama-1.1B** (1.1B parameters, ~600MB)
   - Good for intent classification
   - Fast inference on mobile
   - Can extract entities

2. **Phi-2** (2.7B parameters, ~1.5GB)
   - Better reasoning
   - Good instruction following
   - Still runs on mobile

3. **Gemma-2B** (2B parameters, ~1.2GB)
   - Optimized for mobile
   - Good performance/size ratio

#### Implementation Approach:

```kotlin
// Use executorch or ONNX Runtime for mobile inference
class LlmAssistantViewModel @Inject constructor(
    private val llmInferenceEngine: LlmInferenceEngine,
    private val mcpServer: BuiltInMcpServer
) : ViewModel() {

    private suspend fun processQuery(query: String): AssistantResponse {
        // Step 1: Use LLM to extract structured intent + entities
        val prompt = """
            Extract the user's intent and entities from this query about text messages.

            Query: "$query"

            Respond with JSON:
            {
              "intent": "search|list_threads|summarize|send|...",
              "entities": {
                "contact": "...",
                "keywords": [...],
                "date_range": "...",
                "filters": {...}
              },
              "tool": "...",
              "parameters": {...}
            }
        """.trimIndent()

        val llmResponse = llmInferenceEngine.generate(prompt, maxTokens = 200)
        val structured = parseStructuredResponse(llmResponse)

        // Step 2: Call appropriate MCP tool
        val toolResult = mcpServer.callTool(
            structured.tool,
            structured.parameters
        )

        // Step 3: Use LLM to format natural response
        val formattingPrompt = """
            Format these search results as a natural response:
            Results: ${toolResult.data}
            Original query: "$query"
        """.trimIndent()

        val naturalResponse = llmInferenceEngine.generate(formattingPrompt)

        return AssistantResponse(naturalResponse, structured.tool)
    }
}
```

#### LLM Integration with ONNX Runtime:

```kotlin
class LlmInferenceEngine @Inject constructor(
    private val context: Context
) {
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    init {
        // Load quantized model (INT8 for speed)
        val modelPath = "models/phi2-quantized.ort"
        session = ortEnvironment.createSession(
            context.assets.open(modelPath).readBytes()
        )
    }

    suspend fun generate(prompt: String, maxTokens: Int = 100): String =
        withContext(Dispatchers.Default) {
            // Tokenize, run inference, decode
            val tokens = tokenizer.encode(prompt)
            val output = session!!.run(...)
            tokenizer.decode(output)
        }
}
```

**Pros:**
- ✅ Much better natural language understanding
- ✅ Can handle complex queries
- ✅ Entity extraction built-in
- ✅ Can generate natural responses
- ✅ Still works offline

**Cons:**
- ❌ Larger app size (~1-2GB for model)
- ❌ Higher resource usage (RAM, battery)
- ❌ Slower inference (1-3 seconds)
- ❌ Requires GPU acceleration for good UX
- ❌ Complex integration

**Implementation Effort:** 1-2 weeks

---

### Option C: Cloud-Based LLM API 🌐 (Best Quality)

**Use Gemini, ChatGPT, or Claude API for full NLU:**

#### Implementation:

```kotlin
class CloudAssistantViewModel @Inject constructor(
    private val geminiApi: GeminiApiClient,
    private val mcpServer: BuiltInMcpServer
) : ViewModel() {

    private suspend fun processQuery(query: String): AssistantResponse {
        // Use function calling (tool use) API
        val tools = mcpServer.getAllTools().map { tool ->
            FunctionDeclaration(
                name = tool.name,
                description = tool.description,
                parameters = tool.parameters.toJsonSchema()
            )
        }

        val response = geminiApi.generateContent(
            prompt = query,
            tools = tools,
            history = conversationHistory
        )

        // API returns which tool to call
        val functionCall = response.functionCall
        val toolResult = mcpServer.callTool(
            functionCall.name,
            functionCall.arguments
        )

        // Send result back to API for natural language response
        val finalResponse = geminiApi.generateContent(
            prompt = "Format this result naturally: $toolResult"
        )

        return AssistantResponse(finalResponse.text, functionCall.name)
    }
}
```

#### Use Gemini's Function Calling:

```kotlin
// Define tools in Gemini format
val tools = listOf(
    Tool(
        functionDeclarations = listOf(
            FunctionDeclaration(
                name = "search_messages",
                description = "Search for messages by semantic meaning",
                parameters = Schema(
                    type = "object",
                    properties = mapOf(
                        "query" to Schema(type = "string"),
                        "max_results" to Schema(type = "integer")
                    )
                )
            )
        )
    )
)

val model = generativeModel(
    modelName = "gemini-1.5-flash",
    tools = tools
)
```

**Pros:**
- ✅ Best natural language understanding
- ✅ Handles any complex query
- ✅ Multi-turn conversations
- ✅ Can reason and plan
- ✅ Minimal app size impact
- ✅ Easy to update/improve

**Cons:**
- ❌ Requires internet connection
- ❌ Privacy concerns (messages sent to cloud)
- ❌ API costs
- ❌ Latency (500ms - 2s)
- ❌ Dependency on external service

**Implementation Effort:** 3-5 days

---

### Option D: Hybrid Approach 🎯 (Recommended)

**Combine rule-based + cloud LLM with graceful fallback:**

```kotlin
class HybridAssistantViewModel @Inject constructor(
    private val ruleBasedProcessor: RuleBasedQueryProcessor,
    private val cloudLlm: CloudLlmClient,
    private val mcpServer: BuiltInMcpServer,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    private suspend fun processQuery(query: String): AssistantResponse {
        // Try rule-based first (fast path)
        val intentMatch = ruleBasedProcessor.detectIntent(query)

        return if (intentMatch.confidence > 0.8) {
            // High confidence - use rule-based
            Timber.d("Using rule-based processing (confidence: ${intentMatch.confidence})")
            ruleBasedProcessor.process(query, intentMatch.intent)
        } else if (isNetworkAvailable() && userAllowsCloudProcessing()) {
            // Low confidence + network available - use cloud LLM
            Timber.d("Using cloud LLM for complex query")
            cloudLlm.process(query)
        } else {
            // Fallback: best-effort rule-based
            Timber.d("Fallback to rule-based processing")
            ruleBasedProcessor.processBestEffort(query)
        }
    }
}
```

**Routing Logic:**
- Simple queries (>80% confidence) → Rule-based ⚡
- Complex queries + online → Cloud LLM 🌐
- Complex queries + offline → Enhanced rule-based with "limited understanding" warning
- User preference controls cloud usage

**Pros:**
- ✅ Fast for common queries
- ✅ Handles complex queries well
- ✅ Works offline (degraded)
- ✅ Privacy-conscious (rules first)
- ✅ Best of both worlds

**Cons:**
- ❌ Most complex to implement
- ❌ Need to maintain both systems
- ❌ Inconsistent behavior

**Implementation Effort:** 1 week

---

## 6. Recommended Improvements (Prioritized)

### Phase 1: Quick Wins (1-2 days) 🎯

1. **Improve pattern matching**
   - Add more intent patterns
   - Add confidence scoring
   - Better default handling

2. **Add entity extraction**
   - Contact name extraction
   - Date range parsing ("last week")
   - Filter extraction

3. **Better response formatting**
   - Markdown support
   - Clickable links to threads/messages
   - Nicer error messages

4. **Add query suggestions**
   - Show example queries in empty state
   - Recent queries dropdown
   - Auto-complete common queries

### Phase 2: On-Device LLM (1 week) 🧠

1. **Integrate TinyLlama or Phi-2**
   - Use ONNX Runtime for inference
   - Quantize model to INT8 for speed
   - Implement caching

2. **Tool schema generation**
   - Auto-generate tool descriptions for LLM
   - Convert MCP tools to function calling format

3. **Response generation**
   - Use LLM for natural responses
   - Template fallback for speed

### Phase 3: Cloud Enhancement (Optional) 🌐

1. **Add Gemini API integration**
   - Function calling support
   - Conversation history
   - Privacy controls

2. **Hybrid routing**
   - Smart detection of query complexity
   - Confidence threshold tuning
   - User preference controls

---

## 7. Code Examples for Common Improvements

### Better Intent Detection with Confidence:

```kotlin
data class IntentPattern(
    val pattern: Regex,
    val confidence: Double,
    val entityExtractors: List<(MatchResult) -> Pair<String, Any>>
)

private val intentPatterns = mapOf(
    UserIntent.SEARCH to listOf(
        IntentPattern(
            pattern = Regex("(?:search|find)\\s+(?:messages?|texts?)\\s+(?:about|for|with)\\s+(.+)", RegexOption.IGNORE_CASE),
            confidence = 0.95,
            entityExtractors = listOf({ match -> "keywords" to match.groups[1]!!.value })
        ),
        IntentPattern(
            pattern = Regex("(?:where|when)\\s+did\\s+(.+?)\\s+(?:say|mention)\\s+(.+)", RegexOption.IGNORE_CASE),
            confidence = 0.9,
            entityExtractors = listOf(
                { match -> "contact" to match.groups[1]!!.value },
                { match -> "keywords" to match.groups[2]!!.value }
            )
        )
    ),
    UserIntent.FILTER to listOf(
        IntentPattern(
            pattern = Regex("(?:show|list)\\s+(?:messages?|texts?)\\s+from\\s+(.+?)\\s+(?:from|in)\\s+(.+)", RegexOption.IGNORE_CASE),
            confidence = 0.9,
            entityExtractors = listOf(
                { match -> "contact" to match.groups[1]!!.value },
                { match -> "date_range" to match.groups[2]!!.value }
            )
        )
    )
)

private fun detectIntentWithConfidence(query: String): Pair<UserIntent, Double> {
    var bestMatch: Pair<UserIntent, Double> = UserIntent.UNKNOWN to 0.0

    intentPatterns.forEach { (intent, patterns) ->
        patterns.forEach { pattern ->
            if (pattern.pattern.containsMatchIn(query)) {
                if (pattern.confidence > bestMatch.second) {
                    bestMatch = intent to pattern.confidence
                }
            }
        }
    }

    return bestMatch
}
```

### Contact Name Resolution:

```kotlin
private suspend fun resolveContactToThreadId(contactName: String): Long? {
    // Search threads by recipient name (fuzzy match)
    val threads = threadRepository.getAllThreadsSnapshot()

    return threads.firstOrNull { thread ->
        thread.recipientName?.contains(contactName, ignoreCase = true) == true
    }?.id
}

// Use in query processing:
if (entities.contactNames.isNotEmpty()) {
    val threadId = resolveContactToThreadId(entities.contactNames.first())
    if (threadId != null) {
        // Use thread-specific search
        params["thread_id"] = threadId
    }
}
```

### Date Range Extraction:

```kotlin
data class DateRange(val start: Long, val end: Long)

private fun extractDateRange(query: String): DateRange? {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    return when {
        query.contains("today", ignoreCase = true) -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            DateRange(start = calendar.timeInMillis, end = now)
        }
        query.contains("yesterday", ignoreCase = true) -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            DateRange(start = start, end = calendar.timeInMillis)
        }
        query.contains("last week", ignoreCase = true) -> {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            DateRange(start = calendar.timeInMillis, end = now)
        }
        Regex("last\\s+(\\d+)\\s+(days?|weeks?|months?)").find(query)?.let { match ->
            val amount = match.groups[1]!!.value.toInt()
            val unit = when (match.groups[2]!!.value.lowercase()) {
                in listOf("day", "days") -> Calendar.DAY_OF_MONTH
                in listOf("week", "weeks") -> Calendar.WEEK_OF_YEAR
                else -> Calendar.MONTH
            }
            calendar.add(unit, -amount)
            DateRange(start = calendar.timeInMillis, end = now)
        }
        else -> null
    }
}
```

### Conversation Context:

```kotlin
data class ConversationTurn(
    val userQuery: String,
    val assistantResponse: String,
    val toolUsed: String?,
    val entities: QueryEntities,
    val timestamp: Long
)

class ContextAwareAssistantViewModel @Inject constructor(
    private val mcpServer: BuiltInMcpServer
) : ViewModel() {

    private val conversationHistory = mutableListOf<ConversationTurn>()

    private suspend fun processQueryWithContext(query: String): AssistantResponse {
        val resolvedQuery = resolveAnaphora(query)

        // ... process query ...

        // Save to history
        conversationHistory.add(
            ConversationTurn(
                userQuery = query,
                assistantResponse = response.content,
                toolUsed = response.toolUsed,
                entities = extractedEntities,
                timestamp = System.currentTimeMillis()
            )
        )

        return response
    }

    private fun resolveAnaphora(query: String): String {
        // Handle references to previous context
        val lastTurn = conversationHistory.lastOrNull()

        return when {
            // "Show me more" → repeat last search with higher limit
            query.matches(Regex("show\\s+me\\s+more", RegexOption.IGNORE_CASE)) -> {
                lastTurn?.userQuery ?: query
            }

            // "The latest one" → reference to last result set
            query.matches(Regex("the\\s+(latest|first|last)\\s+one", RegexOption.IGNORE_CASE)) -> {
                // Extract from last results
                lastTurn?.userQuery ?: query
            }

            else -> query
        }
    }
}
```

---

## 8. Testing Strategy

### Unit Tests:

```kotlin
@Test
fun `intent detection - search queries`() {
    val testCases = listOf(
        "search for messages about dinner" to UserIntent.SEARCH,
        "find texts from mom" to UserIntent.SEARCH,
        "where did sarah mention the meeting" to UserIntent.SEARCH
    )

    testCases.forEach { (query, expectedIntent) ->
        val (intent, confidence) = viewModel.detectIntent(query)
        assertEquals(expectedIntent, intent)
        assertTrue(confidence > 0.7)
    }
}

@Test
fun `entity extraction - contact names`() {
    val query = "show messages from John about the project"
    val entities = viewModel.extractEntities(query)

    assertEquals(listOf("John"), entities.contactNames)
    assertEquals(listOf("project"), entities.keywords)
}

@Test
fun `date range extraction`() {
    val testCases = mapOf(
        "messages from last week" to 7,
        "texts from yesterday" to 1,
        "last 30 days" to 30
    )

    testCases.forEach { (query, expectedDays) ->
        val range = viewModel.extractDateRange(query)
        assertNotNull(range)
        val actualDays = TimeUnit.MILLISECONDS.toDays(range!!.end - range.start)
        assertEquals(expectedDays.toLong(), actualDays)
    }
}
```

### Integration Tests:

```kotlin
@Test
fun `end-to-end search flow`() = runTest {
    // Given
    val query = "search for dinner plans"

    // When
    viewModel.sendMessage(query)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertEquals(2, state.messages.size) // user + assistant
    assertTrue(state.messages[1].content.contains("Found"))
    assertEquals("search_messages", state.messages[1].toolUsed)
}
```

---

## 9. Performance Considerations

### Current Performance:
- **Intent detection:** <1ms (regex matching)
- **Tool execution:** 50-500ms (depends on data size)
- **Response formatting:** <5ms
- **Total response time:** 50-500ms ✅

### With On-Device LLM:
- **LLM inference:** 500-3000ms (model size dependent)
- **Tool execution:** 50-500ms
- **Total response time:** 1-3 seconds ⚠️

**Mitigation strategies:**
- Use quantized models (INT8/INT4)
- GPU acceleration (NNAPI/Hexagon)
- Caching for repeated queries
- Show "thinking" animation
- Stream LLM responses

---

## 10. Summary

| Approach | Complexity | Quality | Speed | Offline | Privacy | Recommendation |
|----------|-----------|---------|-------|---------|---------|----------------|
| **Enhanced Rules** | Low | Medium | Fast | ✅ | ✅ | ⭐⭐⭐ Quick win |
| **On-Device LLM** | Medium | High | Medium | ✅ | ✅ | ⭐⭐ Best balance |
| **Cloud LLM** | Low | Very High | Medium | ❌ | ⚠️ | ⭐ Optional |
| **Hybrid** | High | Very High | Fast/Medium | Partial | ✅ | ⭐⭐⭐ Long-term |

### Recommended Path Forward:

1. **Week 1:** Enhanced rule-based system (Phase 1)
   - Low hanging fruit
   - Immediate UX improvement
   - Foundation for future work

2. **Week 2-3:** Evaluate LLM options
   - Test TinyLlama/Phi-2 on Android
   - Benchmark performance
   - Measure accuracy improvement

3. **Week 4+:** Implement chosen approach
   - On-device LLM if performance acceptable
   - Otherwise stick with enhanced rules
   - Add cloud LLM as opt-in premium feature

---

**Current Status:** ⚠️ Basic rule-based system (v1.0)
**Target:** 🎯 Hybrid rule + on-device LLM (v2.0)
**Stretch Goal:** 🚀 Full conversational AI with cloud option (v3.0)
