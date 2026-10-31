package com.rohit.hypercharge.updater

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.rohit.hypercharge.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>?
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(
        val versionTag: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val webUrl: String
    ) : UpdateStatus()
    data class UpToDate(val currentVersion: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object AppUpdater {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/ROHITSHARMA838888/hypercharge-control-hub/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "HyperChargeControlHub")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateStatus.Error("Failed to connect to GitHub (HTTP ${response.code})")
            }

            val body = response.body?.string() ?: return@withContext UpdateStatus.Error("Empty response from server")
            val release = Gson().fromJson(body, GitHubRelease::class.java)

            val remoteTag = release.tagName.trim().removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME.trim().removePrefix("v")

            if (isRemoteNewer(remoteTag, currentVersion)) {
                val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl
                UpdateStatus.Available(
                    versionTag = release.tagName,
                    releaseNotes = release.body ?: "Bug fixes and performance enhancements.",
                    downloadUrl = downloadUrl,
                    webUrl = release.htmlUrl
                )
            } else {
                UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            UpdateStatus.Error(e.message ?: "Network error checking for updates")
        }
    }

    private fun isRemoteNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
