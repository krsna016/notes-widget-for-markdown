# Changelog

All notable public changes will be documented in this file.

## v1.3.1 - 2026-10-21

- Fast notes: choose bullet or checkbox right in the fast note dialog; it is the same setting as in settings.
- Fixed: the fast note and new note dialogs could bring the settings screen to the front when settings were left open in the background.
- Fixed: a note whose first heading repeats its file name showed the title twice on the widget card.
- Play build: the reporting question now appears as soon as you return to settings after adding the widget, and switching reporting on or off in settings counts as the answer.
- Settings: optional sections are marked as optional, and the fast note target now comes right after the notes folder.
- The "What's new" dialog also appears for people updating straight to 1.3.1.

## v1.3.0 - 2026-10-20

- Widget background: pick a colour (System, card pastels, Graphite, Forest or a custom hex) and its transparency. The default is now 85 % opacity; buttons and chips switch to readable colours automatically.
- Fast notes can be added as checkboxes as well as bullets. Without a target file the fast note dialog asks which file to use instead of showing an error, also when the saved file was moved or deleted.
- A small rating chip appears on the widget for two days at a time, at most three times, and never while sync needs attention. It opens a short dialog with links to rate the app or suggest an idea, and can be turned off in settings.
- Play build: the Google Play rating dialog now only appears after a quick note is saved, never when settings open.
- Settings can ask once what would make the app better: thumbs up or down and a short text. The Play build stores the answer anonymously in Firebase Firestore (EU); the foss build opens a GitHub discussion instead.
- After an update, a one-time "What's new" dialog lists the changes. All automatic dialogs share one rule: at most one per visit, at least three days apart.
- Play build: the reporting question now comes after setup instead of on first launch, with shorter text. Nothing is collected before the answer, and crash reports have file paths removed before they are stored.
- The app is now also in Russian, Brazilian Portuguese, Italian and Ukrainian, and on Android 13+ settings link to the per-app language setting.
- Usage events for features and settings (action names and chosen options only), sent only with consent.
- Fixed: the settings screen kept redrawing because of the moving logo, which wasted battery; the logo now stays still.
- Fixed: the fast note dialog could freeze while the keyboard opened.

## v1.2.1 - 2026-10-12

- Fixed: after picking a different notes folder, the widget kept showing the old folder until a manual refresh or the next periodic update. It now redraws right away. Reported on Reddit.

## v1.2.0 - 2026-10-12

- Extra notes: single Markdown files from anywhere on the device can be added to the widget one at a time, next to the notes from the folder. Removing one, by swiping either way or with its remove button, only takes it off the widget and never deletes the file. A file that is already in the notes folder, or is not a `.md` file, is refused with a message.
- A file that was added and later moved or deleted is skipped on the widget and marked as not available in settings.
- Play build: five days after it first runs, the app may ask Google Play to show its rating dialog after a quick note is saved or when settings open, at most every 90 days. Settings also have a "Rate on Google Play" link. The foss build has neither.
- Fixed: once a single file had been granted, the sync chip could report the git repo as unreadable.

## v1.1.0 - 2026-09-04

- New notes are created directly in the selected folder instead of being asked of Obsidian — a note could previously land in the vault root when the vault couldn't be located, reported on Reddit and reproduced by granting access to a subfolder only.
- Choose how a tapped note opens: Obsidian, the device's default Markdown app, or ask every time.
- Fixed: deleting a note that no longer existed kept failing with the same error on every retry instead of just disappearing from the list.
- Fixed: swiping a note to delete it could remove the wrong file after the list changed.
- Fixed: the widget could stop redrawing after its first refresh until something restarted the app process.
- Tagged releases now publish as drafts, so a signed build can be installed and checked before anyone else sees it.

## v1.0.0 - 2026-09-02

Initial public MVP.

- Local-first Android widget for selected Markdown folders.
- Markdown preview for headings, links, lists, task checkboxes, and strikethrough.
- Per-note card height, text size, color, and manual order.
- Fast note append flow for a selected Markdown file.
- Obsidian URI integration for opening existing notes and creating new notes.
- Bottom action bar in the widget: add note and fast note buttons no longer overlap the note cards.
- Git sync indicator in the bottom bar, read from the vault's `.git` metadata: last pull/commit, stuck merge or rebase, and unpushed work. Client-agnostic (CLI git, obsidian-git, Git Sync); tap opens settings with the full status. Can be switched off for non-git sync setups.
- Second widget: a compact list of note names, sharing the data, actions and sync chip with the card widget.
- Automatic refresh when the notes folder changes, instead of waiting for the periodic update.
- Note management in settings: rows collapse to one line, notes can be deleted (with confirmation) and reordered by dragging.
- Notes are opened in the correct Obsidian vault: the vault is detected from `.obsidian` and the path is resolved relative to its root. Previously a subfolder name was passed as the vault, so Obsidian silently opened the last note instead.
- Newly added notes appear at the top of the widget rather than below the fold.
- GitHub Actions release pipeline for signed APK releases.
