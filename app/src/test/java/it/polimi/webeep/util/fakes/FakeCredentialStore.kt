package it.polimi.webeep.util.fakes

import it.polimi.webeep.core.datastore.CredentialStore

/** In-memory [CredentialStore]; [failWrites] simulates a Keystore that cannot persist. */
internal class FakeCredentialStore(initial: Map<String, String> = emptyMap()) : CredentialStore {
    var content: Map<String, String> = initial
        private set
    var failWrites = false
    var reads = 0
        private set

    override fun readAll(): Map<String, String> {
        reads++
        return content
    }

    override fun replaceAll(values: Map<String, String>) {
        if (failWrites) throw java.security.GeneralSecurityException("Secure storage unavailable")
        content = values
    }
}
