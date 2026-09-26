package it.polimi.webeep.core.datastore

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SecureValueCodecTest {

    private lateinit var aead: Aead

    @Before
    fun setup() {
        AeadConfig.register()
        aead = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    @Test
    fun `sealed value opens with the same key`() {
        val sealed = SecureValueCodec.seal(aead, "ws_token", "abc123")

        assertFalse(sealed.contains("abc123"))
        assertEquals("abc123", SecureValueCodec.open(aead, "ws_token", sealed))
    }

    @Test
    fun `sealing is randomized`() {
        assertNotEquals(
            SecureValueCodec.seal(aead, "ws_token", "abc123"),
            SecureValueCodec.seal(aead, "ws_token", "abc123")
        )
    }

    @Test
    fun `value moved under another key is rejected`() {
        val sealed = SecureValueCodec.seal(aead, "full_name", "Mario Rossi")

        assertNull(SecureValueCodec.open(aead, "ws_token", sealed))
    }

    @Test
    fun `malformed or tampered values are rejected`() {
        val sealed = SecureValueCodec.seal(aead, "ws_token", "abc123")
        val tampered = sealed.dropLast(4) + if (sealed.endsWith("AAAA")) "BBBB" else "AAAA"

        assertNull(SecureValueCodec.open(aead, "ws_token", "not base64 !!"))
        assertNull(SecureValueCodec.open(aead, "ws_token", tampered))
    }

    @Test
    fun `value sealed with another keyset is rejected`() {
        val other = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val sealed = SecureValueCodec.seal(other, "ws_token", "abc123")

        assertNull(SecureValueCodec.open(aead, "ws_token", sealed))
    }
}
