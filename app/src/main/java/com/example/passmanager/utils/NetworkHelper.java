package com.example.passmanager.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Helper class for various network operations, including making requests to the HaveIBeenPwned
 * services.
 */
public class NetworkHelper {

    // Constants for HaveIBeenPwned's "Pwned Passwords" API.
    public static final String HIBP_URL_PWNED_PASSWORDS = "https://api.pwnedpasswords.com/range/";
    public static final String HIBP_HASH_ALGORITHM = "SHA-1";
    public static final int HIBP_HASH_LEN = 40;
    public static final int HIBP_HASH_PREFIX_LEN = 5;
    public static final int HIBP_HASH_SUFFIX_LEN = HIBP_HASH_LEN - HIBP_HASH_PREFIX_LEN;


    /**
     *  Check Internet connectivity.
     *
     * @param context The current application context
     * @return true if the device is connected to the Internet; false otherwise.
     */
    public static boolean isInternetConnectionAvailable(Context context) {
        NetworkInfo networkInfo;
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    /**
     * Check if the password has been compromised in a data breach using the "Pwned Passwords" API
     * provided by haveibeenpwned.com
     *
     * @param password The plaintext password
     * @param clearPass Whether to clear the password after checking or not.
     * @return true if the password hash has been found in a data breach; false otherwise.
     */
    public static boolean isPassPwned(char[] password, boolean clearPass) {
        // Hash the password with the algorithm used by HIBP to store their passwords.
        String hash = CryptoHelper.hash(password, HIBP_HASH_ALGORITHM, clearPass);
        if (hash != null) {
            String hashPrefix = hash.substring(0, HIBP_HASH_PREFIX_LEN);
            String hashSuffix = hash.substring(HIBP_HASH_PREFIX_LEN);
            HttpsURLConnection urlConnection;
            BufferedReader reader;

            try {
                // Use the hash prefix in order to receive a response with all password hash
                // suffixes in the HIBP's passwords database that begin with the specified prefix.
                Uri uri = Uri.parse(HIBP_URL_PWNED_PASSWORDS + hashPrefix);
                URL requestUrl = new URL(uri.toString());

                // Setup the connection.
                urlConnection = (HttpsURLConnection) requestUrl.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Get the response.
                InputStream inputStream = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                // Parse each line of the response.
                String line;
                while ((line = reader.readLine()) != null) {
                    String responseHashSuffix = line.substring(0, HIBP_HASH_SUFFIX_LEN);
                    int responseHashCount = Integer.
                            parseInt(line.substring(HIBP_HASH_SUFFIX_LEN  + 1));

                    // If one of the response suffixes matches with our password's hash suffix,
                    // then the password hash is included in the HIBP database.
                    if (responseHashSuffix.equals(hashSuffix)) {
                        return true;
                    }
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
