PantryPal
- Repository: https://github.com/KosisoObiabumuo/PantryPal

PantryPal is an Android app that keeps track of the food in your kitchen: the fridge, freezer, pantry and cupboard. It is built around a playful, Crossy Road style kitchen where every storage opens like the real thing and every item has its own icon.

Features
1. Cartoon kitchen home screen with a fridge-freezer, wall cupboard and pantry. Each door shows how many items are inside, and a banner warns when food is running low.
2. Storages that open like the real thing. The fridge door swings on its left hinge, the cupboard on its right, the pantry's double doors split apart and the freezer lid lifts up. Items then pop onto the shelves one by one, and the doors close with a bounce on the way out.
3. An icon for every item, picked from keywords in its name (milk, bread, chicken, pasta and many more). Niche items fall back to a generic icon for their storage.
4. Item sheet with quantity controls, expiry status, allergens, and quick actions. Tapping an item makes it hop before the sheet opens.
5. Log food form with a live icon preview while typing, input validation, an expiry date picker, priority items and allergen tags.
6. Sorting by expiry date, name or quantity.
7. Low-stock tracking with priority items (like milk and bread) listed first.
8. Undo when removing an item.

Explicit intents

1. Kitchen to storage screen: opens the storage screen with the chosen storage type passed as an extra.
2. Kitchen or storage screen to log food form (with result): the form returns the name of the item added, which is shown in a Snackbar.

Implicit intents

1. Share shopping list (ACTION_SEND): sends the low-stock list to any app that accepts text, such as WhatsApp, Gmail or Messages.
2. Find recipes (ACTION_VIEW): opens a recipe search for the item in the browser.
3. Remind me before it expires (ACTION_INSERT): creates an all-day calendar event the day before the expiry date.

Built with
1. Kotlin
2. Android Views with XML layouts
3. Material Components 3
4. RecyclerView with a grid layout
5. "java.time" for expiry dates
6. Minimum SDK 26 (Android 8.0)
7. 
Project structure
app/src/main/java/com/kosi/pantrypal/
- MainActivity.kt        Kitchen home screen
- StorageActivity.kt     Shelves for one storage, door animation, item sheet
- AddItemActivity.kt     Log food form
- FoodAdapter.kt         Shows items on the shelves
- data/FoodRepository.kt Single source of food data
- model/FoodItem.kt      One food item
- model/StorageType.kt   Fridge, freezer, pantry, cupboard and their look
- model/FoodIcons.kt     Picks an icon for each item

Design decisions
- One reusable storage screen. All four storages use a single StorageActivity, chosen by an explicit intent extra, instead of four near-identical screens.
- Repository layer. Every screen reads and writes food through FoodRepository, so the storage behind it can change without touching the screens.
- Swappable font. The game font is stored as game_font.ttf rather than under its own name, so it can be replaced without editing any layout.

Bugs found and fixed
- Opening the cupboard crashed the app. Cereal was the only sample item with no expiry date, and daysUntilExpiry() used expiryDate.let instead of expiryDate?.let, so a missing date reached ChronoUnit.DAYS.between. Kotlin did not flag it because ChronoUnit is a Java method. Found by reading the crash in Logcat and fixed with a null-safe call.

Build and run
- Clone the repository and open it in Android Studio.
- Let Gradle sync.
- Run on an emulator or phone with Google Play (API 34 or newer recommended).

Planned
- Barcode scanning with the camera to log items quickly
- Location reminders when you arrive at a supermarket
- Meal ideas based on what you have, using up food that expires soonest
- On-device database so items are saved between sessions