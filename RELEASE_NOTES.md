# v1.2.0

- Extra notes: add single Markdown files from anywhere on your device, one at a time, and they
  show on the widget next to the notes from your folder. Settings → Extra Notes → Add File
- Removing an extra note, by swiping either way or with its remove button, only takes it off the
  widget. The file stays where it is
- A file that is already in your notes folder, or is not a `.md` file, is refused with a message
- Play build only: after five days the app may ask Google Play to show its rating dialog, at most
  every 90 days, and settings have a "Rate on Google Play" link. The foss build has neither

Two builds: `foss` has no `INTERNET` permission, `play` adds analytics and crash reporting that can
be switched off. Take `foss` unless you want to send crash reports.

Android 11+. GPL-3.0, as is, without warranty.
