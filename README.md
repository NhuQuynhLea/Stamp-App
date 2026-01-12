# Stamp - Health & Meal Tracker (KMP Hackathon)

**Stamp** is a Kotlin Multiplatform (KMP) application designed to help users track their health and meals. It leverages AI to infer nutritional information from food photos and provides personalized health advice based on user goals.

---

## **Prerequisites**

Before you begin, ensure you have the following installed:

*   **JDK 17** (Required for Android & Gradle)
*   **Android Studio** (Latest version recommended) with Kotlin Multiplatform Mobile plugin.
*   **Xcode** (Required for iOS target) - *macOS only*
*   **CocoaPods** (Required for iOS dependencies)

---

## **Getting Started**

### **Android**

1.  Open the project directory in **Android Studio**.
2.  Wait for the Gradle sync to complete.
3.  Select the **`androidApp`** (or `composeApp`) configuration from the run configurations dropdown.
4.  Choose a connected Android device or an emulator.
5.  Click the **Run** button (green play icon).

### **iOS** (macOS only)

1.  Open the `iosApp/iosApp.xcodeproj` file in **Xcode**.
2.  Ensure that the build target is set to **iosApp**.
3.  Select an iOS Simulator or a connected iOS device.
4.  Click the **Run** button (play icon) in Xcode.

> **Note:** The first build may take some time as it compiles the shared Kotlin code into an iOS framework.

---

## **How to Try Key Features**

Follow this flow to experience the core functionality of the app:

### **1. Set Up Your Profile**
*   Go to the **Profile** tab (User Info).
*   Enter your details (Age, Weight, Activity Level).
*   Select a **Primary Goal** (e.g., Weight Loss, Muscle Gain). This determines which metrics are tracked on the Home screen.
*   Click **Save**.

### **2. Log a Meal (The Logging Loop)**
*   Navigate to the **Journey** tab.
*   Look for a meal slot with a **`?`** icon (e.g., Lunch).
*   Tap the **`?`** to open the **Camera**.
*   Take a photo of your food.
*   The app will process the image (simulated AI inference) and show the **Food Detail Screen** with estimated nutritional info (Calories, Protein, etc.).
*   Review the AI's comment and go back. The `?` is now replaced by your meal photo.

### **3. Check Your Progress**
*   Go to the **Home** tab.
*   Observe how the **Health Metric Tiles** (e.g., Calories, Protein) have updated based on your logged meal.
*   Check the **Daily Trend Chart** to see your progress against your goal.
*   Read the **AI Coach** advice at the bottom for personalized feedback.

---

## **Detailed Documentation**

### **1. Detailed Screen Features**

#### **Screen A: Home (Health Dashboard)**

* **Health Metric Tiles:** Dynamic tiles that update based on the user's selected **Primary Goal**.
* **Weight:** Progress tracking over time (displayed as a separate card).

**Goal to Metric Mapping:**

| Primary Goal | Key Metrics Displayed |
| :--- | :--- |
| **Weight Loss / Maintain Weight** | Calories, Protein, Fiber |
| **Muscle Gain** | Protein, Calories, Meal Timing |
| **Improve Energy** | Carb Quality, Hydration, Water |
| **Blood Sugar Control** | Added Sugar, Fiber, Meal Timing |
| **Heart Health** | Sodium, Fat Quality (Sat. Fat), Fiber |
| **Gut Health / Other** | Calories, Water, Protein |


* **Daily Trend Chart:** A 5-day bar chart showing daily progress for the **three key metrics** associated with the selected goal (e.g., Calories, Protein, Fiber for Weight Loss).
* **Notification Progress:** A top status bar showing how close the user is to their daily health goals.
* **AI Warning System (Cảnh báo):** Triggers based on thresholds (e.g., "High fat intake detected" or "Water intake too low for current weight").
* **Overall Comment & Advice:** AI-generated synthesis of all data, providing a "big picture" health recommendation for the day.

#### **Screen B: Journey View (The Central Hub)**

This screen manages the history and the "to-do" list for eating.

* **Timeline Organization:** Divided by "Today," "Yesterday," etc.
* **Uncaptured Image Placeholders (`?`):** Empty boxes representing missing meal logs (Breakfast, Lunch, Dinner).
* **Action:** Clicking a `?` box opens the **Camera View**.


* **Captured Image Thumbnails:** Displays the photo of the meal already logged.
* **Action:** Clicking a captured photo opens the **Food Detail Screen**.



#### **Screen C: Camera View (Input)**

* **Capture Interface:** Standard camera UI to take a photo of the food.
* **Immediate Inference:** Once the photo is taken, the app automatically transitions to the **Food Detail Screen** to show the AI's results.

