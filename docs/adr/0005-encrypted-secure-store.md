# 0005. Tokens in an encrypted Tink `SecureStore`

Status: Accepted

## Context

The Moodle `wstoken` grants access to the user's account. It was first kept in `EncryptedSharedPreferences` (Jetpack Security), which is deprecated. The SSO flow also returns a `private_token` that can open a full web session, and the profile call returns the user's full name. A previous audit (`docs/AUDIT-2026-09.md`) found the private token and the name were being persisted.

## Decision

- `core/datastore/SecureStore` stores credentials. Values are sealed with Tink AES-256-GCM, using the key name as associated data, so ciphertexts cannot be swapped between keys. The Tink keyset is wrapped by an Android Keystore master key. Ciphertexts are in a DataStore file under `noBackupFilesDir`.
- If Keystore is unusable, even after a reset, the store refuses writes. There is no cleartext fallback (`createAead` rejects a keyset not protected by Keystore).
- `TokenManager` keeps an in-memory copy and exposes it through the `TokenProvider` interface. It stores only the token, the user id and short-lived pending SSO passports.
- `EncryptedSharedPreferences` is used only in `LegacyEncryptedPrefsMigration`, to import and delete the old file once. It is marked for removal together with the `security-crypto` dependency.
- The SSO `private_token` is discarded in `LoginUseCase.execute` and never stored.
- The full name is not stored. It is read from `core_webservice_get_site_info` when needed and kept in memory.
- The store is excluded from backup and device transfer.

## Consequences

- No dependency on a deprecated API for new data.
- A broken keyset (for example after a restore) wipes the secure store and the user logs in again.
- `TokenManager` needs a real Keystore, so its test is an instrumented test (see ADR 0006).
- Existing installs keep working through the legacy migration until it is removed.
- `PRIVACY.md` still describes the older storage and says the name is stored; it needs the same update as the README.
