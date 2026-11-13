# 0005. Tokens in an encrypted Tink `SecureStore`

Status: Accepted

## Context

The Moodle `wstoken` grants access to the user's account. It was first kept in `EncryptedSharedPreferences` (Jetpack Security), which is deprecated. The SSO flow also returns a `private_token` that can open a full web session, and the profile call returns the user's full name. A previous audit (`docs/AUDIT-2026-09.md`) found the private token and the name were being persisted.

## Decision

- `core/datastore/SecureStore` stores credentials. Values are sealed with Tink AES-256-GCM, using the key name as associated data, so ciphertexts cannot be swapped between keys. The Tink keyset is wrapped by an Android Keystore master key. Ciphertexts are in a DataStore file under `noBackupFilesDir`.
- If Keystore is unusable, even after a reset, the store refuses writes. There is no cleartext fallback (`createAead` rejects a keyset not protected by Keystore).
- `TokenManager` keeps an in-memory copy and exposes it through the `TokenProvider` interface. It stores only the token, the user id and short-lived pending SSO passports.
- `EncryptedSharedPreferences` is no longer used (see the 2026-11-13 update below); it only appears in this history.
- The SSO `private_token` is discarded in `LoginUseCase.execute` and never stored.
- The full name is not stored. It is read from `core_webservice_get_site_info` when needed and kept in memory.
- The store is excluded from backup and device transfer.

## Consequences

- No dependency on a deprecated API for new data.
- A broken keyset (for example after a restore) wipes the secure store and the user logs in again.
- `TokenManager` needs a real Keystore, so its test is an instrumented test (see ADR 0006).
- Installs from before 1.1.0 were migrated by a one-shot import that has since been removed (see the update below).
- `PRIVACY.md` still describes the older storage and says the name is stored; it needs the same update as the README.

## Update 2026-11-13

The legacy migration and the `security-crypto` dependency were removed after v1.1.0 shipped. The old session is no longer imported, so users upgrading from a version before 1.1.0 sign in again. `LegacySecurePrefsCleanup` still deletes the old preferences file and its Keystore master key. The decision above is otherwise unchanged.
