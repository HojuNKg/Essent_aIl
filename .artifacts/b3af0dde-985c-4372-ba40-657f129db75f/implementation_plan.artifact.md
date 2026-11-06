# Implementation Plan - Notification Permission for Accessibility Alert

Check for `POST_NOTIFICATIONS` permission before showing the accessibility service warning notification. If the permission is missing, request it from the user.

## Proposed Changes

### [MainActivity](file:///C:/Users/Hoju_/essential-mapper/app/src/main/java/com/aiden/essentialmapper/MainActivity.kt)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Hoju_/essential-mapper/app/src/main/java/com/aiden/essentialmapper/MainActivity.kt)
- Add necessary imports for permission checking (`Manifest`, `PackageManager`, `ContextCompat`, `Build`).
- In `MainContent` Composable:
    - Add `rememberLauncherForActivityResult` for requesting notification permission.
    - Update the `LaunchedEffect` loop to check for notification permission on Android 13+ before calling `showAccessibilityNotification`.
    - If permission is missing, launch the permission requester.

## Verification Plan

### Manual Verification
1.  Clear app data/reinstall to reset permissions.
2.  Disable the Accessibility Service for Essent.aI.
3.  Open the app.
4.  Observe that a notification permission dialog appears (on Android 13+).
5.  Grant permission.
6.  Observe that the accessibility warning notification appears.
7.  Revoke notification permission and observe if it asks again (respecting system behavior for "don't ask again").
