# 📍 MapUp - GPS Location Tracker

**MapUp** is a native Android application built with **Kotlin** and **Jetpack Compose** designed to track user location in the background, persist session data locally using Room, and visualize routes on Google Maps.
## ⚙️ Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Shouvick21/mapUp.git
   ```
   
## 📂 Project Structure

The project follows a **Feature-Based Clean Architecture** approach, organizing code by feature (`location`) and then by layer (`data`, `ui`), keeping core services separate.

```text
com.shouvick.mapup
├── core
│   └── service
│       └── LocationTracingService.kt  // Foreground Service for background tracking
├── feature
│   └── location
│       ├── data
│       │   ├── AppDatabase.kt         // Room Database Holder
│       │   ├── TrackingDao.kt         // Data Access Object
│       │   └── TrackingEntities.kt    // Session & Location Tables
│       ├── domain
│       │   └── (Domain models/repos)
│       └── ui
│           ├── utils/                 // Helper UI components
│           ├── MainScreen.kt          // Dashboard & Permission Handling
│           ├── MainViewModel.kt       // State Management
│           ├── Navigation.kt          // NavHost & Routing
│           └── SessionDetailScreen.kt // Map Visualization & Stats
├── LocationManager.kt                 // GPS Settings & FusedLocation Client
└── MainActivity.kt                    // Entry Point

```

## 🛠 Tech Stack & Libraries

This project leverages modern Android development tools and libraries:

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material3)
* **Architecture:** MVVM (Model-View-ViewModel)

### 📚 Key Libraries Used

| Library | Purpose |
| :--- | :--- |
| **Google Play Services Location** | `libs.google.play.service.location` - Used for `FusedLocationProviderClient` to fetch high-accuracy GPS updates. |
| **Room Database** | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP) - Local persistence for storing tracking sessions and coordinates. |
| **Maps Compose** | `com.google.maps.android:maps-compose` - Jetpack Compose wrapper for Google Maps to render Polylines and Markers. |
| **Google Maps SDK** | `com.google.android.gms:play-services-maps` - The core map rendering engine. |
| **Navigation Compose** | `androidx.navigation:navigation-compose` - Handles navigation between the Main Screen and Details Screen. |
| **ViewModel Compose** | `androidx.lifecycle:lifecycle-viewmodel-compose` - Manages UI state and survives configuration changes. |

## 📱 Screens Overview

The application consists of two primary screens managed by `Navigation.kt`:

### 1. MainScreen
* **Purpose:** The home dashboard where users initiate tracking.
* **Features:**
    * Requests Runtime Permissions (Location & Notification).
    * Checks and resolves GPS Settings (asks user to turn on High Accuracy).
    * Starts the Foreground Service.
    * Displays a list of past "Tracking Sessions" (History).

### 2. SessionDetailsScreen
* **Purpose:** Detailed view of a specific trip.
* **Features:**
    * **Google Map Integration:** Displays the full route path using a Red Polyline.
    * **Auto-Zoom:** Automatically adjusts the camera to fit the start and end points.
    * **Stats Panel:** Shows total **Distance** and **Duration** for the selected session.
    * **Scrollable List:** A bottom sheet (or list) view of all captured raw coordinates.

---

