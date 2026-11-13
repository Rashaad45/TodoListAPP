# ToDo — Android task list

A clean Material 3 to-do app written in Kotlin.

## Features

- **Saved on the device.** Tasks survive closing the app and restarting the phone.
- **Progress header.** A bar shows how many tasks are done out of the total.
- **Filters.** Switch between All, To do and Done, with a count on each.
- **Add, edit and delete.** Tap **New task** to add one. Tap any task to rename or delete it.
- **Swipe to delete, with Undo.** Swipe a task left or right; tap Undo in the message bar to bring it back.
- **Clear completed.** Removes all finished tasks after a confirmation, and can also be undone.
- **Light and dark mode.** Both follow the phone's system setting.

## Get the APK without Android Studio

Every push to GitHub builds the app automatically with GitHub Actions.

1. Open the repo on GitHub and go to **Actions** → **Android build**.
2. Open the latest green run.
3. Under **Artifacts**, download **ToDo-debug-apk**.
4. Unzip it, copy `app-debug.apk` to your phone, and open it. Android will ask you to allow installs from that source the first time.

## Build locally with VS Code (no Android Studio)

You need:

- **JDK 17** (for example Eclipse Temurin 17)
- The **Android SDK command-line tools**, from https://developer.android.com/studio#command-line-tools-only

One-time SDK setup (Windows PowerShell). This assumes you unzipped the tools to `C:\Android\cmdline-tools\latest`:

```powershell
setx ANDROID_HOME C:\Android
C:\Android\cmdline-tools\latest\bin\sdkmanager.bat "platform-tools" "platforms;android-35" "build-tools;35.0.0"
C:\Android\cmdline-tools\latest\bin\sdkmanager.bat --licenses
```

Then, in VS Code's terminal, from the project folder:

```powershell
.\gradlew.bat testDebugUnitTest     # run unit tests
.\gradlew.bat assembleDebug         # build -> app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat installDebug          # install on a USB-connected phone (USB debugging on)
```

For editing Kotlin in VS Code, the **Kotlin** and **Gradle for Java** extensions are enough.

## Project layout

```
app/src/main/java/com/example/todo/
├── MainActivity.kt          screen logic: list, filters, dialogs, swipe, undo
├── data/Task.kt             Task model + pure sorting/filter logic (unit-tested)
├── data/TaskRepository.kt   saves tasks as JSON in SharedPreferences
└── ui/TaskAdapter.kt        RecyclerView ListAdapter with DiffUtil
```
