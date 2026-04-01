# HikeTracker — Android Studio Setup Guide

## Project Overview

A GPS hiking tracker app with:
- **Firebase Authentication** (email & password login/signup)
- **Google Maps** with live route drawing (polyline)
- **Foreground GPS Service** that survives screen-off
- **Room Database** for local hike storage
- **History screen** listing all past hikes

---

## Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| Android SDK | API 34 (compileSdk) |
| Min Device | API 24 (Android 7.0+) |
| Java | 1.8 (set in build.gradle) |

---

## Step 1 — Import the Project

1. Open **Android Studio**
2. Choose **File → Open**
3. Navigate to the `HikeTracker/` folder and click **OK**
4. Wait for Gradle sync to complete (first sync downloads ~200 MB of dependencies)

---

## Step 2 — Set Up Firebase

### 2a. Create a Firebase Project

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add project** → name it `HikeTracker`
3. Disable Google Analytics (optional) → **Create project**

### 2b. Add an Android App

1. In your Firebase project, click **Add app → Android**
2. Enter package name: `com.hiketracker`
3. Click **Register app**
4. Download `google-services.json`
5. Place `google-services.json` inside:
   ```
   HikeTracker/app/google-services.json
   ```

### 2c. Enable Email/Password Authentication

1. In Firebase Console → **Authentication → Sign-in method**
2. Enable **Email/Password**
3. Click **Save**

---

## Step 3 — Set Up Google Maps API Key

### 3a. Enable the Maps SDK

1. Go to [https://console.cloud.google.com](https://console.cloud.google.com)
2. Select (or create) the same project linked to Firebase
3. Navigate to **APIs & Services → Library**
4. Search for **Maps SDK for Android** → **Enable**

### 3b. Create an API Key

1. Go to **APIs & Services → Credentials**
2. Click **Create Credentials → API key**
3. (Recommended) Restrict to **Android apps** using your package name + SHA-1

### 3c. Add the Key to the App

Open `app/src/main/res/values/strings.xml` and replace:

```xml
<string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
```

with your actual key:

```xml
<string name="google_maps_key">AIzaSy...your_key_here</string>
```

---

## Step 4 — Build & Run

1. Connect a **physical Android device** (GPS tracking works best on real hardware)
   - Or use an emulator with **Extended Controls → Location** for simulated GPS
2. Click **Run ▶** in Android Studio
3. Select your device → app installs and launches

---

## Project File Structure

```
HikeTracker/
├── app/
│   ├── google-services.json          ← YOU ADD THIS (Firebase)
│   ├── build.gradle                  ← All dependencies
│   └── src/main/
│       ├── AndroidManifest.xml       ← Permissions + service declaration
│       ├── java/com/hiketracker/
│       │   ├── MainActivity.java     ← Bottom nav host + logout
│       │   ├── model/
│       │   │   └── Hike.java         ← Room entity (data model)
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── HikeDao.java       ← Room queries
│       │   │   │   └── HikeDatabase.java  ← Room DB singleton
│       │   │   └── repository/
│       │   │       └── HikeRepository.java ← Data access layer
│       │   └── ui/
│       │       ├── auth/
│       │       │   ├── LoginActivity.java
│       │       │   └── RegisterActivity.java
│       │       ├── hike/
│       │       │   ├── HikeFragment.java         ← Map + tracking UI
│       │       │   └── LocationTrackingService.java ← Foreground GPS service
│       │       └── history/
│       │           ├── HistoryFragment.java
│       │           └── HikeAdapter.java
│       └── res/
│           ├── layout/               ← All XML layouts
│           ├── navigation/nav_graph.xml
│           ├── menu/                 ← Bottom nav + toolbar menus
│           ├── drawable/             ← Vector icons
│           ├── color/nav_item_color.xml
│           └── values/
│               ├── strings.xml       ← ← ADD YOUR MAPS KEY HERE
│               ├── colors.xml
│               └── themes.xml
└── build.gradle                      ← Root build file
```

---

## How the App Works

### Authentication Flow
```
App Launch
    │
    ▼
MainActivity checks FirebaseAuth.getCurrentUser()
    ├── null  → LoginActivity
    │              ├── Login (email + password)
    │              └── Register → LoginActivity
    └── user  → Main screen (bottom nav)
```

### Tracking Flow
```
[Start Hike] button pressed
    │
    ├── Checks ACCESS_FINE_LOCATION permission
    │     └── If denied → requests permission dialog
    │
    ├── Starts LocationTrackingService (foreground)
    │     ├── Shows persistent notification
    │     └── Requests GPS updates every 3 seconds
    │
    ├── Binds to service → receives location callbacks
    │     ├── Draws green polyline on map
    │     ├── Accumulates distance (filters GPS noise)
    │     └── Updates timer every second
    │
[Stop & Save] button pressed
    ├── Unbinds + stops service
    ├── Saves Hike to Room database (background thread)
    └── Resets UI
```

### Data Storage
- **Room (SQLite)** stores hikes locally on-device
- Each hike row contains: userId, date, startTime, endTime, distanceMeters, durationSeconds
- History screen observes LiveData — updates automatically when new hikes are saved

---

## Permissions Explained

| Permission | Why |
|-----------|-----|
| `ACCESS_FINE_LOCATION` | GPS tracking |
| `ACCESS_COARSE_LOCATION` | Fallback location |
| `FOREGROUND_SERVICE` | Background tracking service |
| `FOREGROUND_SERVICE_LOCATION` | Required on Android 14+ |
| `POST_NOTIFICATIONS` | Foreground service notification (Android 13+) |
| `INTERNET` | Firebase Auth + Maps tile loading |

---

## Common Issues & Fixes

### Map not showing
- Check that `google_maps_key` in `strings.xml` is correct
- Ensure **Maps SDK for Android** is enabled in Google Cloud Console
- Check Logcat for `"Authorization failure"` errors

### GPS not updating on emulator
- Open **Extended Controls (⋮) → Location**
- Set a latitude/longitude and click **Send**
- Or use **GPX playback** to simulate a route

### Firebase login fails
- Verify `google-services.json` is in the `app/` folder (not root)
- Confirm Email/Password is enabled in Firebase Console
- Re-sync Gradle after adding `google-services.json`

### "Missing foregroundServiceType" crash on Android 14
- Already handled: `AndroidManifest.xml` declares `android:foregroundServiceType="location"`

### Build fails: "Failed to resolve firebase-bom"
- Check internet connection and try **File → Sync Project with Gradle Files**

---

## Next Steps (Beyond MVP)

Here are features you can add next:

1. **Pace / Speed** — divide distance by time, show km/h
2. **Elevation** — use `location.getAltitude()`
3. **Route Map in History** — store waypoints as JSON, replay on a static map
4. **Firebase Firestore sync** — back up hikes to the cloud
5. **Hike name / notes** — let user label each hike
6. **Stats dashboard** — total hikes, total km, personal best

---

## Dependencies Summary

```gradle
// UI
com.google.android.material:material:1.11.0
androidx.constraintlayout:constraintlayout:2.1.4
androidx.recyclerview:recyclerview:1.3.2

// Navigation
androidx.navigation:navigation-fragment:2.7.6
androidx.navigation:navigation-ui:2.7.6

// Firebase
firebase-bom:32.7.0
firebase-auth
firebase-firestore

// Maps & Location
play-services-maps:18.2.0
play-services-location:21.1.0

// Room (local DB)
room-runtime:2.6.1
room-compiler:2.6.1  (annotationProcessor)

// Lifecycle
lifecycle-viewmodel:2.7.0
lifecycle-livedata:2.7.0
```
