# VectorText Product Requirements Document (PRD)

## 1. Overview
VectorText is a full-featured Android SMS/MMS/RCS messaging application and AI-enhanced communication platform. It replaces the default Android messaging app and introduces on-device semantic search using a vector store, plus a built-in Model Context Protocol (MCP) server for local AI tool access.  
Package name: **com.vanespark.vectortext**

## 2. Product Goals
### Primary Goals
- Provide a stable, modern default SMS/MMS/RCS app.
- **Deliver a beautiful, polished UI that sets a new standard for Android messaging apps.**
- Implement an on-device vector store for semantic message search.
- Expose an MCP server for AI tools to access and process messages.
- Ensure complete on-device privacy with no cloud dependencies.

### Secondary Goals
- Provide AI-assisted message summaries, automations, and insights.
- Integrate with local LLM engines (PocketGem Agent, Ollama, LM Studio, etc.).
- Maintain excellent performance with large message histories.
- **Create delightful micro-interactions and smooth animations throughout the app.**

## 3. Key Features

### 3.1 Messaging System
- Full SMS support (send, receive).
- MMS (media, group messages).
- RCS (if available via Android Jibe APIs).
- Delivery and read receipts.
- Multi-SIM support.
- Thread pinning, muting, archiving, swipe actions.

### 3.2 Vector Store & Semantic Search
- Automatic embedding of incoming/outgoing messages using TF-IDF-based word hashing approach (384-dimensional vectors).
- Embeddings stored directly in SQLite database (no external vector index required).
- **Similarity Search Method**: Cosine similarity with batched processing (50 chunks per batch to avoid CursorWindow limits).
- **Default Similarity Threshold**: 0.15 (lower threshold for better recall).
- Semantic search for natural-language queries:
  - "Find the message where Sarah sent me the gate code."
  - "Show me things about the roof repair."
- Filters: sender, date, thread, similarity threshold.
- Background embedding pipeline with batching.
- **Note**: MVP uses TF-IDF for speed and device compatibility. Phase 6 roadmap includes upgrade to neural embedding models (all-MiniLM-L6-v2, MediaPipe Text Embedder) for significantly improved search accuracy.

### 3.3 MCP Server
Runs fully on-device as a **Built-in MCP Server** (following android-pocketgem-agent pattern):
- Uses pseudo-URL `builtin://vectortext` for identification
- Implements MCP JSON-RPC protocol (tools/list, tools/call)
- All tools execute locally (no network calls except for send_message)

#### Tools:
- **`search_messages`**: Searches messages using semantic similarity
  - Parameters:
    - `query` (string, required): The search query or question
    - `max_results` (number, optional): Maximum number of message excerpts to return (default: 5, max: 20)
  - Uses **cosine similarity** with 0.15 default threshold for better recall
  - Returns formatted results with sender, timestamp, and relevance score

- **`list_messages`**: Lists recent messages from a thread
  - Parameters:
    - `thread_id` (string, optional): Specific thread ID to list messages from
    - `limit` (number, optional): Maximum number of messages (default: 20)

- **`send_message`**: Sends a new SMS/MMS message
  - Parameters:
    - `phone_number` (string, required): Recipient phone number
    - `text` (string, required): Message content

- **`list_threads`**: Lists all message threads
  - Parameters:
    - `limit` (number, optional): Maximum threads to return (default: 50)

- **`get_thread_summary`**: Generates AI summary of a thread
  - Parameters:
    - `thread_id` (string, required): Thread ID to summarize

#### Resources:
- `/messages/*`
- `/threads/*`
- `/embeddings/*`

#### Events:
- `onNewMessage`
- `onThreadUpdated`

### 3.4 Privacy & Security
- All data stored and processed on-device.
- Zero telemetry.
- Encrypted SQLite and encrypted vector index.
- Optional passcode/biometric app lock.
- Local encrypted backup/restore option.

### 3.5 UI/UX — Beautiful & Modern Design
**Design Philosophy:** VectorText aims to be the most visually stunning messaging app on Android, combining cutting-edge AI features with a polished, intuitive interface that delights users.

