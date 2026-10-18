<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Glitch OS

Glitch OS is a native Android creator toolkit for video generation, connected YouTube channel management, keyboard utilities, and device privacy tools.

The app uses its native video pipeline and device storage for projects, Google OAuth/YouTube Data API for connected channel data, and configured AI/media providers for generation. Live channel insights require a signed-in account with the requested YouTube scopes. If a provider or asset is unavailable, the app should show the failure rather than substitute sample analytics or unrelated media.

View your app in AI Studio: https://ai.studio/apps/c0302a8d-5c7a-4cbf-bd1f-4f4a4ec2be0f

## Run locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio), JDK 17, and the Android SDK versions required by the Gradle configuration.

1. Open this directory in Android Studio and allow Gradle to sync.
2. Configure Firebase/Google OAuth for the application ID in `app/build.gradle.kts`; do not commit private credentials.
3. Create a project `.env` from `.env.example` and add only the provider keys the enabled features need.
4. Run the `app` configuration on an emulator or physical device. Microphone, VPN, keyboard, accessibility, and YouTube features require their relevant user permissions.
5. Before publishing, replace the debug signing configuration with a protected release keystore and secrets supplied through the environment.

## Current implementation inventory and roadmap

See [docs/GLITCH_OS_AUDIT_AND_ROADMAP.md](docs/GLITCH_OS_AUDIT_AND_ROADMAP.md) for the app tool catalog, verified fixes, known limitations, and the phased plan for 100 advancements.
