package com.example.passmanager;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class used for various cryptographic/random operations.
 */
public class CryptoHelper {
    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 16;
    public static final int KEY_LENGTH = 256;
    public static final String PBE_ALGORITHM = "PBEwithSHA256AND256BITAES-CBC-BC";
    public static final int PBE_ITERATIONS = 126;
    public static final String KEY_SPEC_ALGORITHM = "AES";
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";

    public static final String ALPHA_LOWER = "abcdefghijklmnopqrstuvwxyz";
    public static final String ALPHA_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NUMERIC = "0123456789";
    public static final String SPECIAL = " !#$%&'()*+,-./:;<=>?@[]^_`{|}~"; // missing " and \

    public static final int PASS_MAX_STRONGNESS = 4;

    /**
     * Generate a random salt for password-based encryption.
     *
     * @return the salt bytes.
     */
    public static byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        return salt;
    }

    /**
     * Generate a random Initialisation Vector for encryption/decryption.
     *
     * @return the IV bytes.
     */
    public static byte[] generateIv() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        return iv;
    }

    /**
     * Use password-based encryption to generate a key for symmetric encryption. The plaintext
     * password array is cleared from memory after creating the key.
     *
     * @param password Plaintext password used in key generation.
     * @param salt Random salt used in key generation.
     * @return The symmetric key.
     */
    public static SecretKey createPbeKey(char[] password, byte[] salt) {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt,
                    PBE_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            SecretKey key = new SecretKeySpec(factory.generateSecret(pbeKeySpec).getEncoded(),
                    KEY_SPEC_ALGORITHM);
            Arrays.fill(password, (char) 0);

            return key;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt a text using a symmetric-key algorithm. The text array is cleared from memory
     * after the encryption.
     *
     * @return The encrypted text encoded with Base64.
     */
    public static String encrypt(SecretKey key, char[] text) {
        try {
            byte[] iv = generateIv();
            String encodedIv = encode(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

            ByteBuffer buffer =
                    StandardCharsets.UTF_8.encode(CharBuffer.wrap(text));
            byte[] textBytes = new byte[buffer.remaining()];
            buffer.get(textBytes);
            byte[] encryptedBytes = cipher.doFinal(textBytes);

            Arrays.fill(textBytes, (byte) 0);
            Arrays.fill(buffer.array(), (byte) 0);
            Arrays.fill(text, (char) 0);

            // Attach the Initialisation Vector to the beginning of the encrypted text.
            return encodedIv + encode(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt a text using a symmetric-key algorithm.
     *
     * @return The decrypted text, in a char array.
     */
    public static char[] decrypt(SecretKey key, String encrypted) {
        try {
            // Extract the Initialisation Vector from the beginning of the encrypted string.
            int encodedIvLength = encodingLength(IV_LENGTH);
            String encodedIv = encrypted.substring(0, encodedIvLength);
            byte[] iv = decode(encodedIv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            String encryptedText = encrypted.substring(encodedIvLength);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(decode(encryptedText));

            ByteBuffer buffer = ByteBuffer.wrap(decryptedBytes);
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
            char[] decryptedPass = new char[charBuffer.remaining()];
            charBuffer.get(decryptedPass);
            Arrays.fill(charBuffer.array(), (char) 0);

            return decryptedPass;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate a symmetric key and use AndroidKeyStore to securely store it.
     *
     * TODO: Make sure that the key is only stored while the application runs and that
     * other apps cannot access it.
     *
     * @param alias Used for identifying the key in the storage.
     * @return The symmetric key.
     */
    public static SecretKey generateKeyForKeyStore(String alias) {
        try {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_SPEC_ALGORITHM,
                    "AndroidKeyStore");
            final KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(KEY_LENGTH)
                    .setRandomizedEncryptionRequired(false) // Set to false, generate IV manually.
                    .build();
            keyGenerator.init(keySpec);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extract a key from AndroidKeyStore.
     *
     * @param alias Used for identifying the key in the storage.
     * @return The key.
     */
    public static SecretKey getKeyFromKeyStore(String alias) {
        // TODO: Maybe use a protection parameter?
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final KeyStore.SecretKeyEntry keyEntry =
                    (KeyStore.SecretKeyEntry) keyStore.getEntry("masterKey", null);
            return keyEntry.getSecretKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt the master password with a random key. The plaintext master password is cleared
     * from memory after the encryption.
     *
     * @param masterPass plaintext master password
     * @return The encrypted password encoded with Base64.
     */
    public static String encryptMasterPassword(char[] masterPass) {
        SecretKey key = generateKeyForKeyStore("masterKey");
        return encrypt(key, masterPass);
    }

    /**
     * Decrypt the master password with the securely stored key.
     *
     * @param encrypted master password
     * @return The decrypted master password.
     */
    public static char[] decryptMasterPassword(String encrypted) {
        final SecretKey key = getKeyFromKeyStore("masterKey");
        return decrypt(key, encrypted);
    }

    /**
     * Generate a strong random password matching certain criteria.
     *
     * TODO: Make sure that the generated password matches all criteria.
     *
     * @param passLen Length of the password.
     * @return The plaintext password.
     */
    public static String generatePassword(int passLen) {
        // Password characters to choose from.
        char[] passChrs = (ALPHA_LOWER + ALPHA_UPPER + NUMERIC + SPECIAL).toCharArray();

        // Generate passLen bytes.
        SecureRandom secureRandom = new SecureRandom();
        byte[] passBytes = new byte[passLen];
        secureRandom.nextBytes(passBytes);

        // Use each generated byte as an index in the password characters array.
        StringBuilder pass = new StringBuilder(passLen);
        for (byte passByte : passBytes) {
            int val = passByte & 0xFF;
            pass.append(passChrs[val % passChrs.length]);
        }

        return pass.toString();
    }

    /**
     * Calculate password strongness.
     *
     * @param pass The plaintext password.
     * @return The level of strongness.
     */
    public static int passwordStrongness(String pass) {
        int strongness = 0;

        if (pass.length() >= 12)
            strongness++;
        if (pass.chars().anyMatch(c -> ALPHA_UPPER.indexOf(c) != -1))
            strongness++;
        if (pass.chars().anyMatch(c -> NUMERIC.indexOf(c) != -1))
            strongness++;
        if (pass.chars().anyMatch(c -> SPECIAL.indexOf(c) != -1))
            strongness++;

        return strongness;
    }

    /**
     * Encode bytes to a string representation using Base64.
     *
     * @param bytes to be converted.
     * @return The string representation.
     */
    public static String encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Decode a string to the corresponding bytes using Base64.
     *
     * @param base64Str string to be converted.
     * @return The bytes after conversion.
     */
    public static byte[] decode(String base64Str) {
        return Base64.decode(base64Str, Base64.NO_WRAP);
    }

    /**
     * Calculate the length of a Base64 encoded string (with padding enabled).
     *
     * @param length number of bytes encoded.
     * @return number of characters in the encoding.
     */
    public static int encodingLength(int length) {
        return (int) (Math.ceil((double) length / 3) * 4);
    }
}