**Built with Material You (Material 3):**
- **Dynamic Color System**: Full Material You theming that adapts to user's wallpaper
- **Fluid Animations**: Smooth, predictable motion using Material motion system
- **Modern Typography**: Roboto/Product Sans with proper hierarchy and readability
- **Adaptive Layouts**: Responsive design for phones, foldables, and tablets

**Key UI Features:**

**Conversation List:**
- Beautiful conversation cards with rounded corners and subtle shadows
- Contact avatars with gradient rings for unread messages
- Smooth swipe actions (archive, pin, delete) with haptic feedback
- Pull-to-refresh with elegant animation
- Smart categories with collapsible sections
- Empty state illustrations (vector art, delightful)

**Chat Thread:**
- Bubble design with modern chat aesthetics (rounded, color-coded)
- Media viewer with edge-to-edge images, zoom gestures, shared element transitions
- Inline reactions and reply threads
- Message timestamps that fade in on scroll
- Quick action toolbar (copy, share, search similar messages)
- Typing indicators with animated dots
- Send button morphs into voice/attach based on text state

**Semantic Search Interface:**
- Floating search FAB with morphing animation
- Full-screen search overlay with blur background
- Live search suggestions as you type
- Results with relevance scores visualized as color-coded badges
- Highlight matched terms in message previews
- Search history chips with quick filters

**AI Assistant:**
- Floating bubble that follows Material 3 extended FAB patterns
- Expands into bottom sheet with smooth spring animation
- Chat interface for "Ask your messages" feature
- Loading states with shimmer effects
- Result cards with syntax highlighting for structured data

**Settings & Configuration:**
- Modern settings layout with preference cards
- Toggle switches with satisfying animations
- Color picker for theme customization
- Model management with download progress rings
- Dark mode with AMOLED black option
- Accessibility options (font scaling, high contrast, reduce motion)

**Visual Polish:**
- Micro-interactions throughout (button press states, ripple effects)
- Skeleton loading screens (no blank states)
- Contextual illustrations for empty states and errors
- Smooth page transitions using shared element animations
- Haptic feedback for important actions
- Edge-to-edge design (immersive experience)
- Bottom navigation with icon morphing animations

**Theme Options:**
- **Light**: Clean whites with subtle grays
- **Dark**: Deep backgrounds with elevated surfaces
- **AMOLED**: Pure black for battery savings on OLED screens
- **Dynamic Color**: Material You automatic theming from wallpaper
- **Custom Accents**: User-selectable primary/secondary colors

**Inspiration:**
- Google Messages for conversation flow
- Telegram for feature richness and speed
- Signal for privacy-focused clean design
- Apple Messages for polish and attention to detail
- WhatsApp for familiarity and ease of use

## 4. Architecture

### 4.1 Platform
- Android 9+ (API 28+)
- Kotlin + Jetpack Compose
- WorkManager for background tasks
- SQLite + Room ORM (embeddings stored directly in database as text blobs)

### 4.2 Embedding System
**Initial Implementation (MVP):**
- **TF-IDF-based word hashing** with 384-dimensional vectors (following android-pocketgem-agent pattern)
- **No external model files required** - pure algorithmic approach
- Components:
  - `TextEmbeddingService`: Generates embeddings using TF-IDF + word hashing
  - `MessageRetrievalService`: Performs cosine similarity search with batched processing
  - Vocabulary and IDF scores updated as messages are processed
  - Stop word filtering and tokenization

**Future Upgrade Path** (see Phase 6 roadmap):
- all-MiniLM-L6-v2 (ONNX/TFLite) - Recommended
- MediaPipe Text Embedder - Google's official solution
- nomic-embed-text-v1 (GGUF) - Advanced option
- Dual embedding system for smooth migration

**Embedding Metadata:**
- messageId
- threadId
- timestamp
- senderType (user/contact)
- embedding (384-dimensional float array stored as comma-separated string)  