#### **Screen D: Food Detail (Inference Result)**

* **Image Preview:** Shows the captured meal.
* **Food Info (AI Analysis):** Displays inferred nutritional data (Calories, Carbs, Protein, Fat) from the image.
* **AI Meal Comment:** Specific feedback regarding *that specific meal* (e.g., "This meal is balanced, but consider adding more fiber").

#### **Screen E: Profile (User Info View)**

* **Core Personal Profile:** Input for Age, Sex, Height, Weight, Activity Level.
* **Health Context:** Multi-select for health conditions (e.g., Diabetes, IBS) and text input for Diet/Meds.
* **Primary Health Goals:** Selection of main goals (e.g., Weight loss, Muscle gain).
* **Action:** "Save" button updates the repository and reflects in AI analysis context.


---

### **2. Updated User Flow (Step-by-Step)**

The flow is designed to ensure no meal goes unlogged by using visual cues in the Journey.

#### **The Logging Loop**

1. **Entry:** User opens the **Journey View**.
2. **Trigger:** User sees an "Uncaptured" box (`?`) for Lunch.
3. **Capture:** User clicks the `?`  **Camera View** opens  User takes a photo.
4. **Inference:** The app processes the image and moves immediately to the **Food Detail Screen**.
5. **Review:** User views the nutritional info and AI comment, then returns to the Journey. The `?` is now replaced by the meal photo.

#### **The Review Loop**

1. **Selection:** User clicks a previously **Captured Image** in the **Journey View**.
2. **Detail:** The app opens the **Food Detail Screen** to show the saved nutritional info and AI comments for that specific past meal.

#### **The Dashboard Sync**

1. **Aggregation:** All data from the Food Detail screens is sent to the **Home Screen**.
2. **Visualization:** The **Phân Bổ Chart** and **Health Info** tiles update.
3. **Coaching:** The **Overall Comment** on the Home screen adjusts based on the newly added meal data.

---


## **3. Tech Stack**

### **3.1. Mobile App (Jetpack Compose)**

### **Project Structure & File Mapping**

The project is organized by feature within the `commonMain` source set. Here is the mapping of screens to their implementation files:

#### **File System Structure**
```text
composeApp/src/commonMain/kotlin/com/lea/stamp/
├── App.kt                      # Application entry point
├── Greeting.kt                 # Basic greeting class
├── Platform.kt                 # Platform specific definitions
├── data/
│   └── HealthRepository.kt     # Data repository for health metrics
└── ui/
    ├── MainScreen.kt           # Main navigation controller (Scaffold & Bottom Bar)
    ├── camera/
    │   └── CameraScreen.kt     # Implementation of Screen C
    ├── fooddetail/
    │   └── FoodDetailScreen.kt # Implementation of Screen D
    ├── home/                   # feature: Home (Screen A)
    │   ├── HomeScreen.kt
    │   ├── HomeState.kt
    │   ├── HomeViewModel.kt
    │   └── components/         # Home-specific UI components
    │       ├── AIAdviceSection.kt
    │       ├── DailyProgressBar.kt
    │       ├── HealthMetricTile.kt
    │       └── MacroNutrientChart.kt
    └── journey/                # feature: Journey (Screen B)
        ├── JourneyScreen.kt
        └── JourneyViewModel.kt
    └── userinfo/               # feature: Profile (Screen E)
        ├── UserInfoScreen.kt
        └── UserInfoViewModel.kt
```

#### **Screen to File Mapping**

| Screen Name | Feature Description | Main File | State/Logic |
| :--- | :--- | :--- | :--- |
| **Home Screen** | Health Dashboard, charts, and metrics | `ui/home/HomeScreen.kt` | `ui/home/HomeViewModel.kt`<br>`data/HealthRepository.kt` |
| **Journey Screen** | Timeline of meals, logging history | `ui/journey/JourneyScreen.kt` | `ui/journey/JourneyViewModel.kt` |
| **Camera Screen** | Capture food photos | `ui/camera/CameraScreen.kt` | *State handled internally or by parent* |
| **Food Detail** | AI analysis results for a meal | `ui/fooddetail/FoodDetailScreen.kt` | *Receives `Meal` object* |
| **Profile Screen** | User personal info & health goals | `ui/userinfo/UserInfoScreen.kt` | `ui/userinfo/UserInfoViewModel.kt` |
| **Main Navigation** | Bottom bar and destinations | `ui/MainScreen.kt` | *Manages app-level navigation state* |

### **Common Utilities**
Currently, the project **does not** have a general `util` package or common shared utilities directory.
- Reusable UI components specific to the **Home** feature are located in `ui/home/components`.
- Shared logic regarding platform specifics is in `Platform.kt`.
