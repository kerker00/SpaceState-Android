# Compose appearance

The interface follows the [shared Apple/website appearance](https://github.com/kerker00/SpaceState/blob/dev/docs/DESIGN.md)
and [English/German terminology](https://github.com/kerker00/SpaceState/blob/dev/docs/TERMINOLOGY.md).
Compose and Material 3 retain Android navigation, input, touch feedback, search,
pull-to-refresh, menus and system font scaling.

## Theme roles

`ui/theme/Theme.kt` maps paper/slate surfaces and amber actions to Material color
roles. These are the fallback colors on Android 11 and when dynamic color is
disabled. On Android 12+, the existing default uses system wallpaper colors for
surfaces, text and actions. `SpaceStateTheme(dynamicColor = false)` selects the
brand palette for a preview; there is no new user preference.

| Role | Light fallback | Dark fallback |
| --- | --- | --- |
| Page | `#f4f3ee` | `#151a22` |
| Card | `#fbfaf7` | `#1e2530` |
| Text | `#252d38` | `#f4f3ee` |
| Supporting text | `#5e6570` | `#b7c0ce` |
| Action / restricted room | `#925400` | `#fac276` |
| Open | `#4f723b` | `#aad77c` |
| Closed | `#ad3d31` | `#ff9b8b` |

Semantic opening colors remain stable with wallpaper colors enabled; unknown uses
the active Material supporting-text role. `LocalSpaceStatusColors` follows the
theme's actual light/dark argument, including appearance overrides in previews.
Status is always stated in words as well as color; symbols are decorative when
the adjacent text already supplies their meaning. Restricted room labels retain
their distinct meaning and amber role.

## Components and fitting

`SpaceCard` uses a Material Surface with a 16dp radius, 18dp content inset and
outline. Screen padding is 16dp, card spacing 12dp, and content is centered within
a 720dp reading width. Clickable cards use a native Surface, button semantics and
at least a 48dp height. Selection adds both a checkmark/outline and a selected
semantic property. Native Material controls retain their normal touch targets.

Names, addresses, messages, status words and dates wrap without fixed text heights.
Room names and state labels stack vertically rather than competing at opposite
ends of one row. At font scales of 1.5 or above, the large decorative main status
symbol yields width to text. Settings scroll and the directory list keeps its
own scroll area below the labelled search field. Related noninteractive room and
status text is grouped for TalkBack; card section titles have heading semantics.

The screens consume the existing effective `status`, `displayRooms` and
`displayEntries`, so missing, expired or failed evidence remains unknown. Retained
details and room values have explicit historical labels. Original observation,
receipt and latest attempt remain separate, including for rooms. Error retry uses
the existing loader/refresh action. No polling, selection persistence, source
policy, subscriptions, routes or native-client behavior is changed here.

## Previews and device acceptance

`app/src/debug/java/net/grafixmafia/spacestate/ui/AppearancePreviews.kt` supplies
Android Studio previews for status, retained failures, selection and settings.
They cover DE/EN, light/dark, narrow screens, 2x fonts, long names/addresses and
system color roles. Fixtures and callbacks are inert: no ViewModels, Swift/JNI
client, settings writes or production requests. They are excluded from release
sources. Preview declarations and a successful build alone do not establish that
these layouts have been visually inspected on a device.

Use the native debug/test-bundle build described in the README. Then inspect the
previews in Android Studio and verify scrolling, wrapping, selection/menu focus,
keyboard search/back, TalkBack reading order, and light/dark wallpaper colors on
an emulator or device. Large-text and TalkBack acceptance require that separate
platform check. Existing JVM regressions cover the data lifecycle, not rendered
Compose layouts.

Material behavior references: [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults),
[semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics),
[Material 3 and dynamic color](https://developer.android.com/develop/ui/compose/designsystems/material3).
