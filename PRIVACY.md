# Launchly Privacy Notice

Last technical update: July 20, 2026. Maintainers should obtain legal review before treating this document as final legal wording.

Launchly is an unofficial client. It does not include automatic analytics, advertising SDKs, crash reporting, or telemetry.

## Account and authentication data

The sign-in screen loads Google’s HTTPS account pages in an isolated WebView and exchanges the resulting Google OAuth cookie for an AAS token through `android.clients.google.com`. Launchly stores the account email, display name when available, AAS token, and profile artwork URL when available.

The complete session is encrypted with AES-GCM. Its encryption key is non-exportable and held by Android Keystore; the encrypted session file is stored in `noBackupFilesDir`. If the key or ciphertext becomes unreadable, Launchly deletes the session and requires sign-in again. WebView cookies and state are cleared after every sign-in outcome. Signing out deletes the encrypted session.

This is not an official Google authentication integration. Using it may carry account or service-policy risk. Use an account only if you understand and accept that risk.

## Network connections

Launchly connects to:

- Google account and Android client endpoints for authentication, profile retrieval, purchase entitlement, and authenticated Minecraft delivery;
- the `minecraft-linux/mcpelauncher-versiondb` repository on GitHub for the compatible version catalog;
- links on `github.com/flarialmc/Launchly` only when you explicitly open documentation or source links.

Launchly does not operate an intermediary analytics or account server.

## Local data

Launchly stores managed-version metadata and download records in a Room database, preferences in DataStore, cached catalogs in app-private storage, and downloaded APK splits under the app-private `filesDir/versions` directory. APK files are checked for package name, requested version, split consistency, ABI compatibility, signatures, and signer trust before Android is asked to install them.

Android backup and device-transfer extraction are disabled for Launchly data. Installed Minecraft and its own app data are outside Launchly’s storage.

Version `0.2.0` performs one deliberate Launchly-only reset. It clears old Launchly credentials, cookies, preferences, database records, and downloads. It never uninstalls or modifies the installed Minecraft package during that reset.

## Diagnostics

Diagnostic export happens only after you choose **Export diagnostics** and select a destination. The JSON report includes app/device versions, device ABIs, installed Minecraft version, and download states or errors. It excludes authentication tokens, account email, display name, signer secrets, and WebView data. Launchly does not upload the report.

## Removal

Use **Sign out** to remove the authentication session. Deleting managed versions removes their Launchly-owned APK files. Uninstalling Launchly removes its private local data according to Android’s app-storage behavior; it does not uninstall Minecraft.
