# VectorText TODO List

## Phase 1 — Messaging Core + Beautiful UI Foundation ✨

### Foundation Setup
- [ ] Initialize Android project structure with Kotlin + Jetpack Compose
  - Package: com.vanespark.vectortext
  - Min SDK: API 28 (Android 9+)
  - Target SDK: Latest stable
  - Dependencies: Compose, Room, Hilt, WorkManager

- [ ] Set up Hilt dependency injection framework
  - Application module
  - Database module
  - Repository module
  - ViewModel module

- [ ] Create Material You theming system
  - Dynamic color support
  - Light/Dark/AMOLED theme variants
  - Theme switcher in settings
  - Color scheme extraction from wallpaper

### Database & Data Layer
- [ ] Design and implement Room database schema
  - Messages table (id, thread_id, address, body, date, type, embedding fields)
  - Threads table (id, recipient, last_message, unread_count, is_pinned, is_archived)
  - Contacts table (id, name, phone, avatar_uri)
  - Indexes for performance

- [ ] Create data models (entities)
  - Message entity
  - Thread entity
  - Contact entity

- [ ] Implement DAOs (Data Access Objects)
  - MessageDao (CRUD operations, queries)
  - ThreadDao
  - ContactDao

- [ ] Create Repository layer
  - MessageRepository
  - ThreadRepository
  - ContactRepository

### SMS/MMS Core Functionality
- [ ] Implement SMS/MMS provider integration
  - Read SMS permissions handling
  - ContentProvider queries for existing messages
  - ContentObserver for new message detection

- [ ] Create SMS receiver for incoming messages
  - BroadcastReceiver for SMS_RECEIVED
  - Parse and store incoming messages
  - Update thread metadata

- [ ] Implement message sending
  - SmsManager integration
  - MMS support with media attachments
  - Delivery receipts
  - Multi-SIM support detection

- [ ] Add default messaging app handler
  - Check and request default SMS app status
  - Handle role manager APIs (Android 10+)

### UI Foundation - Conversation List
- [ ] Create navigation structure
  - Bottom navigation (if needed)
  - NavHost with Compose Navigation
  - Deep linking support

- [ ] Design and implement ConversationListScreen
  - LazyColumn with conversation items
  - Pull-to-refresh functionality
  - Empty state with illustration
  - Skeleton loading state

- [ ] Create beautiful conversation card UI
  - Contact avatar with gradient rings for unread
  - Message preview with proper truncation
  - Timestamp formatting
  - Unread badge
  - Pinned indicator
  - Archived state

- [ ] Implement swipe actions
  - Swipe to archive with haptic feedback
  - Swipe to delete with confirmation
  - Pin/unpin action
  - Smooth animations

- [ ] Add ConversationListViewModel
  - Load threads from database
  - Handle search/filter
  - Manage selection state
  - Update thread status (read/unread/archived/pinned)

### UI - Chat Thread Screen
- [ ] Design and implement ChatThreadScreen
  - LazyColumn with reverse layout for messages
  - Message grouping by date
  - Scroll to bottom FAB
  - Typing indicator placeholder

- [ ] Create message bubble UI components
  - Incoming message bubble (left-aligned)
  - Outgoing message bubble (right-aligned)
  - Timestamp display (fade in on scroll)
  - Delivery status indicators
  - Color-coded by sender

- [ ] Implement media message support
  - Image display with thumbnails
  - Video thumbnails with play button
  - Tap to open full-screen viewer
  - Shared element transitions

- [ ] Add message composition UI
  - Text input field with Material You styling
  - Send button with morph animation
  - Attachment button
  - Character counter for SMS

- [ ] Create ChatThreadViewModel
  - Load messages for thread
  - Send message action
  - Mark messages as read
  - Handle media attachments
  - Pagination for long threads

### Polish & Micro-interactions
- [ ] Add haptic feedback throughout
  - Button presses
  - Swipe actions
  - Message send
  - Long-press actions

