package com.freefit.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val notes: String,
    val sizeBytes: Long,
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Ready(val file: File, val info: UpdateInfo) : UpdateState()
    data class Failed(val info: UpdateInfo) : UpdateState()
}

/**
 * 应用内自更新（OTA）：检查 GitHub 最新 Release → 下载 APK → 拉起系统安装器。
 * 全程零服务器：版本源是本仓库 Releases 的公开 API。
 */
object Updater {

    private const val RELEASE_API = "https://api.github.com/repos/songyi1999/freefit/releases/latest"
    private const val ASSET_NAME = "FreeFit-release.apk"

    /** 检查更新；无更新或网络失败返回 null（静默降级） */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(RELEASE_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "FreeFit-App")
            if (conn.responseCode != 200) return@runCatching null
            val root = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = root.optString("tag_name").trimStart('v', 'V')
            if (!isNewer(tag, currentVersion)) return@runCatching null
            var url: String? = null
            var size = 0L
            val assets = root.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name") == ASSET_NAME) {
                        url = a.optString("browser_download_url")
                        size = a.optLong("size")
                        break
                    }
                }
            }
            url?.let { UpdateInfo(tag, it, root.optString("body"), size) }
        }.getOrNull()
    }

    /** 下载 APK 到应用私有目录；进度 0..100；失败返回 null */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "updates").apply { mkdirs() }
            // 清理旧版本安装包
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "freefit-${info.version}.apk")

            val conn = URL(info.downloadUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("User-Agent", "FreeFit-App")
            if (conn.responseCode != 200) return@runCatching null
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
            var read = 0L
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (read > 1_000_000) file else null
        }.getOrNull()
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** 引导用户到系统设置开启"允许安装未知应用" */
    fun requestInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** 拉起系统安装器覆盖安装（签名一致，数据保留） */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** 浏览器打开 Release 页（应用内下载失败时的兜底） */
    fun openInBrowser(context: Context, info: UpdateInfo) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrNull(i) ?: 0
            val y = b.getOrNull(i) ?: 0
            if (x != y) return x > y
        }
        return false
    }
}
