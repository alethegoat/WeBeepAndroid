# Security policy

## Reporting a vulnerability

Please do not open a public issue for a security problem. Use GitHub's private vulnerability reporting instead: open the repository's **Security** tab and choose **Report a vulnerability**. This creates a private security advisory that only the maintainer can see.

Include the app version, Android version, steps to reproduce and the impact you expect. Never include a real token, `wstoken` value or other person's data in a report.

This is a student project. Replies and fixes are best effort, with no guaranteed timeline. Please allow time for a fix before disclosing the issue publicly.

## Supported versions

Only the latest release receives security fixes.

## Scope

In scope:

- Token handling (storage, logging, exposure in URLs, routes or intents)
- The SSO callback (`webeepandroid://` and `moodlemobile://` deep links)
- Local storage (secure store, Room, downloads, caches, backups)
- Network behavior (TLS, interceptors, redirects)

Out of scope:

- Vulnerabilities in the WeBeep / Moodle servers or their SSO. They belong to Politecnico di Milano and should be reported to the university.
- Issues that need a rooted device, a malicious build or physical access to an unlocked phone.