### 4.3 MCP Server Architecture
**Built-in MCP Server** (following android-pocketgem-agent pattern):
- **In-process server** using pseudo-URL `builtin://vectortext` for identification
- **JSON-RPC 2.0 compliance** (tools/list, tools/call methods)
- **Zero network overhead** - all tools execute locally
- **Tool registry** with automatic registration
- Components:
  - `BuiltInMcpServer`: Core server implementation
  - `MessageSearchTool`: Implements `search_messages` with cosine similarity
  - `MessageListTool`, `SendMessageTool`, `ThreadListTool`, `ThreadSummaryTool`
- **Optional External Access**: Can be exposed via HTTP/WebSocket for external MCP clients (PocketGem Agent, Claude Desktop)
  - When enabled: Local-only server on `127.0.0.1` random port
  - Routes external requests to built-in server
  - Maintains same tool interface

## 5. Roadmap

**UI/UX Priority:** Beautiful, polished design is a priority throughout ALL phases. Each feature should be implemented with careful attention to visual design, animations, and user experience.

### Phase 1 — Messaging Core + Beautiful UI Foundation
- SMS/MMS functionality
- **Material You theming system** (dynamic colors, light/dark/AMOLED themes)
- **Polished conversation list** (avatars, swipe actions, smooth animations)
- **Beautiful chat thread UI** (bubble design, media viewer, quick actions)
- Basic search with elegant UI
- Default messaging app handling
- **Micro-interactions and haptic feedback**
- **Skeleton loading states and empty state illustrations**

### Phase 2 — Vector Store + Search UI Excellence
- Embedding pipeline
- Semantic search
- Index management
- **Stunning search UI** (floating FAB, full-screen overlay, live suggestions)
- **Relevance score visualizations** (color-coded badges)
- **Search result highlighting** and preview animations

### Phase 3 — MCP Server + Developer Experience
- Tool exposure
- Integration with local AI clients
- **Beautiful developer menu** (debug info cards, connection status)
- Settings UI for MCP configuration

### Phase 4 — AI Features + Delightful Interactions
- Summaries per thread with **beautiful result cards**
- Smart categories with **smooth category transitions**
- Insights dashboard with **data visualizations**
- **AI assistant floating bubble** with conversational UI

### Phase 5 — Advanced Features + Polish
- Local encrypted backups with **progress animations**
- Automations ("rules engine") with **visual rule builder**
- AI-enhanced contact profiles with **rich contact cards**
- **Final polish pass**: animation tuning, performance optimization, accessibility audit

### Phase 6 — Neural Embedding Upgrade
**Migration from TF-IDF to Neural Embedding Models**

**Rationale:**
While TF-IDF provides fast, lightweight semantic search suitable for MVP, neural embedding models offer significantly better semantic understanding and search accuracy.

**Target Models:**
1. **all-MiniLM-L6-v2** (Recommended)
   - Size: ~80MB quantized (int8)
   - Dimensions: 384 (same as current TF-IDF)
   - Format: ONNX Runtime Mobile or TensorFlow Lite
   - Performance: ~100-200ms per message on mid-range devices
   - Accuracy: Industry-standard sentence embeddings

2. **MediaPipe Text Embedder**
   - Google's official on-device embedding solution
   - Optimized for mobile (GPU acceleration support)
   - Pre-trained on diverse text corpora
   - Direct Android API integration

3. **nomic-embed-text-v1** (GGUF)
   - Size: ~100MB quantized
   - Advanced semantic understanding
   - Better for complex queries
   - Requires GGML runtime integration

**Implementation Plan:**
1. **Dual Embedding System** (Transition Period)
   - Keep TF-IDF for backward compatibility
   - Add neural model as opt-in feature
   - Store embedding_version field to track method used
   - Allow users to choose based on device capability

2. **Model Download & Management**
   - Add model catalog screen (similar to LLM model management)
   - Resumable downloads with checksum verification
   - Model storage in app-private directory
   - Quick test feature to verify model performance

3. **Background Re-indexing**
   - WorkManager job to re-embed existing messages
   - Progressive re-indexing (batch by batch)
   - User can continue using TF-IDF during migration
   - Progress indicator in settings

4. **Hybrid Search** (Advanced)
   - Use TF-IDF for fast pre-filtering
   - Apply neural embeddings for top N results
   - Best of both: speed + accuracy

