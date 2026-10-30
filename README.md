# AI Debate Arena (Android APK source)

This is a build-ready Android Studio project. It is not a precompiled APK.
It builds a simple native Android app that runs a 10-round Gemini vs Groq debate
and then asks Gemini to judge the transcript.

## Build APK using GitHub Actions from a phone
1. Create a GitHub account and a new **private** repository.
2. Upload all files and folders from this ZIP to the repository root.
3. Open the repository's **Actions** tab.
4. Enable Actions if GitHub asks.
5. Select **Build Android APK** and tap **Run workflow** (or push to `main`).
6. Open the completed workflow run, scroll to **Artifacts**, and download
   `AI-Debate-Arena-APK`.
7. Extract the ZIP and install `app-debug.apk` on Android. Allow installation
   from this source when Android asks.

## API keys
Get a Gemini key at https://aistudio.google.com/apikey
Get a Groq key at https://console.groq.com/keys

Enter keys only into a build you trust. This sample sends requests directly
from the phone to the official APIs; keys are held in the app's in-memory
fields and are not intentionally written to disk. The APK is not signed for
Play Store distribution.

## Models
The sample currently uses `gemini-2.5-flash` and `openai/gpt-oss-20b`.
If a provider retires or restricts a model, update the model ID in
`MainActivity.kt` to a currently available model in your provider console.

## Notes
- 10 rounds means 10 Gemini answers + 10 Groq answers + 1 Gemini judge call.
- Provider free tiers are quota-limited and can change.
- Do not use this sample with confidential information.