- [ ] Implement smooth page transitions
  - Conversation list → Chat thread
  - Shared element transitions for avatars
  - Spring animations for screens

- [ ] Create empty state illustrations
  - No conversations state
  - No messages in search
  - Use vector graphics

- [ ] Add ripple effects to all tappable elements
  - Proper bounded/unbounded ripples
  - Material You ripple colors

### Settings Foundation
- [ ] Create SettingsScreen structure
  - Preference cards layout
  - Settings categories
  - Material You styling

- [ ] Add theme settings
  - Light/Dark/AMOLED selector
  - Dynamic color toggle
  - Custom accent color picker

- [ ] Implement app permissions screen
  - SMS permissions status
  - Request permissions flow
  - Default app status

### Testing & Verification
- [ ] Write unit tests for repositories
  - Message CRUD operations
  - Thread updates
  - Contact queries

- [ ] Write unit tests for ViewModels
  - Conversation list logic
  - Chat thread logic
  - Message sending

- [ ] Create UI tests for core flows
  - Send message flow
  - View conversation flow
  - Swipe actions

- [ ] Manual testing on real device
  - Send/receive SMS
  - MMS with images
  - Default app switching
  - Theme switching

---

## Phase 2 — Vector Store + Search UI Excellence 🔍

### Embedding System (TF-IDF)
- [ ] Implement TextEmbeddingService
  - TF-IDF algorithm (384 dimensions)
  - Tokenization and stop word filtering
  - Word hashing
  - Vector normalization
  - Cosine similarity calculation
  - Corpus management for IDF scores

- [ ] Add embedding storage to database
  - Update Message entity with embedding field
  - Serialization: embedding to comma-separated string
  - Deserialization: string to FloatArray
  - Migration script for existing messages

- [ ] Create embedding generation pipeline
  - WorkManager background job
  - Batch processing (avoid overload)
  - Progress tracking
  - Handle new incoming messages
  - Re-index on corpus update

### Semantic Search
- [ ] Implement MessageRetrievalService
  - Query embedding generation
  - Batched similarity search (50 chunks per batch)
  - Similarity threshold filtering (0.15 default)
  - Result sorting by relevance
  - Context building for RAG

- [ ] Add search functionality to repositories
  - Search by semantic similarity
  - Filter by thread, sender, date range
  - Combine with keyword search option

### Search UI
- [ ] Create SearchScreen with beautiful UI
  - Floating search FAB on conversation list
  - Morphing animation to full-screen overlay
  - Blur background effect
  - Material You styling

- [ ] Implement live search
  - Search as you type
  - Debouncing for performance
  - Loading states with shimmer
  - Results update smoothly

- [ ] Design search result cards
  - Message preview with highlight
  - Relevance score badge (color-coded)
  - Sender and timestamp
  - Tap to navigate to message in thread

- [ ] Add search history and suggestions
  - Recent searches as chips
  - Quick filter chips
  - Clear history option

- [ ] Create SearchViewModel
  - Manage search query state
  - Perform semantic search
  - Handle filters
  - Track search history

### Index Management UI
- [ ] Add indexing status to settings
  - Show total messages indexed
  - Indexing progress indicator
  - Last indexed timestamp
  - Force re-index button

- [ ] Create background sync indicator
  - Subtle notification when indexing
  - Don't interrupt user
  - Battery-aware scheduling

### Testing
- [ ] Unit tests for TextEmbeddingService
  - Embedding generation consistency
  - Cosine similarity accuracy
  - Edge cases (empty text, special chars)

- [ ] Unit tests for MessageRetrievalService
  - Search relevance
  - Batching correctness
  - Threshold filtering

- [ ] Integration tests for search flow
  - End-to-end search
  - Performance with large dataset
  - Accuracy validation

---

## Phase 3 — MCP Server + Developer Experience 🛠️

### MCP Server Core
- [ ] Implement BuiltInMcpServer class
  - JSON-RPC 2.0 protocol handling
  - Tool registration system
  - Request routing
  - Response formatting
  - Error handling