**Database Schema Changes:**
```sql
-- Add model tracking
ALTER TABLE messages ADD COLUMN embedding_model TEXT DEFAULT 'tfidf';
ALTER TABLE messages ADD COLUMN embedding_dim INTEGER DEFAULT 384;

-- New table for model metadata
CREATE TABLE embedding_models (
    id INTEGER PRIMARY KEY,
    model_name TEXT UNIQUE,
    model_path TEXT,
    dimension INTEGER,
    size_bytes INTEGER,
    is_active INTEGER DEFAULT 0,
    downloaded_at INTEGER,
    sha256 TEXT
);
```

**Performance Targets:**
- Embedding generation: < 200ms per message (neural model)
- Search latency: < 1s for 10K messages
- Model load time: < 5s cold start
- Memory overhead: < 200MB during embedding generation

**Settings UI:**
- Embedding Model selection (TF-IDF / Neural Model)
- Model download & management screen
- Re-indexing progress indicator
- Toggle for GPU acceleration (if available)
- Clear cache / force re-index option

**Backward Compatibility:**
- TF-IDF remains default for devices with < 4GB RAM
- Neural models optional download
- Automatic fallback to TF-IDF if model fails to load
- No breaking changes to MCP server API

**Success Metrics:**
- Search accuracy improvement: > 30% better relevance
- User satisfaction: Positive feedback on search quality
- Adoption rate: > 40% of users with capable devices enable neural models
- Performance: No ANRs or significant battery impact

## 6. Risks
| Risk | Mitigation |
|------|------------|
| High battery usage | Batch embeddings during idle/charging; TF-IDF approach is lightweight |
| Database size with embeddings | Store embeddings as compressed strings; implement pruning for old messages |
| Search accuracy with TF-IDF | Lower similarity threshold (0.15) for better recall; upgrade to neural models in future |
| Device performance variance | TF-IDF approach works on all devices; batched processing (50 chunks) prevents memory issues |
| RCS inconsistencies | Use fallback to MMS/SMS automatically |

## 6.5. Design Principles & Quality Standards

**Core Design Principles:**

1. **Beauty is Not Optional**
   - Every screen, every interaction must be carefully designed
   - If it doesn't look good, it's not done
   - Visual polish is a feature, not a nice-to-have

2. **Smooth, Not Slow**
   - 60fps animations are the minimum standard
   - Use spring physics for natural motion
   - Jank is unacceptable
   - Performance and beauty must coexist

3. **Delight in Details**
   - Micro-interactions make the difference
   - Loading states should be engaging, not boring
   - Empty states are opportunities for personality
   - Every tap should feel responsive

4. **Accessibility First**
   - Beautiful for everyone, including users with disabilities
   - TalkBack navigation must be logical and complete
   - Dynamic type support is mandatory
   - High contrast modes must maintain visual appeal

5. **Consistent, Not Boring**
   - Follow Material You patterns as foundation
   - Add custom refinements for premium feel
   - Maintain consistency across the app
   - Surprise and delight without breaking expectations

**Quality Checklist (Every Feature):**

**Visual Design:**
- [ ] Follows Material 3 guidelines with custom refinements
- [ ] Dynamic color theming applied correctly
- [ ] Proper spacing, padding, and alignment throughout
- [ ] Typography hierarchy is clear and readable
- [ ] Colors meet WCAG AA contrast requirements minimum
- [ ] Dark mode implementation is thoughtful (not just inverted colors)

**Animation & Motion:**
- [ ] All transitions are smooth (60fps minimum)
- [ ] Spring physics used for natural movement
- [ ] Shared element transitions for related content
- [ ] Loading states use skeleton screens or shimmer effects
- [ ] No abrupt state changes (fade/slide instead)
- [ ] Motion can be reduced via accessibility settings

**Interaction:**
- [ ] Tap targets are minimum 48dp
- [ ] Haptic feedback for important actions
- [ ] Ripple effects on all tappable elements
- [ ] Swipe gestures have clear visual feedback
- [ ] Long-press actions have helpful tooltips
- [ ] Multi-touch gestures work reliably

