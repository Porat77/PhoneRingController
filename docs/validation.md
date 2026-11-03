# Validation and release checklist

## Automated checks

- Node syntax and scheduling tests: run with npm run check and npm test.
- Java pure engine: test/RuleEngineCheck.java runs without Android dependencies.
- Android CI: unit tests, assembleDebug, lintDebug; download APK only from a successful run.
- CI results, rather than source presence, establish whether an APK builds.

## Required device tests — NOT YET RUN

Use a Samsung phone and an Android emulator/device with API 29 and API 35.

- Fresh install starts disabled with no rules and no permissions.
- Permission denial, grant, revocation and return from system Settings.
- Create rule, edit, delete, relaunch, and verify persisted state.
- Incoming real call before, within, and at end of a quiet interval.
- Notifications are suppressed; alarms and media remain allowed.
- Daytime, midnight, Saturday-to-Sunday, overlapping rules.
- Pause exactly one hour, cancel pause, reboot during pause.
- Disable global scheduling during quiet hours; own rule becomes inactive.
- Manual DND from another source remains in effect when this app stops quiet.
- User disables/removes own system rule; UI reports disconnected/disabled.
- Screen off, Doze, battery saver; exact access allowed and denied.
- Reboot, app update, time/time-zone changes, DST spring gap and fall repeat.
- Force-stop then reopen: explain that force-stop interrupts automation.
- Update Android System WebView; unsupported message listener shows update guidance.
- Accessibility: TalkBack, large text, RTL, dialog focus, touch targets.
- Inspect requested permissions and verify no network/personal-data transfer.

Do not publish as production-ready until these are recorded with device model,
OS, time, expected/actual behavior, and any OEM-specific restrictions.
