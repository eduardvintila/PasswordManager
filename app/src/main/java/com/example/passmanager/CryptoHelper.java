package com.example.passmanager;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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


    // TODO: Try to avoid "String" as much as possible, use char[] (or StringBuilder) in order to
    // clear the plaintext passwords from memory.


    public static byte[] generateSalt() {
        // TODO: Maybe use an explicit seed?
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        return salt;
    }

    public static byte[] generateIv() {
        // TODO: Maybe use an explicit seed?
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        return iv;
    }

    public static SecretKey createPbeKey(String password, byte[] salt) {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt,
                    PBE_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            return new SecretKeySpec(factory.generateSecret(pbeKeySpec).getEncoded(),
                    KEY_SPEC_ALGORITHM);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encrypt(SecretKey key, String text) {
        try {
            byte[] iv = generateIv();
            String ivHex = bytesToHexString(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            return ivHex + bytesToHexString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(SecretKey key, String encrypted) {
        try {
            String ivHex = encrypted.substring(0, IV_LENGTH * 2);
            byte[] iv = hexStringToBytes(ivHex);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            String encryptedText = encrypted.substring(IV_LENGTH * 2);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(hexStringToBytes(encryptedText));

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public static String encryptMasterPassword(String masterPass) {
        SecretKey key = generateKeyForKeyStore("masterKey");
        return encrypt(key, masterPass);
    }

    public static String decryptMasterPassword(String encrypted) {
        final SecretKey key = getKeyFromKeyStore("masterKey");
        return decrypt(key, encrypted);
    }

    public static String generatePassword(int passLen) {
        char[] passChrs = (ALPHA_LOWER + ALPHA_UPPER + NUMERIC + SPECIAL).toCharArray();

        SecureRandom secureRandom = new SecureRandom();
        byte[] passBytes = new byte[passLen];
        secureRandom.nextBytes(passBytes);

        StringBuilder pass = new StringBuilder(passLen);
        for (byte passByte : passBytes) {
            int val = passByte & 0xFF;
            pass.append(passChrs[val % passChrs.length]);
        }

        return pass.toString();
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            int val = aByte & 0xFF;
            if (val < 16) {
                // Add a leading '0' if the byte value is represented with one hex digit.
                sb.append('0');
            }
            sb.append(Integer.toHexString(val));
        }
        return sb.toString();
    }

    public static byte[] hexStringToBytes(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(hexString.substring(index, index + 2), 16);
            bytes[i] = (byte) val;
        }
        return bytes;
    }


}