**Performance:**
- [ ] Screen loads in < 300ms
- [ ] No dropped frames during animations
- [ ] Smooth scrolling with large lists (RecyclerView optimization)
- [ ] Images load progressively (blur-up technique)
- [ ] No blocking operations on UI thread

**Polish:**
- [ ] No placeholder text in production
- [ ] Proper empty states with illustrations
- [ ] Error messages are helpful and friendly
- [ ] Success states are celebrated (subtle animations)
- [ ] Edge cases handled gracefully
- [ ] No Lorem ipsum anywhere

**Inspiration & Reference Apps:**
- **Google Messages**: Conversation flow, Material You implementation
- **Telegram**: Rich features, speed, custom animations
- **Signal**: Clean design, privacy-focused UI
- **Apple Messages**: Polish, attention to detail, delightful interactions
- **WhatsApp**: Simplicity, familiarity, reliability
- **Material Design 3 Gallery**: Latest patterns and components

**Design Tools:**
- Figma for mockups and prototypes
- Material Theme Builder for color schemes
- Lottie for complex animations
- Rive for interactive animations (optional)

**Non-Negotiables:**
- No jank or frame drops
- No ugly loading spinners (use skeleton screens)
- No generic Material Design default look
- No inconsistent spacing or alignment
- No inaccessible UI elements
- No shortcuts on visual quality

## 7. Technical Implementation Details

### 7.1 Text Embedding Service
Following the android-pocketgem-agent pattern:

**Key Components:**
```kotlin
class TextEmbeddingService {
    companion object {
        const val EMBEDDING_DIMENSION = 384  // Standard dimension
        const val MIN_WORD_LENGTH = 3
        // Stop words list (common words to filter out)
    }

    // Core methods:
    suspend fun generateEmbedding(text: String): FloatArray
    suspend fun generateEmbeddingsWithProgress(texts: List<String>,
        onProgress: ((Float, String) -> Unit)?): List<FloatArray>
    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float
    fun updateCorpus(documents: List<String>)
    fun embeddingToString(embedding: FloatArray): String
    fun stringToEmbedding(embeddingString: String): FloatArray
}
```

**Algorithm:**
1. **Tokenization**: Lowercase, remove punctuation, filter stop words and short words
2. **TF-IDF Calculation**:
   - TF (Term Frequency): Count of word in message / total words
   - IDF (Inverse Document Frequency): ln((total_messages + 1) / (messages_with_word + 1)) + 1
3. **Word Hashing**: Hash word to index (word.hashCode() % 384)
4. **Vector Construction**: Accumulate TF-IDF scores at hashed indices
5. **Normalization**: Normalize vector to unit length

### 7.2 Message Retrieval Service
```kotlin
class MessageRetrievalService {
    suspend fun retrieveRelevantMessages(
        query: String,
        maxResults: Int = 5,
        similarityThreshold: Float = 0.15f
    ): List<String>

    suspend fun buildRagContext(
        query: String,
        maxResults: Int = 3,
        maxContextLength: Int = 1000
    ): String?
}
```

**Search Algorithm:**
1. Generate embedding for search query
2. Fetch message chunks in batches (50 per batch) from database
3. For each chunk:
   - Deserialize embedding from string
   - Calculate cosine similarity with query embedding
   - Keep if similarity >= threshold (0.15)
4. Sort all results by similarity (descending)
5. Take top N results
6. Format with metadata (sender, timestamp, relevance %)

**Batching Strategy:**
- Batch size: 50 chunks (prevents CursorWindow 2MB limit)
- Each embedding: ~4-5KB as string
- Batch memory: ~250KB (safe margin)

### 7.3 Database Schema
```sql
-- Messages table (extends existing SMS/MMS schema)
CREATE TABLE messages (
    id INTEGER PRIMARY KEY,
    thread_id INTEGER,
    address TEXT,
    body TEXT,
    date INTEGER,
    type INTEGER,  -- 1=inbox, 2=sent
    -- RAG fields:
    embedding TEXT,  -- Comma-separated floats
    embedding_version INTEGER DEFAULT 1,
    last_indexed INTEGER
);

-- Index for efficient queries
CREATE INDEX idx_messages_thread ON messages(thread_id);
CREATE INDEX idx_messages_last_indexed ON messages(last_indexed);
```