- [ ] Create MCP data models
  - McpRequest
  - McpResponse
  - Tool interface
  - ToolParameter
  - ToolResult

### MCP Tools Implementation
- [ ] Implement search_messages tool
  - MessageSearchTool class
  - Parameters: query, max_results
  - Execute semantic search
  - Format results with metadata

- [ ] Implement list_messages tool
  - MessageListTool class
  - Parameters: thread_id, limit
  - Query messages from thread
  - Format chronologically

- [ ] Implement send_message tool
  - SendMessageTool class
  - Parameters: phone_number, text
  - Validate phone number
  - Send SMS via SmsManager
  - Return delivery status

- [ ] Implement list_threads tool
  - ThreadListTool class
  - Parameters: limit
  - Return thread list with metadata

- [ ] Implement get_thread_summary tool
  - ThreadSummaryTool class
  - Parameters: thread_id
  - Generate summary (placeholder for now)
  - Return formatted summary

### MCP Server Integration
- [ ] Create MCP service layer
  - Start/stop server
  - Handle tool calls
  - Logging and debugging

- [ ] Add MCP to dependency injection
  - Singleton scope
  - Inject required services
  - Lifecycle management

### Developer UI
- [ ] Create developer/debug settings screen
  - MCP server status card
  - Connected clients indicator
  - Recent tool calls log
  - Test tool execution

- [ ] Add MCP configuration settings
  - Enable/disable MCP server
  - Port configuration (if external)
  - Security settings

### Optional: External MCP Access
- [ ] (Optional) Add HTTP/WebSocket wrapper
  - Local-only server (127.0.0.1)
  - Random port selection
  - Route to built-in server
  - Connection security

### Testing
- [ ] Unit tests for each MCP tool
  - search_messages accuracy
  - list_messages pagination
  - send_message validation
  - Error handling

- [ ] Integration tests for MCP server
  - JSON-RPC request/response cycle
  - Multiple tool calls
  - Error scenarios

---

## Phase 4 — AI Features + Delightful Interactions 🤖

### Thread Summaries
- [ ] Implement thread summarization
  - Load all messages from thread
  - Format for AI consumption
  - Call local LLM (placeholder)
  - Display summary in beautiful card

- [ ] Create summary UI component
  - Expandable summary card in thread
  - Shimmer loading effect
  - Syntax highlighting for structured data
  - Share summary button

### Smart Categories
- [ ] Implement automatic thread categorization
  - Category detection logic
  - Personal, Work, Promotions, etc.
  - ML-based classification (simple rules for MVP)

- [ ] Add category UI to conversation list
  - Collapsible category sections
  - Smooth expand/collapse animations
  - Category badges

### AI Assistant Interface
- [ ] Create AI assistant floating bubble
  - Material 3 extended FAB pattern
  - Follows scroll behavior
  - Smooth spring animation

- [ ] Implement assistant bottom sheet
  - Expands from FAB
  - Chat interface for "Ask your messages"
  - Input field and send button
  - Result cards with rich formatting

- [ ] Create AI assistant ViewModel
  - Manage conversation state
  - Call MCP tools internally
  - Format responses

### Insights Dashboard
- [ ] Design insights screen
  - Message statistics
  - Top contacts
  - Activity heatmap
  - Data visualizations

- [ ] Implement data aggregation
  - Message count over time
  - Response time analysis
  - Conversation metrics

---

## Phase 5 — Advanced Features + Polish ✨

### Encrypted Backup
- [ ] Implement local backup system
  - Export database to encrypted file
  - Progress animation during backup
  - Backup file management

- [ ] Implement restore functionality
  - Import from backup file
  - Decrypt and verify
  - Progress indicators

### Automations (Rules Engine)
- [ ] Create automation system
  - Rule definition structure
  - Trigger conditions (sender, keyword, time)
  - Actions (auto-reply, category, notification)

- [ ] Build visual rule builder UI
  - Drag-and-drop rule creation
  - Condition builder
  - Action selector
  - Test rule feature

