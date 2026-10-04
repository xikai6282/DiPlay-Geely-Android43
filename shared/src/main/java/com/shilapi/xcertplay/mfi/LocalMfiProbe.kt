package com.shilapi.xcertplay.mfi

import java.io.File
import java.math.BigInteger
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence

/** ADB/app_process probe: exercises Android's local crypto without starting a CarPlay session. */
object LocalMfiProbe {
    @JvmStatic fun main(arguments: Array<String>) {
        require(arguments.size == 1) { "Expected private identity directory" }
        var signatureCount = 0
        repeat(2) { startup ->
            val client = LocalMfiAuthenticationClient.load(File(arguments.single())) { signatureCount++ }
            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificates(client.readCertificate().inputStream()).single()
            repeat(3) { trial ->
                val digest = ByteArray(32).also(SecureRandom()::nextBytes)
                val raw = client.signChallenge(digest)
                check(raw.size == 64)
                val der = DERSequence(arrayOf(
                    ASN1Integer(BigInteger(1, raw.copyOfRange(0, 32))),
                    ASN1Integer(BigInteger(1, raw.copyOfRange(32, 64))),
                )).encoded
                val verifier = Signature.getInstance("NONEwithECDSA")
                verifier.initVerify(certificate.publicKey)
                verifier.update(digest)
                check(verifier.verify(der)) { "Signature verification failed" }
                digest[0] = (digest[0].toInt() xor 1).toByte()
                verifier.initVerify(certificate.publicKey)
                verifier.update(digest)
                check(!verifier.verify(der)) { "Changed challenge was incorrectly accepted" }
                println("offline-probe startup=${startup + 1} trial=${trial + 1} signatureBytes=64 verifies=true changedChallengeRejected=true")
            }
        }
        println("offline-probe PASS localSignatures=$signatureCount remoteRequests=0 (provider has no network operations)")
    }
}