### 7.4 Built-in MCP Server Implementation
```kotlin
@Singleton
class BuiltInMcpServer {
    companion object {
        const val BUILTIN_SERVER_URL = "builtin://vectortext"
    }

    private val tools = mutableMapOf<String, Tool>()

    fun registerBuiltInTool(tool: Tool)
    suspend fun handleListToolsRequest(request: McpRequest): McpResponse
    suspend fun handleCallToolRequest(request: McpRequest): McpResponse
}

// Message Search Tool
@Singleton
class MessageSearchTool @Inject constructor(
    private val messageRetrievalService: MessageRetrievalService
) : Tool {
    override val name = "search_messages"
    override val description = "Searches messages using semantic similarity"
    override val parameters = listOf(
        ToolParameter("query", ParameterType.STRING, required = true),
        ToolParameter("max_results", ParameterType.NUMBER, required = false, default = "5")
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult {
        val query = arguments["query"] as String
        val maxResults = (arguments["max_results"] as? Number)?.toInt() ?: 5

        val results = messageRetrievalService.retrieveRelevantMessages(
            query, maxResults, similarityThreshold = 0.15f
        )

        return ToolResult(success = true, data = formatResults(results))
    }
}
```

### 7.5 Performance Characteristics
| Metric | Target | Notes |
|--------|--------|-------|
| **Embedding Generation** | < 50ms per message | TF-IDF is fast, no neural network |
| **Batch Indexing** | 1000 messages/min | Background WorkManager job |
| **Search Latency** | < 500ms for 10K messages | Batched cosine similarity |
| **Memory Usage** | < 50MB for search | Batch processing limits memory |
| **Storage Overhead** | ~2KB per message | 384 floats as comma-separated string |

### 7.6 Reference Implementation
The TF-IDF embedding and similarity search implementation is based on **android-pocketgem-agent**:

**Source Files to Reference:**
- `TextEmbeddingService.kt`: TF-IDF embedding generation, cosine similarity
  - Path: `app/src/main/java/com/vanespark/pocketgem/domain/service/TextEmbeddingService.kt`

- `RagRetrievalService.kt`: Batched similarity search with database integration
  - Path: `app/src/main/java/com/vanespark/pocketgem/domain/service/RagRetrievalService.kt`

- `RagTool.kt`: MCP tool implementation for `search_documents`
  - Path: `app/src/main/java/com/vanespark/pocketgem/domain/agent/tools/RagTool.kt`

- `BuiltInMcpServer.kt`: Built-in MCP server pattern
  - Path: `app/src/main/java/com/vanespark/pocketgem/domain/agent/mcp/BuiltInMcpServer.kt`

**Test Files:**
- `TextEmbeddingServiceTest.kt`: Unit tests for embedding generation
- `RagRetrievalServiceTest.kt`: Integration tests for similarity search

**Adaptation Notes:**
- Replace "document" terminology with "message" terminology
- Adapt chunk size from document chunks to message granularity
- Add thread_id grouping for message context
- Integrate with Android SMS/MMS provider APIs
- Add sender/recipient metadata to search results

## 8. Name & Branding
**App name:** VectorText
**Package ID:** com.vanespark.vectortext

**Brand Identity:**
- **Visual Style**: Sleek, modern, minimalist with attention to detail
- **Core Values**: Privacy, Intelligence, Beauty, Speed
- **Design Language**: Material You + custom refinements for premium feel
- **Color Philosophy**: Dynamic and adaptive, respecting user preferences
- **Typography**: Clean, readable, hierarchy-focused
- **Iconography**: Custom vector icons with consistent style

**Brand Positioning:**
- "The messaging app for people who care about privacy AND design"
- "Semantic search meets beautiful design"
- "Your messages, smarter and more beautiful"
- Premium feel, open-source heart

**Target Aesthetic:**
- Technical sophistication without complexity
- Warmth and approachability in a privacy-first app
- Polished like a flagship Google app
- Modern without being trendy
- Timeless design that won't feel dated

