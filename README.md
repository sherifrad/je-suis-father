# Je Suis Father 🛡️👨‍👧‍👦

**Je Suis Father** is an open-source, privacy-first native Android parental control application designed to provide parents with mindful, transparent, and robust oversight while protecting children in digital spaces.

---

## 📌 Project Overview

- **Application Name:** Je Suis Father
- **Platform:** Native Android (Kotlin & Jetpack Compose)
- **License:** Open Source ([Apache License 2.0](LICENSE))
- **Status:** Initial Foundation & Architecture Preparation

---

## 🏗️ Architecture & Tech Stack

- **Language:** Kotlin (JVM 17+)
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Navigation:** Modern Jetpack Navigation 3 (`androidx.navigation3`)
- **Build System:** Gradle (Kotlin DSL, Version Catalogs `gradle/libs.versions.toml`)
- **Architecture Pattern:** Clean Architecture / MVI (Model-View-Intent) & Unidirectional Data Flow (UDF)
- **Target SDK:** Android 37 (Min SDK 24+)

---

## 📂 Repository Structure

```text
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/example/jesuisfather/
│   │   │   │   ├── data/             # Repositories & Data sources
│   │   │   │   ├── ui/               # Compose Screens, ViewModels, Themes
│   │   │   │   ├── MainActivity.kt   # App Entry Point
│   │   │   │   └── Navigation.kt     # Jetpack Navigation 3 Routing
│   │   │   └── res/                  # Resources & Strings
│   │   ├── test/                     # Unit Tests
│   │   └── androidTest/              # Instrumented UI Tests
│   └── build.gradle.kts
├── gradle/                           # Gradle Wrapper & Version Catalog
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE                           # Apache 2.0 Open Source License
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or later (or Android SDK installed locally)
- JDK 17+
- Android Device or Emulator (API 24+)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/sherifrad/je-suis-father.git
cd je-suis-father

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
