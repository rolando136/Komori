# Komori (木漏れ日)

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="128" height="128" alt="Komori Logo" />
</p>

<p align="center">
  <em>Sunlight filtering through trees. A calm, distraction-free companion to organize your day.</em>
</p>

<p align="center">
  <a href="https://github.com/rolando136/Komori/releases"><img src="https://img.shields.io/badge/Release-APK%20Available-8FA886?style=for-the-badge" alt="Release APK" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2024%2B)-7CA6B4?style=for-the-badge&logo=android&logoColor=white" alt="Android Version" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Kotlin-2.0.21-9D847B?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-D499AA?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
</p>

---

## 🌿 About Komori

**Komori** is an aesthetic, all-in-one personal organizer built natively for Android using **Jetpack Compose** and **Material 3**. Inspired by the Japanese word *Komorebi* (木漏れ日)—the play of sunlight through foliage—Komori brings serenity, focus, and clarity to daily planning.

Instead of busy dashboards and cluttered tools, Komori unifies your **Schedule**, **Tasks**, **Habits**, and **Notes** into an elegant swipeable experience with soft pastel color palettes, smooth animations, and clean typography.

---

## ✨ Features

### 📅 1. Calendar & Weekly Timetable
- **Interactive Weekly Schedule**: Visual time grid with hour-by-hour view.
- **Smart Recurrence**: Support for one-off and repeating weekly schedules (e.g. classes, routines, appointments).
- **Conflict Handling**: Intelligent dialog displaying overlapping events without clutter.
- **Month & Year Picker**: Quick date navigation with animated calendar transitions.

### ✅ 2. Focus (Tasks & Subtasks)
- **Nested Subtasks**: Break complex goals down into bite-sized actionable items with real-time completion progress tracking.
- **Priorities & Due Dates**: Color-coded priorities (High, Medium, Low) with date & time alarms.
- **Interactive Detail Modal**: Inspect, edit, check off subtasks, or delete in a clean floating surface.
- **Active & Completed Separation**: Clear distinction between active tasks and checked-off accomplishments.

### 🌸 3. Rituals (Daily Habits)
- **Weekly Habit Board**: Track consistency across Monday through Sunday with minimal rounded checkboxes.
- **Streak Tracker**: Automatic calculation of current streaks with forgiving day-prior rollover.
- **Aesthetic Phosphor Icons**: Choose from 16 curated icons (Lotus, Book, Water Drop, Sun, Moon, Coffee, Heart, etc.).
- **Pre-seeded Habits**: Comes ready with *Morning Meditation*, *Hydration*, and *Daily Reading*.

### 📝 4. Notes & Thoughts
- **Instant Search**: Real-time filtering across titles and note contents.
- **Pin to Top**: Keep important reflections or pinned lists readily accessible.
- **Rich Card View**: Clean typography with timestamps and automatic line clamping.
- **Full CRUD Support**: Effortlessly add, update, and manage your notes.

### 🎨 5. Calming Aesthetic Themes
Komori includes 5 hand-crafted pastel palettes tailored for visual comfort in both **Light** and **Dark** modes:

| Theme | Inspiration | Light Accent | Dark Accent |
| :--- | :--- | :--- | :--- |
| **Komori** (Default) | Forest & Sage leaves | `#8FA886` | Deep Forest Black |
| **Coffee** | Warm latte & old paper | `#9D847B` | Espresso Black |
| **Ocean** | Misty blue seas | `#7CA6B4` | Abyss Navy |
| **Sakura** | Gentle dusty rose petals | `#D499AA` | Midnight Rose |
| **Lavender** | Dreamy twilight wisteria | `#A699B5` | Void Purple |

---

## 📱 Screenshots & Visual Design

> Komori emphasizes minimal borders, soft tonal surfaces, Google Sans typography, and rounded cards (20–28dp) to provide a tactile, calming experience.

```
┌────────────────────────────────────────────────────────┐
│                      MY FOCUS                          │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ◯  Complete Project Roadmap           [High]     │  │
│  │    Subtasks: 2/3 completed (67%)                 │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ◯  Read 20 pages of Book              [Low]      │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│        [ 📅 Schedule ] [ ✅ Focus ] [ 🌿 Rituals ] [ 📝 Notes ]        │
└────────────────────────────────────────────────────────┘
```

---

## 🛠️ Architecture & Tech Stack

Komori is built following modern Android Architecture best practices (Unidirectional Data Flow + MVVM):

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3.
- **State & Lifecycle**: Kotlin `StateFlow`, `collectAsStateWithLifecycle`, Coroutines.
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) v2.6+ with custom `TypeConverter`s for JSON and date handling.
- **Icons**: [Phosphor Icons Android](https://github.com/adamglin0/phosphor-icon-android) (Bold weight).
- **Navigation**: Jetpack Navigation Compose with Edge-to-Edge display support.
- **Typography**: Google Sans typeface family.

---

## 🚀 Download & Installation

You can download the pre-compiled APK directly from the repository:

1. Download **[`app/release/app-release.apk`](app/release/app-release.apk)** from this repository or the [Releases](https://github.com/rolando136/Komori/releases) tab.
2. Transfer or open the `.apk` file on your Android device (Android 8.0+ / API 24 or newer).
3. If prompted, enable **Install Unknown Apps** for your browser or file manager.
4. Open **Komori** and begin your mindful daily routine.

---

## 💻 Building from Source

To build and run Komori locally using Android Studio:

### Prerequisites
- **Android Studio** (Ladybug / Iguana or newer recommended)
- **JDK 17+** (JDK 21 tested and verified)
- **Android SDK** API 36 (`compileSdk 36`, `minSdk 24`)

### Steps
```bash
# 1. Clone the repository
git clone https://github.com/rolando136/Komori.git
cd Komori

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Run Unit Tests
./gradlew testDebugUnitTest

# 4. Build Signed/Release APK
./gradlew assembleRelease
```
The generated APKs will be located in `app/build/outputs/apk/`.

---

## 🧪 Testing

The codebase includes local unit test coverage for core converters and habit streak calculation algorithms:

```bash
./gradlew testDebugUnitTest
```
- `ConvertersTest`: Validates Room JSON serialization, Priority string mappings, and `Set<String>`/`Set<Int>` converters.
- `HabitStreakTest`: Validates habit streak counts, rollover edge cases, and weekly check states.

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).

---

<p align="center">
  Crafted with care by <a href="https://github.com/rolando136">Rolando</a> 🌿
</p>
