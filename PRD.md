# PRODUCT REQUIREMENTS DOCUMENT (PRD)
## Chefly - AI-Powered Recipe App

**Document Version:** 1.0  
**Date:** May 26, 2026  
**Project Type:** Skripsi (Thesis) Project  
**Status:** Active Development

---

## 📋 TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [Problem Statement](#problem-statement)
3. [Product Goals & Objectives](#product-goals--objectives)
4. [Target Audience](#target-audience)
5. [Core Features](#core-features)
6. [Functional Requirements](#functional-requirements)
7. [Non-Functional Requirements](#non-functional-requirements)
8. [User Scenarios & Use Cases](#user-scenarios--use-cases)
9. [User Interface & Navigation](#user-interface--navigation)
10. [Technical Architecture](#technical-architecture)
11. [Data Models](#data-models)
12. [API & Data Specifications](#api--data-specifications)
13. [Performance & Scalability](#performance--scalability)
14. [Security & Permissions](#security--permissions)
15. [Release Plan & Timeline](#release-plan--timeline)
16. [Success Metrics & KPIs](#success-metrics--kpis)
17. [Constraints & Assumptions](#constraints--assumptions)
18. [Future Roadmap](#future-roadmap)
19. [Glossary](#glossary)

---

## EXECUTIVE SUMMARY

### Product Overview

**Chefly** adalah aplikasi mobile Android yang memanfaatkan teknologi **Artificial Intelligence (Computer Vision)** untuk memberikan pengalaman baru dalam menemukan resep makanan. Dengan menggunakan model **YOLOv8 TFLite**, aplikasi dapat mendeteksi bahan makanan secara real-time melalui kamera perangkat, membuat proses pencarian resep menjadi lebih intuitif dan interaktif.

### Key Value Propositions

- **🎯 Real-time Ingredient Detection**: Deteksi bahan makanan hanya dengan mengarahkan kamera
- **📱 Offline-First**: Semua proses deteksi dan inference berjalan on-device tanpa memerlukan internet
- **🔍 Smart Recipe Matching**: Rekomendasi resep berdasarkan bahan yang tersedia menggunakan cosine similarity
- **❤️ Personalized Experience**: Simpan resep favorit dan akses dengan mudah
- **🎨 Modern UI**: Interface menarik dengan Jetpack Compose dan Material Design 3

### Problem Solved

| Problem | Solution |
|---------|----------|
| Kesulitan menemukan resep sesuai bahan di dapur | Real-time ingredient detection & smart matching |
| Proses pencarian resep yang memakan waktu | Quick access dengan AI-powered recommendations |
| Ketergantungan internet | On-device ML processing (TFLite offline) |
| Pengalaman pengguna yang membosankan | Modern UI dengan animations dan interaktif design |

---

## PROBLEM STATEMENT

### Background

Banyak pengguna menghadapi challenge dalam menemukan resep yang sesuai dengan bahan-bahan yang mereka miliki di dapur. Proses tradisional memerlukan:

1. Mencatat semua bahan yang tersedia
2. Membuka aplikasi resep atau browser
3. Mengetik setiap bahan atau melakukan pencarian manual
4. Memfilter hasil secara manual

### Target User Challenges

- ⏱️ **Time-consuming**: Proses pencarian resep memakan waktu
- 🤔 **Decision Fatigue**: Terlalu banyak pilihan resep yang membingungkan
- 📝 **Manual Entry**: Perlu mengetik atau mengingat nama bahan
- 🔗 **Dependency on Internet**: Banyak aplikasi memerlukan koneksi internet stabil
- 😕 **Poor Accuracy**: Hasil pencarian tidak selalu relevan dengan apa yang pengguna inginkan

### Opportunity

Dengan memanfaatkan teknologi Computer Vision dan Machine Learning, kita dapat:
- Mengidentifikasi bahan secara otomatis melalui kamera
- Memberikan rekomendasi resep yang akurat dan relevan
- Meningkatkan engagement pengguna dengan experience yang lebih interaktif
- Menciptakan value yang unik di market aplikasi resep

---

## PRODUCT GOALS & OBJECTIVES

### Primary Goals

1. **🎯 Simplify Recipe Discovery**: Membuat proses menemukan resep semudah mengarahkan kamera
2. **🧠 Integrate AI/ML**: Mengimplementasikan state-of-the-art computer vision untuk ingredient detection
3. **📱 Create Engaging UX**: Deliver modern, intuitive user interface yang engaging
4. **🎓 Educational Value**: Demonstrasi implementasi TFLite, on-device ML, dan Jetpack Compose

### Secondary Goals

- Membangun user base yang loyal melalui personalization (favorites)
- Establish Chefly sebagai *recipe discovery tool* yang unik dan inovatif
- Gain experience dalam mobile development dengan teknologi cutting-edge
- Create foundation untuk future features dan monetization

### OKRs (Objectives & Key Results)

| Objective | Key Results |
|-----------|------------|
| **Deliver MVP v1.0** | ✓ Core features implemented<br/>✓ YOLOv8 detection working<br/>✓ Recipe database populated<br/>✓ UI polished & performant |
| **Achieve 60% Detection Accuracy** | ✓ Correctly identify 60%+ ingredients<br/>✓ Confidence scores above threshold<br/>✓ Handle common food items |
| **Ensure Smooth Performance** | ✓ Inference time < 500ms<br/>✓ Frame rate ≥ 30 FPS<br/>✓ Memory usage < 150MB |
| **Build User Engagement** | ✓ Average session duration > 5 mins<br/>✓ Return rate > 40%<br/>✓ Favorites saved per session |

---

## TARGET AUDIENCE

### Primary User Personas

#### 1. **The Busy Professional** 👨‍💼
- **Age**: 25-45 years old
- **Profile**: Bekerja penuh waktu, limited time untuk meal prep
- **Pain Points**: Mencari resep cepat, tidak punya waktu browsing
- **Needs**: Quick recipe suggestions, simple ingredients
- **Behavior**: Uses app 2-3x per week during cooking time

#### 2. **The Home Cook** 👩‍🍳
- **Age**: 35-65 years old
- **Profile**: Enjoy cooking, suka experiment dengan resep baru
- **Pain Points**: Terbatas dengan resep yang diketahui, forget ingredients at home
- **Needs**: Variety of recipes, detailed instructions, ingredient flexibility
- **Behavior**: Regular user, save favorites, high engagement

#### 3. **The Tech-Savvy Millennial** 📱
- **Age**: 18-35 years old
- **Profile**: Early adopter, interested in new technology
- **Pain Points**: Bored with traditional recipe apps, want innovative features
- **Needs**: Modern UI, AI features, social sharing
- **Behavior**: High engagement, frequent use, love trying new features

#### 4. **The Student/Budget-Conscious** 🎓
- **Age**: 18-30 years old
- **Profile**: Limited budget, want to maximize food inventory
- **Pain Points**: Wastage due to forgotten ingredients, limited money
- **Needs**: Ways to use existing ingredients, budget-friendly recipes
- **Behavior**: Occasional user, searching for specific recipes

### Inclusion Criteria

- Android users (API level 28 and above)
- Have smartphone with camera
- Interest in cooking or recipe discovery
- Language proficiency in Indonesian/English

### Exclusion Criteria

- Users without smartphone camera
- Dietary restrictions not supported by recipe database
- Technical limitations (device storage, older Android versions)

---

## CORE FEATURES

### 🌟 Feature Overview

#### Tier 1: MVP Features (Must Have)

| # | Feature | Priority | Status |
|---|---------|----------|--------|
| 1.1 | Real-time Ingredient Detection | 🔴 Critical | In Development |
| 1.2 | Recipe Browsing & Search | 🔴 Critical | In Development |
| 1.3 | Recipe Detail View | 🔴 Critical | In Development |
| 1.4 | Save Favorite Recipes | 🟡 High | In Development |
| 1.5 | Manual Ingredient Selection | 🟡 High | In Development |
| 1.6 | Onboarding & Splash Screen | 🟡 High | In Development |
| 1.7 | Home Screen Dashboard | 🟡 High | In Development |

#### Tier 2: Enhanced Features (Nice to Have)

| # | Feature | Priority | Timeline |
|---|---------|----------|----------|
| 2.1 | Photo Gallery Import | 🟢 Medium | v1.1 |
| 2.2 | Advanced Filters (Category, Cook Time) | 🟢 Medium | v1.1 |
| 2.3 | Recipe Ratings & Reviews | 🟢 Medium | v1.2 |
| 2.4 | Shopping List Generation | 🟢 Medium | v1.2 |
| 2.5 | Nutritional Information | 🟡 High | v1.3 |
| 2.6 | Step-by-step Cooking Mode | 🟡 High | v1.3 |

#### Tier 3: Future Enhancements (Nice to Have)

| # | Feature | Priority | Timeline |
|---|---------|----------|----------|
| 3.1 | User Authentication | 🟢 Medium | v2.0 |
| 3.2 | Cloud Sync & Backup | 🟢 Medium | v2.0 |
| 3.3 | Social Sharing | 🟢 Medium | v2.0 |
| 3.4 | Meal Planning Calendar | 🟢 Medium | v2.1 |
| 3.5 | Voice Commands | 🟢 Medium | v2.2 |
| 3.6 | Multi-language Support | 🟡 High | v2.2 |
| 3.7 | Dietary Filters | 🟡 High | v2.3 |
| 3.8 | Ingredi | 🟢 Ment Historyedium | v2.3 |

---

## FUNCTIONAL REQUIREMENTS

### 1. Ingredient Detection Module

#### 1.1 Real-time Camera Detection

**Requirement ID**: FR-1.1  
**Description**: App must capture live camera feed and detect food ingredients in real-time

**Functional Specifications**:

```
GIVEN: User opens Camera/Scan screen
WHEN: Camera is active and pointed at food items
THEN:
  - Live video feed displays on screen
  - Detected ingredients show with bounding boxes
  - Confidence scores displayed for each detection
  - Detection results update every frame (≥30 FPS)
  - User can tap on detected item to get more info
```

**Acceptance Criteria**:

- [ ] Camera feed runs smoothly at 30+ FPS
- [ ] Detection boxes update in real-time (<500ms latency)
- [ ] Confidence threshold >= 40% by default
- [ ] Maximum 10 simultaneous detections displayed
- [ ] Support landscape and portrait orientations
- [ ] Handle various lighting conditions

**Technical Specification**:
- Model: YOLOv8 TFLite (640x640 resolution)
- Framework: TensorFlow Lite 2.13.0
- Threads: 4 (configurable)
- Acceleration: XNNPACK enabled
- Input Size: 640x640 pixels
- Output Tensor: 1 x 25200 x (4 + numClasses)

#### 1.2 Photo Upload & Detection

**Requirement ID**: FR-1.2  
**Description**: Allow users to upload photos from gallery and detect ingredients

**Functional Specifications**:

```
GIVEN: User selects "Choose from Gallery" option
WHEN: User picks image from device storage
THEN:
  - Image loads and displays
  - Detection runs on selected image
  - Results show with same accuracy as camera
  - User can proceed to recipe matching
```

**Acceptance Criteria**:

- [ ] Support JPEG, PNG, and WebP formats
- [ ] Handle images up to 10MB
- [ ] Resize image intelligently without quality loss
- [ ] Show loading indicator during processing
- [ ] Cache detection results

---

### 2. Recipe Management Module

#### 2.1 Recipe Browsing

**Requirement ID**: FR-2.1  
**Description**: Users can browse and search recipes from database

**Functional Specifications**:

```
GIVEN: User navigates to Recipe/Resep screen
WHEN: Screen loads
THEN:
  - Display list of all recipes with pagination
  - Show recipe cards with image, name, category
  - Load additional recipes when scrolling to bottom
  - Display 10-20 recipes per page
  - Support smooth scrolling performance
```

**Acceptance Criteria**:

- [ ] Implement pagination (20 items per page)
- [ ] Display recipe image, name, category
- [ ] Show key info: cook time, servings, difficulty
- [ ] Smooth infinite scroll behavior
- [ ] Search functionality enabled
- [ ] Filter by category available

#### 2.2 Recipe Search

**Requirement ID**: FR-2.2  
**Description**: Users can search recipes by name or ingredients

**Functional Specifications**:

```
GIVEN: User enters search query
WHEN: Text typed in search field
THEN:
  - Show real-time search results (debounced)
  - Filter recipes by name containing query
  - Filter recipes by ingredients
  - Show number of results found
  - Allow sorting by relevance
```

**Acceptance Criteria**:

- [ ] Case-insensitive search
- [ ] Search debouncing (300-500ms)
- [ ] Show results count
- [ ] Support partial word matching
- [ ] Highlight search terms in results
- [ ] Remember recent searches

#### 2.3 Recipe Detail View

**Requirement ID**: FR-2.3  
**Description**: Display comprehensive recipe information

**Functional Specifications**:

```
GIVEN: User taps on a recipe
WHEN: Recipe detail screen opens
THEN:
  - Display full recipe information:
    - Recipe name & image
    - Category & cooking method
    - Complete ingredient list
    - Step-by-step instructions
    - Cooking time & servings
    - Similarity score (if applicable)
  - Show favorite toggle button
  - Enable sharing functionality (future)
```

**Acceptance Criteria**:

- [ ] Display all recipe metadata
- [ ] Ingredients formatted as readable list
- [ ] Steps formatted as numbered list
- [ ] Show similarity score when from detection
- [ ] Favorite button toggles state
- [ ] Image loads efficiently
- [ ] Responsive layout for different screen sizes

---

### 3. Ingredient Detection & Matching

#### 3.1 Manual Ingredient Selection

**Requirement ID**: FR-3.1  
**Description**: Users can manually add ingredients if detection fails

**Functional Specifications**:

```
GIVEN: User on Add Ingredient/TambahBahan screen
WHEN: User searches or scrolls ingredient list
THEN:
  - Display list of available ingredients
  - User can search ingredients by name
  - User selects ingredients from list
  - Selected ingredients show in chips/tags
  - User confirms selection
  - System finds recipes matching ingredients
```

**Acceptance Criteria**:

- [ ] Ingredient list searchable
- [ ] Support multiple ingredient selection
- [ ] Show selected ingredients as tags
- [ ] Clear button to reset selection
- [ ] Search is case-insensitive
- [ ] Display ingredient count

#### 3.2 Smart Recipe Matching

**Requirement ID**: FR-3.2  
**Description**: Match detected ingredients to recipes using similarity scoring

**Functional Specifications**:

```
GIVEN: User has detected/selected ingredients
WHEN: User taps "Find Recipes" or auto-navigate
THEN:
  - System calculates cosine similarity
  - Filter recipes containing matched ingredients
  - Sort by similarity score (highest first)
  - Display results with similarity percentage
  - Show matching and non-matching ingredients
```

**Technical Implementation**:

```
Algorithm: Cosine Similarity
- Represent ingredients as vectors
- Calculate dot product of detected vs recipe ingredients
- Divide by magnitude of both vectors
- Normalize score to 0-100%

Formula: similarity = (A · B) / (||A|| × ||B||)

Threshold: Show recipes with >20% similarity
Sorting: Primary by similarity, secondary by popularity (loves)
```

**Acceptance Criteria**:

- [ ] Similarity scoring accurate
- [ ] Results sorted by relevance
- [ ] Show percentage match
- [ ] Highlight matching ingredients
- [ ] Recipes with 0% match hidden

---

### 4. Favorites Management

#### 4.1 Save Favorite Recipes

**Requirement ID**: FR-4.1  
**Description**: Users can save recipes as favorites for quick access

**Functional Specifications**:

```
GIVEN: User viewing recipe detail
WHEN: User taps favorite/heart button
THEN:
  - Recipe marked as favorite
  - Button state changes (filled/unfilled)
  - Data persisted to local database
  - Favorite appears in Saved screen
  - User can remove from favorite anytime
```

**Acceptance Criteria**:

- [ ] Favorite data persists across app restarts
- [ ] Visual feedback on favorite toggle
- [ ] Favorites accessible from Saved screen
- [ ] Count of favorites displayed
- [ ] Bulk operations (delete all) available

#### 4.2 Saved Recipes Screen

**Requirement ID**: FR-4.2  
**Description**: Display all saved favorite recipes

**Functional Specifications**:

```
GIVEN: User navigates to Saved/Tersimpan screen
WHEN: Screen loads
THEN:
  - Display list of favorited recipes
  - Show same recipe cards as browse view
  - Sort by recent additions
  - Allow removal from favorites
  - Show empty state if no favorites
```

**Acceptance Criteria**:

- [ ] Display all favorite recipes
- [ ] Empty state message when no favorites
- [ ] Swipe to remove option
- [ ] Sort by date added
- [ ] Pagination if many favorites

---

### 5. User Onboarding

#### 5.1 Splash Screen

**Requirement ID**: FR-5.1  
**Description**: Display splash screen on app startup

**Functional Specifications**:

```
GIVEN: App starts
WHEN: Application initializes
THEN:
  - Splash screen displays for 2-3 seconds
  - Show app logo & name
  - Check onboarding completion status
  - Navigate to onboarding or home
```

**Acceptance Criteria**:

- [ ] Display animation during splash
- [ ] Check onboarding status
- [ ] Proper navigation flow
- [ ] Hide after specified duration

#### 5.2 Onboarding Flow

**Requirement ID**: FR-5.2  
**Description**: Welcome new users with app tour

**Functional Specifications**:

```
GIVEN: First-time user runs app
WHEN: Splash screen completes
THEN:
  - Show onboarding screens (3-5 slides)
  - Each slide explains feature with visuals
  - Show "Get Started" button on last slide
  - Mark onboarding as complete
  - Navigate to home screen
```

**Onboarding Slides**:

1. **Welcome**: "Temukan resep baru setiap hari"
2. **Scan**: "Tunjukkan kamera ke bahan makanan"
3. **Discover**: "Dapatkan rekomendasi resep"
4. **Save**: "Simpan resep favorit Anda"
5. **Get Started**: Action button

**Acceptance Criteria**:

- [ ] All slides display correctly
- [ ] Swipe/arrow navigation between slides
- [ ] Skip option available
- [ ] Completion status persisted
- [ ] Animations smooth

---

### 6. Home Screen Dashboard

#### 6.1 Home Screen

**Requirement ID**: FR-6.1  
**Description**: Central hub showing key features and suggestions

**Functional Specifications**:

```
GIVEN: User opens app and completes onboarding
WHEN: Home screen loads
THEN:
  - Display quick action buttons (Scan, Search)
  - Show trending/suggested recipes
  - Show recent favorites
  - Show quick stats (total favorites, etc)
  - Circular progress for onboarding
```

**Screen Sections**:

1. **Header Section**:
   - Greeting message (context-aware)
   - Quick action buttons

2. **Suggested Recipes Section**:
   - 3-5 recipe cards
   - Horizontally scrollable
   - "See All" link

3. **Recent Searches/Views**:
   - Show recently viewed recipes
   - Quick re-access to previous items

4. **Footer**:
   - Floating action button for camera

**Acceptance Criteria**:

- [ ] All sections load correctly
- [ ] Smooth scrolling performance
- [ ] Navigation to other screens works
- [ ] Quick action buttons functional
- [ ] Responsive layout

---

## NON-FUNCTIONAL REQUIREMENTS

### Performance Requirements

| Requirement | Specification | Measurement |
|-------------|---------------|-------------|
| **Inference Speed** | <500ms per detection | Frame capture to result display |
| **Frame Rate** | ≥30 FPS | Camera preview smoothness |
| **App Startup Time** | <3 seconds | Cold start to home screen |
| **Screen Response Time** | <200ms | Tap to screen transition |
| **Memory Usage** | <150MB average | Peak memory during detection |
| **Battery Consumption** | Minimal drain | <10% per hour camera use |
| **Storage** | <100MB app size | Installation package size |

### Scalability Requirements

- Support for 500+ recipes in database
- Handle pagination with unlimited data
- Support concurrent user operations
- Efficient database queries (sub-100ms)
- Image caching for performance

### Reliability & Availability

| Requirement | Target |
|-------------|--------|
| **App Crash Rate** | <0.1% of sessions |
| **Camera Availability** | 99% uptime |
| **Database Availability** | 99.9% |
| **Mean Time to Recovery** | <1 minute for crashes |
| **Data Integrity** | 100% |

### Compatibility

```
Supported Android Versions:
- Minimum: Android 9.0 (API Level 28)
- Target: Android 16 (API Level 36)
- Security patches: Latest available

Device Requirements:
- RAM: ≥2GB minimum
- Storage: ≥100MB free space
- Camera: 8MP minimum (2MP functional)
- Processor: ARM64 architecture
```

### Accessibility

- [ ] WCAG 2.1 Level AA compliance
- [ ] Support for screen readers
- [ ] Minimum text size 16sp
- [ ] High contrast colors for visibility
- [ ] Touch target minimum 48x48dp

### Security Requirements

- [ ] HTTPS for any network communication
- [ ] Local data encryption at rest
- [ ] No sensitive data in logs
- [ ] Secure permission handling
- [ ] Input validation on all screens

---

## USER SCENARIOS & USE CASES

### Use Case 1: Quick Dinner Discovery

**User**: The Busy Professional  
**Scenario**:

```
Time: 6 PM, workday just ended
Location: Kitchen at home
Task: Need dinner idea quickly

Flow:
1. Open Chefly app (already installed from previous use)
2. Tap "Scan/Pindai" button from home screen
3. Opens camera view
4. Points camera at fridge/kitchen counter
5. App detects: chicken, tomato, garlic, onion
6. Shows detection results with 92% confidence
7. Taps "Find Recipes" button
8. See 8 recipes containing these ingredients
9. Selects "Lemon Butter Chicken" (92% match)
10. Views detailed recipe
11. Starts cooking in 15 minutes

Success Metrics:
- Task completed in <3 minutes
- Confident in recipe choice
- Easy-to-follow instructions
```

**User Pain Points Solved**:
- ✓ No manual ingredient entry
- ✓ Quick results
- ✓ Relevant recommendations
- ✓ No internet needed (offline)

---

### Use Case 2: Exploring New Recipes

**User**: The Home Cook  
**Scenario**:

```
Time: Saturday morning
Location: Home, planning week meals
Task: Find interesting new recipes to try

Flow:
1. Open Chefly app
2. Navigate to "Recipes/Resep" tab
3. Browse featured recipes
4. Swipe through recipe cards
5. Find "Asian Fusion Bowl" recipe
6. Tap to see full details
7. Review ingredients & steps
8. Tap heart icon to save as favorite
9. Continue browsing other categories
10. Later: Access saved recipes from "Saved" tab

Success Metrics:
- Explored 5+ recipes in 10 minutes
- Found 2 recipes to try
- Easy to save favorites
```

**User Experience Benefits**:
- ✓ Discover variety easily
- ✓ Quick access to full details
- ✓ Persistent favorites
- ✓ Organized recipe collection

---

### Use Case 3: Ingredient-Based Search

**User**: The Tech-Savvy Millennial  
**Scenario**:

```
Time: Evening, shopping for dinner
Location: Grocery store
Task: Find recipes for ingredients already bought

Flow:
1. Buy: salmon, asparagus, lemon, butter
2. Get home, open Chefly
3. Tap "Add Ingredients/TambahBahan"
4. Search for "salmon" - select it
5. Search for "asparagus" - select it
6. Search for "lemon" - select it
7. Search for "butter" - select it
8. Tap "Find Recipes"
9. See 4 recipes matching exactly these ingredients
10. Choose "Pan-Seared Salmon with Asparagus"
11. Cook immediately

Success Metrics:
- Manual ingredient search intuitive
- Relevant results
- No unrelated recipes
```

---

### Use Case 4: Building Favorites Library

**User**: Student / Budget-Conscious  
**Scenario**:

```
Time: Ongoing (multiple sessions)
Goal: Build collection of favorite quick recipes

Session 1:
- Find 3 recipes under 20min cook time
- Save all as favorites

Session 2:
- Open Saved tab
- See all 3 saved recipes
- Pick one to cook

Session 3:
- Find 2 new recipes to add
- Continue building library

Benefits:
- ✓ Personalized recipe collection
- ✓ Quick access to known recipes
- ✓ Reduce cooking time finding recipes
```

---

## USER INTERFACE & NAVIGATION

### Navigation Architecture

```
┌─────────────────────────────────────────┐
│         Splash Screen (2-3s)            │
└─────────────┬───────────────────────────┘
              │
              ├─ First Time? → Onboarding (5 screens)
              │                      ↓
              └─ Returning? → ──────→ Main Screen
                                      │
        ┌─────────────────────────────┼─────────────────────────┐
        │                             │                         │
    ┌───▼───┐  ┌────────┐  ┌──────────▼──────┐  ┌──────────┐  ┌──▼────────┐
    │Beranda│  │ Pindai │  │     Resep      │  │ Tersimpan│  │   More    │
    │(Home) │  │ (Scan) │  │  (Recipes)     │  │ (Saved)  │  │ (Future)  │
    └───┬───┘  └───┬────┘  └────────┬───────┘  └──────────┘  └───────────┘
        │          │               │
        └──────────┼───────────────┴──────────────────┐
                   │                                  │
            ┌──────▼──────────┐          ┌───────────▼─────────┐
            │ Recipe Details  │          │ Recipe Search View  │
            │ - Save/Unsave   │          │ - Filter Results    │
            │ - View Steps    │          │ - Tap to Details    │
            └─────────────────┘          └─────────────────────┘
                   ▲                              │
                   │                              │
            ┌──────┴──────────┐                   │
            │ Device Camera   │                   │
            │ - Detection     │                   │
            │ - Add Manual    │◄──────────────────┘
            │   Ingredients   │
            └─────────────────┘
```

### Screen Structure

#### 1. **Splash Screen**
- Logo & app name
- Animated loader
- Duration: 2-3 seconds
- Navigation: → Onboarding XOR Home

#### 2. **Onboarding Screens** (5 slides)
- Slide 1: Welcome message
- Slide 2: Camera detection feature
- Slide 3: Recipe discovery
- Slide 4: Save favorites
- Slide 5: Get Started button

**Navigation**: ← Previous | Next → | Skip | Get Started

#### 3. **Home/Beranda Screen**
**Components**:
- Top App Bar with greeting
- Quick action buttons (Scan, Search)
- Suggested recipes section
- Recent items section
- Bottom navigation

**Actions**: Scan, Search, Browse Recipes, View Saved

#### 4. **Scan/Pindai Screen**
**Components**:
- Full-screen camera preview
- Detection overlay with bounding boxes
- Detected ingredients list (bottom sheet)
- "Find Recipes" button
- "Add Manual" button

**Actions**: Capture/Toggle camera, Tap detection to view, Find recipes

#### 5. **Add Ingredients/TambahBahan Screen**
**Components**:
- Search field for ingredients
- Scrollable ingredient list
- Selected ingredients (chip tags)
- "Find Recipes" button

**Actions**: Search, Select/Deselect, Clear all, Find recipes

#### 6. **Recipes/Resep Screen**
**Components**:
- Top app bar with search
- Recipe list with pagination
- Recipe cards (image, name, category)
- Category filter chips

**Actions**: Search, Filter, Tap recipe for details, Infinite scroll

#### 7. **Recipe Detail Screen**
**Components**:
- Recipe image (full width)
- Recipe name & category
- Metadata (cook time, servings)
- Ingredients list
- Step-by-step instructions
- Similarity score (if applicable)
- Favorite button
- Share button (future)

**Actions**: Save/Unsave favorite, Share, Scroll to see all

#### 8. **Saved/Tersimpan Screen**
**Components**:
- Top app bar "My Saved Recipes"
- List of favorite recipes
- Empty state message
- Swipe to delete option

**Actions**: Tap to view details, Swipe to remove, Pull to refresh

---

### Design System

#### Color Scheme (Material Design 3)

```
Primary Colors:
- Terracotta: #E36C47 (Primary accent)
- Warm Ivory: #F5E6D3 (Background)
- Dark Brown: #664033 (Text primary)

Semantic Colors:
- Success: #4CAF50 (Green)
- Warning: #FF9800 (Orange)
- Error: #F44336 (Red)
- Info: #2196F3 (Blue)

Neutral Colors:
- Surface: #FFFFFF
- Surface Variant: #EFEFEF
- Outline: #CCCCCC
```

#### Typography

```
Font Family: Rubik (default system font)

Font Sizes:
- H1: 32sp / Bold
- H2: 28sp / Bold
- H3: 24sp / Semi-Bold
- Caption: 16sp / Regular
- Body1: 16sp / Regular
- Body2: 14sp / Regular
- Label: 12sp / Medium
```

#### Component Sizes

```
Touch Targets: 48x48dp minimum
Card Padding: 16dp
Screen Padding: 16dp
Icon Size: 24x24dp (standard), 32x32dp (large)
Button Height: 48dp
Input Height: 56dp
```

#### Animation Specs

```
Duration: 250-400ms (standard)
Easing: Cubic Bezier (ease-in-out)
Transitions: Fade, Slide, Scale
Lottie: Complex animations for onboarding
```

---

## TECHNICAL ARCHITECTURE

### Architecture Pattern: MVVM with Clean Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Presentation Layer                     │
│  (UI - Compose, ViewModel, Navigation)                  │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│                    Domain Layer                          │
│  (Business Logic - Use Cases, Entities)                 │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│                    Data Layer                            │
│  (Repositories, Data Sources - Local DB, ML)            │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                  Cross-Cutting Concerns                  │
│  (DI, Logging, Error Handling, Utilities)               │
└──────────────────────────────────────────────────────────┘
```

### Technology Stack

#### Frontend Framework
- **Jetpack Compose**: Declarative UI framework
- **Jetpack Navigation**: Navigation Compose for screen routing
- **Material Design 3**: Design system
- **Lottie**: Animation library

#### Data & Storage
- **Room Database**: Local SQLite database
- **DataStore Preferences**: Persistent key-value storage
- **Coil**: Image loading and caching

#### Machine Learning
- **TensorFlow Lite 2.13.0**: ML inference
- **TensorFlow Lite GPU/NNAPI**: Acceleration (optional)
- **XNNPACK**: CPU acceleration for faster inference

#### Camera & Image Processing
- **CameraX**: Modern camera API
- **Image Analysis**: Real-time frame processing

#### Dependency Injection
- **Dagger Hilt**: DI framework for Android

#### Build Tool
- **Gradle 8.x**: Build system
- **Kotlin 1.9+**: Programming language

### Module Structure

```
app/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── RecipeDao.kt
│   │   └── entity/
│   │       ├── RecipeEntity.kt
│   │       └── ...
│   ├── model/
│   │   ├── DetectedIngredient.kt
│   │   ├── OnboardingPage.kt
│   │   └── ...
│   └── repository/
│       ├── RecipeRepository.kt
│       ├── IngredientRepository.kt
│       └── ...
├── ml/
│   ├── YOLO26Detector.kt
│   ├── DetectionTypes.kt
│   └── ...
├── ui/
│   ├── navigation/
│   │   ├── Screen.kt
│   │   └── ...
│   ├── screens/
│   │   ├── HomeScreen.kt
│   │   ├── CameraScreen.kt
│   │   ├── RecipeScreen.kt
│   │   ├── RecipeDetailScreen.kt
│   │   ├── SavedScreen.kt
│   │   ├── AddIngredientScreen.kt
│   │   ├── onboarding/
│   │   │   ├── OnboardingScreen.kt
│   │   │   └── components/
│   │   └── splash/
│   │       └── SplashScreen.kt
│   ├── viewmodel/
│   │   ├── MainViewModel.kt
│   │   ├── HomeViewModel.kt
│   │   ├── CameraViewModel.kt
│   │   ├── RecipeViewModel.kt
│   │   ├── RecipeDetailViewModel.kt
│   │   ├── SavedScreenViewModel.kt
│   │   ├── AddIngredientViewModel.kt
│   │   └── SharedViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Typography.kt
│       ├── Theme.kt
│       └── ...
├── di/
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── ...
├── util/
│   ├── Extensions.kt
│   └── Constants.kt
└── CheflyApplication.kt / MainActivity.kt
```

---

## DATA MODELS

### Recipe Model

```kotlin
data class Recipe(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val ingredients: String,        // Comma/semicolon delimited
    val steps: String,              // Newline delimited
    val totalIngredients: Int?,
    val totalSteps: Int?,
    val loves: Int?,                // Popularity metric
    val cookingMethod: String?,
    val isFavorite: Boolean = false,
    val similarity: Float = 0f      // Cosine similarity score
)
```

**Field Descriptions**:

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| id | String | Unique identifier | UUID format, required |
| name | String | Recipe name | Max 100 chars, required |
| imageUrl | String | Recipe image URL | Valid URL format |
| category | String | Food category | Enum: "Italian", "Asian", etc |
| ingredients | String | Ingredient list | Delimited string, required |
| steps | String | Cooking steps | Numbered/bulleted steps |
| totalIngredients | Int | Count | Calculated from ingredients |
| totalSteps | Int | Count | Calculated from steps |
| loves | Int | Popularity | ≥0, for sorting |
| cookingMethod | String | Cooking technique | Max 50 chars |
| isFavorite | Boolean | Bookmark flag | Default false |
| similarity | Float | Match score | 0.0-1.0 range |

---

### DetectedIngredient Model

```kotlin
data class DetectedIngredient(
    val label: String,             // Ingredient name
    val confidence: Float,         // 0.0-1.0 confidence score
    val boundingBox: RectF,        // Coordinates on image
    val imageUrl: String? = null   // Reference image
)
```

**Field Descriptions**:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| label | String | Ingredient name | "apple", "chicken" |
| confidence | Float | Detection confidence (0-1) | 0.92 |
| boundingBox | RectF | Screen coordinates | left, top, right, bottom |
| imageUrl | String | Ingredient image | Optional reference image |

---

### OnboardingPage Model

```kotlin
data class OnboardingPage(
    val title: String,
    val description: String,
    val imageResId: Int,           // Drawable resource ID
    val lottieAnimation: String?   // Lottie JSON file
)
```

---

### Database Entities

#### RecipeEntity

```kotlin
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val ingredients: String,
    val steps: String,
    val totalIngredients: Int?,
    val totalSteps: Int?,
    val loves: Int?,
    val cookingMethod: String?,
    @ColumnInfo(defaultValue = "false")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val similarity: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

## API & DATA SPECIFICATIONS

### Local Database Schema

#### recipes table

```sql
CREATE TABLE recipes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    imageUrl TEXT,
    category TEXT,
    ingredients TEXT NOT NULL,
    steps TEXT NOT NULL,
    totalIngredients INTEGER,
    totalSteps INTEGER,
    loves INTEGER,
    cookingMethod TEXT,
    isFavorite BOOLEAN DEFAULT 0,
    similarity REAL DEFAULT 0,
    createdAt INTEGER,
    updatedAt INTEGER
);

CREATE INDEX idx_recipes_category ON recipes(category);
CREATE INDEX idx_recipes_isFavorite ON recipes(isFavorite);
CREATE INDEX idx_recipes_name ON recipes(name);
```

### Recipe Data Format

#### CSV Import Format

```csv
id,name,imageUrl,category,ingredients,steps,totalIngredients,totalSteps,loves,cookingMethod
1,Simple Pasta,https://...,Italian,"flour,egg,salt",Mix and cook,3,2,100,Boiling
2,Fried Rice,https://...,Asian,"rice,egg,oil",Stir fry and combine,3,3,150,Pan frying
```

#### JSON API Response Format (Future)

```json
{
  "status": "success",
  "data": {
    "recipes": [
      {
        "id": "1",
        "name": "Simple Pasta",
        "imageUrl": "https://...",
        "category": "Italian",
        "ingredients": "flour,egg,salt",
        "steps": "Mix and cook",
        "totalIngredients": 3,
        "totalSteps": 2,
        "loves": 100,
        "cookingMethod": "Boiling"
      }
    ]
  }
}
```

---

## PERFORMANCE & SCALABILITY

### ML Inference Performance

| Component | Target | Measurement |
|-----------|--------|-------------|
| **Model Load Time** | <500ms | Initial load from assets |
| **Single Inference** | <500ms | Per frame processing |
| **Frame Capture** | <33ms | 30 FPS target |
| **Post-processing** | <100ms | NMS, filtering, scaling |
| **Total Latency** | <800ms | Capture to display |

### Memory Management

```
Target Memory Usage:
- App Baseline: 40-50MB
- With Camera Active: 70-100MB
- During Inference: 100-150MB
- Peak: <200MB

Memory Allocation Strategy:
- Pre-allocate buffers for input tensor (640×640×3)
- Reuse output buffers
- Clear large objects after processing
- Use object pooling for frequently allocated objects
```

### Database Performance

```
Query Performance Targets:
- Fetch all recipes: <100ms
- Search recipes by name: <50ms
- Filter by category: <30ms
- Get recipe by ID: <10ms
- Update favorite status: <20ms

Pagination Strategy:
- Page size: 20 recipes
- Lazy load on scroll
- Cache pages in memory (5 pages max)
- Pre-fetch next page on scroll event
```

### Storage Optimization

```
Expected Space Usage:
- APK Size: <100MB (with model)
- Database (500 recipes): ~5-10MB
- Image Cache: 20-50MB (configurable)
- Total Installation: <150MB

Compression:
- .tflite model: Not compressed (required)
- Images: Cached with Coil
- Database: SQLite default compression
```

### Scalability Considerations

#### For 1,000+ Recipes

```
Optimization Strategies:
1. Implement full-text search (FTS) in SQLite
2. Add database indexes on frequently searched fields
3. Implement recipe caching on disk
4. Use pagination (mandatory)
5. Implement search result caching
6. Add recipe categorization for quick filtering
```

#### For Real-time Sync (Future)

```
Cloud Architecture (v2.0):
- Firebase Realtime Database or Cloud Firestore
- Offline-first with local sync
- Conflict resolution strategy
- Incremental sync
- Background sync service
```

---

## SECURITY & PERMISSIONS

### Required Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

### Permission Handling

#### Camera Permission

```
Requirement: Camera permission required for Scan feature
Handling Strategy:
- Check permission at runtime (API 23+)
- Request permission with rationale
- Handle grant/deny scenarios
- Disable camera feature if denied
- Allow user to enable later via settings
```

#### Internet Permission

```
Purpose: Image loading, future cloud sync
Handling: Required for app functionality
Scope: HTTPS only for secure connections
```

### Data Security

#### Local Storage

```
Sensitive Data Protection:
- User favorites stored locally (encrypted with native encryption)
- Detection history not persisted
- No authentication tokens stored
- No API credentials in code
- No sensitive logging
```

#### Network Communication

```
HTTPS Enforcement:
- All external URLs use HTTPS
- Certificate pinning (optional)
- No data sent over HTTP
- Timeout policies (30 seconds)
```

#### ML Model Security

```
Model Protection:
- Model file stored in assets (read-only)
- Not accessible to other apps
- Integrity verification on load
- No model file export
```

---

## RELEASE PLAN & TIMELINE

### Version Strategy

```
Versioning: Semantic Versioning (x.y.z)
- x: Major (breaking changes)
- y: Minor (features)
- z: Patch (bug fixes)

Current Version: 1.0.0 (MVP Release)
Target Release: Q2 2026
```

### Release Roadmap

#### **v1.0.0 - MVP Release** [Q2 2026]

**Features**:
- Real-time ingredient detection (YOLOv8)
- Recipe browsing with pagination
- Recipe search by name/ingredients
- Save favorite recipes
- Manual ingredient selection
- Onboarding flow
- Splash screen

**Timeline**: 8 weeks
- Week 1-2: Core UI & navigation
- Week 3-4: ML integration & detection
- Week 5-6: Database & repository
- Week 7: Testing & QA
- Week 8: Launch preparation

**Success Criteria**:
- [ ] All MVP features working
- [ ] Detection accuracy > 60%
- [ ] Frame rate ≥ 30 FPS
- [ ] App size < 100MB
- [ ] Zero critical bugs
- [ ] User acceptance testing passed

---

#### **v1.1.0 - Enhanced Features** [Q3 2026]

**New Features**:
- Photo gallery import
- Advanced recipe filters (cook time, difficulty)
- Recipe ratings display
- Shopping list generation
- Detection confidence visualization

**Improvements**:
- Performance optimization
- UI/UX refinement
- Bug fixes from v1.0

**Timeline**: 6 weeks

---

#### **v1.2.0 - Smart Features** [Q4 2026]

**New Features**:
- Nutritional information display
- Ingredient substitution suggestions
- Related recipes recommendations
- Multiple language support (Start with Indonesian)

**Timeline**: 8 weeks

---

#### **v2.0.0 - Cloud & Social** [2027]

**Major Features**:
- User authentication
- Cloud sync with Firebase
- Social recipe sharing
- User ratings & reviews
- Meal planning calendar
- Voice commands

**Timeline**: 12 weeks

---

### Release Process

```
Pre-Release:
1. Code freeze (72 hours before)
2. Final QA testing
3. Performance profiling
4. Security audit
5. App signing

Release:
1. Build release APK
2. Upload to Google Play
3. Beta testing (1 week)
4. Rollout (25% → 50% → 100%)
5. Monitor crash rate

Post-Release:
1. Monitor user feedback
2. Track metrics
3. Plan next sprint
4. Hotfix any critical issues
```

---

## SUCCESS METRICS & KPIS

### User Metrics

| Metric | Target | Frequency |
|--------|--------|-----------|
| **Daily Active Users (DAU)** | 100+ (v1.0) | Daily |
| **Monthly Active Users (MAU)** | 500+ (v1.0) | Monthly |
| **User Retention (Day 7)** | 40%+ | Weekly |
| **User Retention (Day 30)** | 20%+ | Monthly |
| **Churn Rate** | <30% | Monthly |

### Engagement Metrics

| Metric | Target | Frequency |
|--------|--------|-----------|
| **Average Session Length** | 5+ minutes | Daily |
| **Session Frequency** | 3+ per week | Weekly |
| **Feature Usage** | 60%+ use camera | Weekly |
| **Favorite Additions** | 5+ per session | Weekly |
| **Recipe Views** | 10+ per session | Daily |

### Technical Metrics

| Metric | Target | Frequency |
|--------|--------|-----------|
| **Crash Rate** | <0.1% | Daily |
| **App Load Time** | <3 seconds | Daily |
| **Detection accuracy** | >60% | Weekly |
| **Frame Rate** | ≥30 FPS | Daily |
| **Memory Usage** | <150MB avg | Daily |
| **API Response Time** | <100ms | Daily |

### Business Metrics

| Metric | Target | Frequency |
|--------|--------|-----------|
| **App Store Rating** | 4.5+ stars | Monthly |
| **User Reviews** | 50+ reviews | Monthly |
| **Download Count** | 1,000+ | Monthly |
| **Positive Feedback %** | 80%+ | Monthly |
| **Support Tickets** | <20/month | Monthly |

### ML Model Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| **Detection Accuracy** | >60% | % of correct detections |
| **Precision** | >70% | % of detections that are correct |
| **Recall** | >50% | % of actual ingredients detected |
| **Inference Speed** | <500ms | Per frame processing time |
| **Supported Ingredients** | 50+ | Types of detectable items |

---

## CONSTRAINTS & ASSUMPTIONS

### Technical Constraints

| Constraint | Impact | Mitigation |
|-----------|--------|-----------|
| Android 9.0 minimum | Older devices excluded | No legacy API support |
| 150MB max memory | Limited ML model size | Use optimized YOLOv8n |
| Camera API dependency | Device without camera incompatible | Check at app start |
| TFLite inference on CPU | Slower on low-end devices | Multi-threading, XNNPACK |

### Business Constraints

| Constraint | Details |
|-----------|---------|
| **Development Time** | 8 weeks for MVP |
| **Budget** | Limited to open-source libraries |
| **Team Size** | 1 engineer (thesis project) |
| **Deployment** | Google Play Store only (v1.0) |
| **Database** | Pre-loaded, no backend sync |

### User Constraints

| Constraint | Details |
|-----------|---------|
| **Device Requirements** | Android 9.0+, 2GB RAM, camera |
| **Network** | Camera feature offline-capable |
| **Ingredients** | Limited to COCO dataset |
| **Languages** | Indonesian/English only (v1.0) |
| **Recipes** | Pre-loaded (no user generation) |

### Assumptions

```
Technical Assumptions:
1. XNNPACK provides sufficient CPU acceleration
2. 640×640 resolution optimal for inference speed
3. YOLOv8 confidence threshold 0.4 sufficient
4. Room database handles <1000 recipes efficiently
5. Device camera ≥8MP meets accuracy needs

User Assumptions:
1. Users have stable device storage
2. Camera is available on device
3. Users understand app permissions
4. Detection failure is acceptable (<40%)
5. Manual ingredient selection is fallback option

Business Assumptions:
1. Thesis project doesn't require monetization
2. User growth organic (no marketing)
3. MVP satisfies thesis requirements
4. No legal/compliance issues with recipes/images
5. No third-party API dependencies needed
```

---

## FUTURE ROADMAP

### Q1 2027: Social & Sharing

- [ ] User profiles
- [ ] Recipe sharing via social media
- [ ] Social comments & ratings
- [ ] Follow favorite recipe creators

### Q2 2027: Personalization

- [ ] User preferences (diet, allergies)
- [ ] Personalized recommendations
- [ ] Cooking history tracking
- [ ] Difficulty level selection

### Q3 2027: Smart Features

- [ ] Voice recipe search
- [ ] Step-by-step cooking assistant with timers
- [ ] Ingredient substitution engine
- [ ] Nutritional calculator

### Q4 2027: Advanced ML

- [ ] Fine-tuned model for better accuracy
- [ ] Multi-ingredient detection improvement
- [ ] Recipe generation (future ML)
- [ ] Food quality assessment

### 2028+: Expansion

- [ ] Multi-platform (Web, iOS)
- [ ] Meal planning integration
- [ ] Grocery delivery integration
- [ ] Smart kitchen device integration

---

## GLOSSARY

| Term | Definition |
|------|-----------|
| **YOLO** | You Only Look Once - real-time object detection algorithm |
| **TFLite** | TensorFlow Lite - ML framework for mobile |
| **Cosine Similarity** | Metric to measure similarity between vectors (0-1 range) |
| **Bounding Box** | Rectangle around detected object on screen |
| **Confidence Score** | Probability of correct detection (0-1, e.g., 92%) |
| **MVP** | Minimum Viable Product - core features only |
| **DAU** | Daily Active Users |
| **MAU** | Monthly Active Users |
| **CameraX** | Modern Android camera API |
| **Jetpack Compose** | Declarative UI framework for Android |
| **Room Database** | Android SQLite abstraction library |
| **DataStore** | Key-value storage replacement for SharedPreferences |
| **Hilt** | Dependency injection framework for Android |
| **Non-Maximum Suppression (NMS)** | Post-processing to remove duplicate detections |
| **Inference** | Running ML model to get predictions |
| **On-device ML** | ML processing on device (offline) |
| **Tensor** | Multi-dimensional array of numbers used in ML |
| **Frame Rate** | Number of images per second (FPS) |
| **Latency** | Time delay between input and output |
| **OKR** | Objectives & Key Results - goal-setting framework |

---

## DOCUMENT HISTORY

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | May 26, 2026 | Product Team | Initial PRD creation |

---

## APPROVAL & SIGNOFF

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Product Manager | - | - | - |
| Tech Lead | - | - | - |
| Project Manager | - | - | - |

---

**End of Document**

*For questions or clarifications, contact the product team.*

