# Vertext - AI-Powered Android Messaging App

An intelligent, feature-rich SMS/MMS messaging application for Android with semantic search, AI assistance, and beautiful Material You design.

## ✨ Key Features

### 💬 Core Messaging
- **Full SMS/MMS Support** - Send and receive text messages and multimedia content
- **Rich Media Attachments** - Images, videos, and audio with full playback support
- **MMS Subject Lines** - Proper display and formatting of MMS subjects
- **Group Conversations** - Color-coded sender bubbles for easy identification
- **Read Receipts** - Track message delivery and read status

> **Note**: Message reactions feature is currently disabled due to reliability issues with cross-device compatibility. All text messages are now displayed without filtering.

### 🎨 Media Playback
- **Image Viewer** - Full-screen viewing with pinch-to-zoom (1x-5x) and pan gestures
- **Video Player** - Native ExoPlayer controls with seek bar and playback controls
- **Audio Player** - ExoPlayer-based playback with progress tracking and duration display
- **Smart Loading** - Async media loading with Coil, loading states, and error handling
- **All Formats** - Support for JPEG, PNG, GIF, WebP, MP4, MP3, AAC, and more

### 🔍 AI-Powered Search
- **Semantic Search** - Find messages by meaning, not just keywords
- **Vector Embeddings** - TF-IDF-based message indexing (384 dimensions)
- **Relevance Scoring** - Results ranked by semantic similarity
- **Smart Suggestions** - Search history and quick filters

### 🤖 AI Assistant
- **Natural Language Queries** - Ask questions about your messages in plain English
- **MCP Integration** - Model Context Protocol for AI tool calling
- **Smart Summaries** - Thread summaries with statistics and insights
- **Auto-Categorization** - Automatic conversation categorization (Personal, Work, etc.)

### 📊 Analytics & Insights
- **Message Statistics** - Total messages, sent/received counts, response times
- **Activity Charts** - Daily/weekly activity visualization
- **Top Contacts** - Most frequent conversation partners
- **Conversation Metrics** - Thread analytics and engagement data

### 🎨 Beautiful Design
- **Material You** - Dynamic theming with wallpaper-based colors
- **Light/Dark Modes** - System-adaptive themes with AMOLED black option
- **Smooth Animations** - Spring physics and shared element transitions
- **Haptic Feedback** - Tactile responses for interactions

### 🔒 Privacy & Security
- **Encrypted Backups** - AES256-GCM encryption for local backups
- **Blocked Contacts** - Block unwanted senders
- **Selective Backup** - Choose what to backup and restore

### 🛠️ Automation
- **Rules Engine** - Create automation rules with triggers, conditions, and actions
- **Auto-Reply** - Automatic responses based on sender, keywords, or time
- **Smart Organization** - Auto-archive, categorize, or mark messages as read
- **Time-Based Rules** - Schedule actions for specific days/times

## 🏗️ Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room with SQLite
- **Dependency Injection**: Hilt
- **Media Playback**: ExoPlayer (Media3 v1.5.0)
- **Image Loading**: Coil 3
- **Background Processing**: WorkManager
- **AI/Search**: Custom TF-IDF vector embeddings
- **MCP Server**: Built-in Model Context Protocol server

## 📱 Requirements

- **Android 9.0+** (API 28+)
- **Target SDK**: 35
- **Permissions**: SMS, Contacts, Storage (for media)
- **Default SMS App**: Must be set as default messaging app

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/android-vectortext.git
   ```

2. Open in Android Studio (Hedgehog or later)

3. Sync Gradle dependencies

4. Run on device or emulator:
   ```bash
   ./gradlew installDebug
   ```

5. Grant required permissions and set as default SMS app

## 📚 Documentation

- **[PROGRESS.md](PROGRESS.md)** - Development progress and implementation details
- **[TODO.md](TODO.md)** - Roadmap and upcoming features
- **[CLAUDE.md](CLAUDE.md)** - Development guide for Claude AI assistant
- **[PRD.md](PRD.md)** - Product Requirements Document

## 🎯 Recent Updates

### MMS Media Rendering (2025-11-20)
- ✅ Complete media attachment support for images, videos, and audio
- ✅ Full-screen image viewer with zoom and pan gestures
- ✅ Video player with ExoPlayer native controls
- ✅ Audio playback with real-time progress tracking
- ✅ MMS subject display and proper formatting
- ✅ All media formats supported with graceful fallbacks

### Message Reactions (2025-11-20) - **DISABLED**
- ⚠️ **Currently Disabled** - Reaction feature disabled due to reliability issues
- ⚠️ All text messages now display without filtering (including reaction-format texts)
- ⚠️ May be re-enabled in future with improved implementation

### Performance Optimization (2025-11-20)
- ✅ Configurable message load limit (10-10,000 messages)
- ✅ Lazy loading for better performance
- ✅ User-adjustable settings for device capabilities

## 🤝 Contributing

This is a personal project, but suggestions and feedback are welcome! Please open an issue to discuss potential changes.

## 📄 License

[Add your license here]

## 🙏 Acknowledgments

- Built with ❤️ using Jetpack Compose and Material You
- ExoPlayer for professional media playback
- Coil for efficient image loading
- Anthropic's Claude for development assistance
