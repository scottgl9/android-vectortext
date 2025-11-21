.PHONY: debug release bundle-debug bundle-release bundle clean install test help

# Default target
.DEFAULT_GOAL := help

# APK Build targets
debug: ## Build debug APK
	@echo "Building debug APK..."
	./gradlew assembleDebug

release: ## Build release APK
	@echo "Building release APK..."
	./gradlew assembleRelease

# AAB (Android App Bundle) Build targets
bundle-debug: ## Build debug AAB (Android App Bundle)
	@echo "Building debug AAB..."
	./gradlew bundleDebug
	@echo "AAB file: app/build/outputs/bundle/debug/app-debug.aab"

bundle-release: ## Build release AAB for Google Play Store
	@echo "Building release AAB..."
	./gradlew bundleRelease
	@echo "AAB file: app/build/outputs/bundle/release/app-release.aab"

bundle: bundle-release ## Build release AAB (alias for bundle-release)

# Clean target
clean: ## Clean build artifacts
	@echo "Cleaning build..."
	./gradlew clean

# Test target
test: ## Run unit tests
	@echo "Running unit tests..."
	./gradlew testDebugUnitTest

# Install targets
install: install-release ## Install APK to device (defaults to release)

install-debug: debug ## Install debug APK to device
	@echo "Installing debug APK to device..."
	adb install -r app/build/outputs/apk/debug/app-debug.apk

install-release: release ## Install release APK to device
	@echo "Installing release APK to device..."
	adb install -r app/build/outputs/apk/release/app-release.apk

# Help target
help: ## Show this help message
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
