# Visual and UX Improvements Plan

This plan outlines the visual and user experience improvements for the Essent.aI application, focusing on header consistency, events page layout, disabled item styling, navigation, and permission alerts.

## User Review Required

> [!IMPORTANT]
> The "dark white filter" for disabled event items will be implemented by lowering the overall alpha and using a darker gray color for text and icons to ensure a consistent "dimmed" look across all elements (text, emojis, and app icons).

## Proposed Changes

### UI Components & Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Hoju_/essential-mapper/app/src/main/java/com/aiden/essentialmapper/MainActivity.kt)

- **Header Consistency:**
    - Update `EventsScreen`, `PlaceholderScreen`, and `SettingsScreen` to use a consistent `TopAppBar` style similar to `ConfigScreen`.
    - Apply `TypewriterTitle()` or a similar styled title to all top-level screens.
- **Events FAB:**
    - Move the "+" (Add) button from the `TopAppBar` to a `FloatingActionButton` in `EventsScreen`.
- **Disabled Event Styling:**
    - Update `StripeEventItem` and `RectangleEventItem` to apply a more pronounced "dimmed" effect when disabled. This will include changing text color to a darker shade and ensuring the entire row feels "inactive".
- **Settings Back Gesture:**
    - Add `BackHandler` to `SettingsScreen` to allow exiting via system back gesture.
- **Accessibility Permission Check & Notification:**
    - Implement a check for Accessibility Service permission on app start.
    - Create a notification channel and send a notification if the permission is missing, prompting the user to enable it.

### Service Logic

#### [MODIFY] [EssentialKeyService.kt](file:///C:/Users/Hoju_/essential-mapper/app/src/main/java/com/aiden/essentialmapper/EssentialKeyService.kt)
- Ensure the service can handle being triggered even if the notification was clicked (though the notification will just open the app or settings).

## Verification Plan

### Automated Tests
- No specific automated tests requested, but I will verify the build success.

### Manual Verification
- **Visuals:** Run the app and check that all 4 main screens have the same header style.
- **FAB:** Navigate to the Events screen and verify the "+" button is now a FAB at the bottom right.
- **Disabled Filter:** Disable an event and verify that the whole item (text, emoji, icon) looks dimmed.
- **Back Gesture:** Open Settings and try the system back gesture to go back to the Config screen.
- **Permission Notification:** Revoke accessibility permission, restart the app, and verify that a notification appears.