### AI-Enhanced Contact Profiles
- [ ] Implement rich contact cards
  - Conversation history summary
  - Relationship insights
  - Important messages
  - Contact notes

- [ ] Add contact profile UI
  - Beautiful card layout
  - Edit contact info
  - View conversation stats

### Final Polish Pass
- [ ] Animation tuning
  - Review all animations for smoothness
  - Ensure 60fps minimum
  - Spring physics for natural motion

- [ ] Performance optimization
  - Profile app with Android Profiler
  - Optimize database queries
  - Reduce memory allocations
  - LazyColumn optimization

- [ ] Accessibility audit
  - TalkBack navigation testing
  - Content descriptions for all images
  - Semantic labels for controls
  - High contrast mode testing
  - Dynamic type support

- [ ] Edge case handling
  - Very long messages
  - Large group chats
  - Network failures
  - Storage full scenarios

---

## Phase 6 — Neural Embedding Upgrade (Future) 🧠

### Model Infrastructure
- [ ] Create model management system
  - Model catalog with metadata
  - Download manager with resume support
  - Checksum verification
  - Model storage in app-private directory

### Dual Embedding System
- [ ] Implement model abstraction layer
  - EmbeddingProvider interface
  - TfidfEmbeddingProvider
  - NeuralEmbeddingProvider (all-MiniLM-L6-v2)
  - Model selection logic

### Migration & Re-indexing
- [ ] Create migration system
  - Background re-indexing WorkManager job
  - Progressive re-indexing (batch by batch)
  - Dual index during transition
  - Completion tracking

### Neural Model Settings UI
- [ ] Add model management screen
  - Available models list
  - Download models
  - Model size and info
  - Active model selection
  - GPU acceleration toggle

---

## Documentation & Infrastructure

### Documentation
- [ ] Create comprehensive README.md
  - Project overview
  - Features list
  - Setup instructions
  - Screenshots

- [ ] Write ARCHITECTURE.md
  - Module structure
  - Design patterns used
  - Data flow diagrams
  - Key decisions

- [ ] Document API/MCP interface
  - Tool descriptions
  - Parameter specifications
  - Example requests/responses

### Code Quality
- [ ] Set up lint rules
  - Kotlin style guide
  - Custom lint checks
  - Fix all warnings

- [ ] Add CI/CD pipeline (optional)
  - GitHub Actions workflow
  - Automated testing
  - Build verification

### Privacy & Security
- [ ] Implement encrypted storage
  - SQLCipher for database
  - Encrypted SharedPreferences
  - Key management

- [ ] Add app lock feature
  - Biometric authentication
  - Passcode option
  - Auto-lock timeout

---

## Future Enhancements & Feature Ideas 🚀

### Contact Integration
- [ ] Implement contact name resolution from Android Contacts Provider
  - Query ContactsContract for phone numbers
  - Display contact names instead of phone numbers in conversation list
  - Show contact photos as avatars
  - Auto-update contact names when changed
  - Handle multiple phone numbers for same contact
  - Fallback to phone number when contact not found

### Conversation Organization
- [ ] Add folder/category system for organizing conversations
  - Create custom folders (Work, Family, Friends, Projects, etc.)
  - Assign conversations to folders
  - Multi-select conversations for batch folder assignment
  - Folder-based filtering in conversation list
  - Color-coded folders with custom icons
  - Nested folder support
  - Quick folder switcher in navigation drawer
  - Conversation can belong to multiple folders (tags system)

- [ ] Implement smart folder suggestions
  - Auto-suggest folders based on contact groups
  - ML-based categorization (Personal, Work, Promotions)
  - Frequency-based folder recommendations
  - Time-based folders (Recent, This Week, This Month)

### Advanced Search Features
- [ ] Implement full-text search with highlighting
  - Search message content, contact names, phone numbers
  - Highlight search terms in results
  - Search filters (date range, sender, media type)
  - Search history with autocomplete
  - Save search queries as smart folders
  - Regular expression support for power users

