# NutriVerse — AI-Powered Nutrition & Diet Planner

NutriVerse is an Android application designed to make everyday nutrition management easier through food tracking, AI-powered assistance, personalized diet planning, and nutrition insights.

## About the Project

NutriVerse brings multiple nutrition-management tools together in one application. It helps users record meals, explore food information, organize groceries, manage pantry items, and review their nutrition progress.

## Features

- **Food & Meal Tracking** — Record meals and monitor nutritional intake.
- **Food Catalog** — Search and browse available food items.
- **Barcode Scanner** — Look up packaged food products.
- **AI Food Recognition** — Analyze food images to identify possible food items.
- **AI Diet Planner** — Generate personalized diet-plan drafts for review.
- **Nutrition Dashboard** — Review nutritional intake and progress.
- **Health Report Analysis** — Analyze supported health-report information.
- **Pantry Management** — Organize food items and track expiry information.
- **Smart Recipes** — Explore recipe suggestions based on available ingredients.
- **Grocery Management** — Organize shopping lists and food-related expenses.
- **Weekly AI Review** — Receive AI-generated insights into nutrition patterns.

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | User interface |
| Material 3 | UI components and design |
| Clean Architecture | Separation of application responsibilities |
| MVVM | UI and presentation logic |
| Room Database | Local data storage |
| Firebase Authentication | User authentication |
| Cloud Firestore | Cloud data storage and synchronization |
| Firebase AI Logic | AI-powered features |
| OpenFoodFacts API | Food product information |
| CameraX | Camera integration |
| Google ML Kit | Barcode scanning |
| WorkManager | Background tasks |

## Architecture

NutriVerse follows Clean Architecture and the MVVM pattern to separate the user interface, business logic, and data operations.

The application uses Room for local data storage and an offline-first approach. Firebase services and external APIs support features that require cloud connectivity, food product information, or AI processing.

## Privacy and Security

The project aims to handle application data responsibly through authentication, appropriate permissions, input validation, and secure configuration.

API credentials and private configuration files should not be committed to the repository.

## Project Status

NutriVerse is an Android application project. Feature availability and behavior depend on the current implementation, device compatibility, and external service configuration.

## Disclaimer

NutriVerse is intended for general nutrition tracking and planning. AI-generated results may be inaccurate and should be reviewed before use. The application does not replace professional medical advice or consultation with a qualified nutrition professional.

## Developer

Developed as an Android application project using Kotlin and modern Android development technologies.
