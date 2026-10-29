package com.monteirogames.nutriverse.util

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object AppCheckDebugHelper {

    private const val PREFS_PREFIX = "com.google.firebase.appcheck.debug.store."
    private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    private val UUID_REGEX = Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")

    /**
     * Retrieves the authentic debug token generated and stored by the Firebase App Check SDK.
     * Never fabricates, generates, or guesses a token.
     */
    suspend fun getDebugToken(context: Context): String? = withContext(Dispatchers.IO) {
        // 1. Try reading directly from FirebaseApp's persistence store
        var token = readFromFirebaseAppStore(context)
        if (isValidToken(token)) return@withContext token

        // 2. Scan shared_prefs directory for any debug store file
        token = searchDebugStorePrefs(context)
        if (isValidToken(token)) return@withContext token

        // 3. Check Logcat output for the official log line emitted by DebugAppCheckProvider
        token = extractTokenFromLogcat()
        if (isValidToken(token)) return@withContext token

        // 4. If not ready yet, trigger getAppCheckToken to ensure DebugAppCheckProvider executes determineDebugSecret
        try {
            FirebaseAppCheck.getInstance().getAppCheckToken(false)
        } catch (_: Exception) {}

        // Poll for up to 3 seconds (10 x 300ms) for background generation to complete
        for (i in 1..10) {
            delay(300)
            token = readFromFirebaseAppStore(context)
            if (isValidToken(token)) return@withContext token

            token = searchDebugStorePrefs(context)
            if (isValidToken(token)) return@withContext token

            token = extractTokenFromLogcat()
            if (isValidToken(token)) return@withContext token
        }

        null
    }

    private fun readFromFirebaseAppStore(context: Context): String? {
        return try {
            val firebaseApp = FirebaseApp.getInstance()
            val prefsName = "$PREFS_PREFIX${firebaseApp.persistenceKey}"
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.getString(DEBUG_SECRET_KEY, null)?.trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun searchDebugStorePrefs(context: Context): String? {
        return try {
            val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                val files = sharedPrefsDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.name.startsWith(PREFS_PREFIX) && file.name.endsWith(".xml")) {
                        val prefsName = file.name.removeSuffix(".xml")
                        val sp = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        val secret = sp.getString(DEBUG_SECRET_KEY, null)?.trim()
                        if (isValidToken(secret)) {
                            return secret
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTokenFromLogcat(): String? {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -v tag")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val regexWithParens = Regex("""Enter this debug secret \(([a-fA-F0-9\-]+)\)""")
            val uuidExtractor = Regex("""([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})""")

            var line: String?
            var foundToken: String? = null
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("Enter this debug secret") || currentLine.contains("DebugAppCheckProvider")) {
                    val match = regexWithParens.find(currentLine)
                    if (match != null) {
                        foundToken = match.groupValues[1].trim()
                    } else {
                        val uuidMatch = uuidExtractor.find(currentLine)
                        if (uuidMatch != null) {
                            foundToken = uuidMatch.groupValues[1].trim()
                        }
                    }
                }
            }
            reader.close()
            if (isValidToken(foundToken)) foundToken else null
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return UUID_REGEX.matches(token.trim())
    }
}
