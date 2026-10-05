# Spend Aware

Spend Aware is an Android expense-tracking app built to help users understand their spending, plan budgets, and keep an eye on day-to-day spending pace.

## Features

- Record expenses with an amount, category, date, and optional note.
- Review and manage transaction history.
- Create categories and organize spending by category.
- Set monthly, weekly, or yearly budgets, including fixed commitments.
- See budget burn-rate indicators such as amount spent, amount remaining, and projected spending.
- View spending analytics, including category totals and spending over time.
- Switch between the app's available profiles; profile data is stored separately.
- Get spending-anomaly checks when adding an expense. The app uses category history when enough data is available and a threshold-based check otherwise.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Navigation Compose and ViewModel
- Room for local database storage
- DataStore Preferences for onboarding preferences
- Gradle Kotlin DSL with a version catalog

## Requirements

- Android Studio with Android SDK Platform 37 installed
- JDK 25 (the Gradle daemon toolchain is configured for Java 25)
- Android device or emulator running Android 8.0 (API 26) or later

The Gradle wrapper is included in the repository. The first build may need an internet connection to download Gradle and Android dependencies.

## Build and run

1. Clone the repository and open the project root in Android Studio.
2. Allow Gradle to sync and install any requested Android SDK components.
3. Select the `app` run configuration and launch it on an emulator or connected device.

To build a debug APK from a terminal at the project root:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To run the local unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

The current test source set contains the default sample unit test; feature-specific test coverage has not yet been added.

## Project structure

```text
app/src/main/java/com/ayaansaksham/spend_aware/
├── data/         Room database, entities, DAOs, and repositories
├── domain/logic/ Budget pacing and spending-analysis calculations
├── ui/           Compose screens and app theme
├── viewmodel/    UI state and application logic
└── MainActivity.kt
```

## Data and privacy

Expense, budget, category, savings-goal, and profile settings are stored locally on the device using Room. The app also uses DataStore for the onboarding name. The project does not currently configure account authentication, cloud synchronization, or a remote API.

## License

No license is currently specified. All rights remain with the repository's authors unless a license is added.