- [ ] Add search operators
  - `from:contact` - Messages from specific contact
  - `to:contact` - Messages to specific contact
  - `has:media` - Messages with attachments
  - `before:date` - Messages before date
  - `after:date` - Messages after date
  - `in:folder` - Messages in specific folder

### Enhanced Contact Features
- [ ] Rich contact profiles
  - View full conversation history
  - Important messages bookmarked
  - Contact notes and reminders
  - Relationship insights (first message date, frequency)
  - Contact birthday tracking
  - Last contacted indicator
  - Contact-specific notification settings
  - Quick actions (call, video call, email)

### Message Organization
- [ ] Implement message bookmarking/starring
  - Star important messages
  - Starred messages view
  - Search within starred messages
  - Export starred messages

- [ ] Add message labels/tags
  - Custom labels for messages
  - Color-coded labels
  - Label-based filtering
  - Quick label selector

### Productivity Features
- [ ] Schedule messages for later sending
  - Date/time picker for scheduled send
  - View scheduled messages
  - Edit/cancel scheduled messages
  - Timezone awareness

- [ ] Message templates/quick replies
  - Save frequently used messages
  - Template categories
  - Variables in templates (name, date, time)
  - Quick access from compose screen

- [ ] Draft messages
  - Auto-save drafts while typing
  - Draft indicator in conversation list
  - Resume drafts when reopening conversation
  - Draft sync across devices (future)

### Media & Attachments
- [ ] Enhanced media viewer
  - Full-screen image viewer with zoom
  - Video player with controls
  - Media gallery view (all images from conversation)
  - Save media to device
  - Share media to other apps
  - Edit images before sending

- [ ] Rich media support
  - GIF support
  - Audio message recording
  - Voice note playback
  - Document attachments (PDF, etc.)
  - Location sharing
  - Contact card sharing

### Import/Export & Backup
- [ ] Advanced backup features
  - Automatic backup scheduling
  - Cloud backup options (Google Drive, Dropbox)
  - Incremental backups
  - Selective backup (specific conversations/folders)
  - Backup encryption with password

- [ ] Import from other messaging apps
  - Import from Google Messages
  - Import from Signal backup
  - Import from WhatsApp backup
  - CSV import for bulk messages

- [ ] Export conversations
  - Export as PDF with formatting
  - Export as HTML
  - Export as plain text
  - Export specific date ranges
  - Include/exclude media

### Themes & Customization
- [ ] Advanced theme customization
  - Custom color palettes
  - Per-conversation themes
  - Bubble shape customization
  - Font size settings
  - Compact/comfortable/spacious density options
  - Chat wallpapers
  - Animated backgrounds

### Notification Enhancements
- [ ] Smart notifications
  - Contact-specific notification sounds
  - Folder-specific notification settings
  - Quiet hours with scheduling
  - Priority notifications for important contacts
  - Smart notification grouping
  - Quick reply from notification
  - Snooze notifications

### Privacy & Security
- [ ] Enhanced privacy features
  - Hide message content in notifications
  - App lock with PIN/biometric
  - Hide specific conversations
  - Private conversations with separate unlock
  - Screenshot prevention (optional)
  - Self-destructing messages (local only)

### Performance & Technical
- [ ] Performance optimizations
  - Lazy loading for large conversations
  - Image caching and optimization
  - Database query optimization
  - Memory usage reduction
  - Battery optimization
  - Storage management (auto-cleanup old media)

### Accessibility
- [ ] Accessibility improvements
  - Screen reader optimization
  - Voice control integration
  - High contrast themes
  - Font scaling support
  - Keyboard navigation
  - Reduced motion option

### Developer Features
- [ ] MCP server enhancements
  - Add more tool capabilities
  - Webhook support for real-time updates
  - API documentation
  - Example client implementations
  - Rate limiting and authentication
  - Batch operations support

---

*Legend: ✨ = UI/UX heavy, 🔍 = Search/AI, 🛠️ = Developer tools, 🤖 = AI features, 🧠 = Advanced ML, 🚀 = Future enhancements*
