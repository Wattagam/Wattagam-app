package com.wattagam.app

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * 디버그 빌드 전용 · 앱이 갑자기 꺼진 이유를 남겨 다음 실행 때 보여준다.
 * 폰을 노트북에 연결하지 않고도 원인(오류 위치)을 확인하기 위한 개발용 도구다.
 * 코틀린·자바 오류는 직접 기록하고, 네이티브(AR·지도 엔진) 오류는 안드로이드가 남긴 종료 기록(Android 11+)에서 읽는다.
 */
internal object DebugCrashLog {
    private const val FILE_NAME = "debug_last_crash.txt"
    private const val PREFS = "debug_crash"
    private const val KEY_SEEN = "seen_until"
    private const val MAX_TOMBSTONE_BYTES = 2 * 1024 * 1024
    private const val MAX_NATIVE_LINES = 80

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { File(app.filesDir, FILE_NAME).writeText("${Date()} · ${thread.name}\n${Log.getStackTraceString(error)}") }
            previous?.uncaughtException(thread, error)
        }
    }

    /** 아직 확인하지 않은 비정상 종료 기록. 없으면 null (백그라운드 스레드에서 호출) */
    fun read(context: Context): String? {
        val parts = mutableListOf<String>()
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()?.let { parts += it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SEEN, 0)
            val exits = context.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(context.packageName, 0, 5)
            val crashReasons = setOf(ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE, ApplicationExitInfo.REASON_ANR)
            exits.filter { it.timestamp > seen && it.reason in crashReasons }.maxByOrNull { it.timestamp }?.let { info ->
                parts += buildString {
                    append(Date(info.timestamp)).append(" · ").append(reasonName(info.reason))
                    info.description?.let { append('\n').append(it) }
                    if (info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                        runCatching { info.traceInputStream?.use { readableLines(it) } }.getOrNull()?.let { append('\n').append(it) }
                    }
                }
            }
        }
        return parts.joinToString("\n\n").ifBlank { null }
    }

    /** 확인한 기록을 지운다. 이후에는 새로 생긴 종료만 보여준다 */
    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putLong(KEY_SEEN, System.currentTimeMillis()) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun reasonName(reason: Int) = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "앱 오류(코틀린·자바)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "네이티브 오류"
        ApplicationExitInfo.REASON_ANR -> "응답 없음(ANR)"
        else -> "종료 코드 $reason"
    }

    /** 네이티브 오류 기록(바이너리)에서 사람이 읽을 수 있는 글자 줄(오류 신호·함수·라이브러리 이름)만 뽑는다 */
    private fun readableLines(input: InputStream): String {
        val bytes = input.readNBytesCompat(MAX_TOMBSTONE_BYTES)
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) {
                current.append(c.toChar())
            } else {
                if (current.length >= 6) lines += current.toString()
                current.clear()
                if (lines.size >= MAX_NATIVE_LINES) break
            }
        }
        return lines.joinToString("\n")
    }

    private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
        val buffer = ByteArray(limit)
        var total = 0
        while (total < limit) {
            val read = read(buffer, total, limit - total)
            if (read < 0) break
            total += read
        }
        return buffer.copyOf(total)
    }
}

/** 디버그 빌드 전용 · 직전에 앱이 비정상 종료됐으면 그 기록을 보여준다 (스크린샷으로 공유) */
@Composable
internal fun DebugCrashDialog() {
    val context = LocalContext.current
    var log by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { log = withContext(Dispatchers.IO) { DebugCrashLog.read(context) } }
    val text = log ?: return
    AlertDialog(
        onDismissRequest = { log = null },
        title = { Text(stringResource(R.string.debug_crash_title)) },
        text = {
            SelectionContainer {
                Text(
                    text,
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                log = null
                Thread { DebugCrashLog.clear(context.applicationContext) }.start()
            }) { Text(stringResource(R.string.debug_crash_clear)) }
        },
        dismissButton = { TextButton(onClick = { log = null }) { Text(stringResource(R.string.debug_crash_close)) } },
    )
}
