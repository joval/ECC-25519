/*
 * Copyright (C) 2014 Ralf Wondratschek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vrallev.android.ecc;

import android.test.AndroidTestCase;

import org.assertj.core.api.Assertions;

import java.security.SecureRandom;

/**
 * @author Ralf Wondratschek
 */
public class TestEcc255Helper extends AndroidTestCase {

    public void testEd25519() throws Exception {

        // instances from the same seed must contain the keys
        final byte[] seed = new byte[32];
        Ecc25519Helper helper1 = new Ecc25519Helper(KeyHolder.createPrivateKey(seed));
        Ecc25519Helper helper2 = new Ecc25519Helper(KeyHolder.createPrivateKey(seed));

        Assertions.assertThat(helper1.getKeyHolder().getPrivateKey()).isEqualTo(helper2.getKeyHolder().getPrivateKey());
        Assertions.assertThat(helper1.getKeyHolder().getPublicKeyDiffieHellman()).isEqualTo(helper2.getKeyHolder().getPublicKeyDiffieHellman());
        Assertions.assertThat(helper1.getKeyHolder().getPublicKeySignature()).isEqualTo(helper2.getKeyHolder().getPublicKeySignature());

        helper2 = new Ecc25519Helper(helper2.getKeyHolder().getPrivateKey());

        Assertions.assertThat(helper1.getKeyHolder().getPrivateKey()).isEqualTo(helper2.getKeyHolder().getPrivateKey());
        Assertions.assertThat(helper1.getKeyHolder().getPublicKeyDiffieHellman()).isEqualTo(helper2.getKeyHolder().getPublicKeyDiffieHellman());
        Assertions.assertThat(helper1.getKeyHolder().getPublicKeySignature()).isEqualTo(helper2.getKeyHolder().getPublicKeySignature());

        seed[0] = 1;
        helper2 = new Ecc25519Helper(KeyHolder.createPrivateKey(seed));

        Assertions.assertThat(helper1.getKeyHolder().getPrivateKey()).isNotEqualTo(helper2.getKeyHolder().getPrivateKey());
        Assertions.assertThat(helper1.getKeyHolder().getPublicKeyDiffieHellman()).isNotEqualTo(helper2.getKeyHolder().getPublicKeyDiffieHellman());
        Assertions.assertThat(helper1.getKeyHolder().getPublicKeySignature()).isNotEqualTo(helper2.getKeyHolder().getPublicKeySignature());

        // diffie hellman
        Ecc25519Helper temp = new Ecc25519Helper();
        byte[] sharedSecret1 = temp.diffieHellman(helper1.getKeyHolder().getPrivateKey(), helper2.getKeyHolder().getPublicKeyDiffieHellman());
        byte[] sharedSecret2 = temp.diffieHellman(helper2.getKeyHolder().getPrivateKey(), helper1.getKeyHolder().getPublicKeyDiffieHellman());
        Assertions.assertThat(sharedSecret1).isNotNull().isEqualTo(sharedSecret2);

        sharedSecret2 = helper1.diffieHellman();
        Assertions.assertThat(sharedSecret1).isNotNull().isNotEqualTo(sharedSecret2);

        // signature
        SecureRandom random = new SecureRandom();
        for (int i = 0; i <= 14; i++) {
            int length = (int) Math.pow(2, i);
            byte[] message = new byte[length];
            random.nextBytes(message);

            byte[] signature = helper1.sign(message);
            assertTrue(helper1.isValidSignature(message, signature));
            assertFalse(helper2.isValidSignature(message, signature));

            signature = helper2.sign(message);
            assertFalse(helper1.isValidSignature(message, signature));
            assertTrue(helper2.isValidSignature(message, signature));
        }
    }

    public void testKeyHolderCTORIsSideEffectFree() {
        final byte[] pk1 = KeyHolder.createPrivateKey("hello".getBytes());
        final byte[] pk2 = pk1.clone();
        new KeyHolder(pk2);
        Assertions.assertThat(pk1).isEqualTo(pk2);
    }

    public void testKeyHolderGetPrivateKeyReturnsUnmodifiedPrivateKey() {
        final byte[] pk1 = KeyHolder.createPrivateKey("hello".getBytes());
        final byte[] pk2 = new KeyHolder(pk1.clone()).getPrivateKeyUnclamped();
        Assertions.assertThat(pk1).isEqualTo(pk2);
    }

    public void testEcc25519HelperSignClampsParameter() {
        // if pk1 is not clamped, signature verification will fail.
        // the .clone() calls are to make sure no other side effects affect the outcome.
        final byte[] pk1 = KeyHolder.createPrivateKey("hello".getBytes());

        byte[] sig = new Ecc25519Helper().sign("message".getBytes(), pk1);

        assertTrue((new Ecc25519Helper()).isValidSignature("message".getBytes(), sig, new KeyHolder(pk1.clone()).getPublicKeySignature()));
    }

    public void testEcc25519HelperSignIsSideEffectFreeOnPrivateKeyParameter() {
        // ensure that clamping of pk2 is side-effect free.
        // the .clone() calls are to make sure no other side effects affect the outcome.
        final byte[] pk1 = KeyHolder.createPrivateKey("hello".getBytes());
        final byte[] pk2 = pk1.clone();

        byte[] sig = new Ecc25519Helper().sign("message".getBytes(), pk2);

        Assertions.assertThat(pk1).isEqualTo(pk2);
    }
}
