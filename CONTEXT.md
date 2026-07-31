# Context

A glossary of domain concepts used across the Readium Kotlin Toolkit. It describes the problem space, not the implementation.

## Copy Protection

The set of restrictions a Content Protection technology (e.g. Readium LCP) places on extracting text from a publication. Copy protection does not forbid selecting text — it governs what may leave the publication, typically through the clipboard.

## Copy Allowance

A budget of characters that a license grants for copying, e.g. an LCP license may allow copying up to 10,000 characters. The allowance is consumed all-or-nothing: a copy either fits in the remaining budget and consumes it entirely for that text, or is denied without consuming anything. A license may also grant an unlimited allowance.

## Counted Copy

A copy operation which consumes the copy allowance before writing to the clipboard. If the allowance is insufficient, the copy is denied and the clipboard is left untouched. Counted copies are performed by the navigators, either by intercepting native copy events (system selection menu, Ctrl+C) or through an explicit API for reading apps.

## User Rights

The permissions a Content Protection grants the user on a publication — copying and printing — along with the operations consuming them. Rights are managed by the publication's Content Protection Service; an unprotected publication has unrestricted rights.

## Content Protection Service

A publication service providing information about a publication's protection: whether access is restricted, the credentials used to unlock it, and its User Rights. Reading apps and navigators use it without knowing the underlying protection technology.

## Selection ActionMode

The Android contextual menu shown over selected text. By default the system provides its items (Copy, Share, Select all, Web Search, Translate…). A reading app may replace it with a custom callback, which discards all system items — including Copy — and makes the app responsible for routing its own Copy item through a counted copy.
