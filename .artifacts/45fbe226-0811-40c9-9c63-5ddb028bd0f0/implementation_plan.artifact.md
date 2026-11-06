# UI Refinement: Header Blur and Hotbar Transparency

This plan details the changes to `MainActivity.kt` to refine the header and hotbar (bottom navigation bar) as requested.

## Proposed Changes

### [MainActivity.kt](file:///C:/Users/Hoju_/essential-mapper/app/src/main/java/com/aiden/essentialmapper/MainActivity.kt)

#### [MODIFY] Header Blur Logic
Implement a soft gradient blur at the bottom of the `TopAppBar` in all main screens (`ConfigScreen`, `EventsScreen`, `MacrosScreen`, and `GamesScreen`). This will create a smooth transition between the header and the scrollable content.
- Set `TopAppBar` container color to `Transparent`.
- Add `drawBehind` modifier to draw a solid black background for the header and a vertical gradient from black to transparent starting from the bottom edge of the header.

#### [MODIFY] Hotbar (Bottom Navigation) Appearance
- Make the `PillBottomNavigation` background semi-transparent grey (`0x662B2B2B`) to give it a floating, non-attached look.
- In `PillNavItem`, remove the ripple effect (indication) when clicking by setting `indication = null` and providing a custom `MutableInteractionSource`.
- Ensure the selected item remains dark grey (`0xFF1A1A1A`) as it is currently.

## Verification Plan

### Manual Verification
- Deploy the app to a device or emulator.
- **Header**: Verify that the header has a soft black gradient at the bottom that blends into the content as you scroll.
- **Hotbar**:
    - Verify that the bottom bar background is semi-transparent.
    - Verify that clicking on items does not show the grey ripple effect.
    - Verify that the selected item still has its dark grey background.
