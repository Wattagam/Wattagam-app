package com.wattagam.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.wattagam.app.R
import com.wattagam.app.data.AccountProblem
import com.wattagam.app.data.api.ApiException
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** 서버에서 받아오는 화면 데이터의 상태 */
sealed interface Remote<out T> {
    data object Loading : Remote<Nothing>
    data class Failed(val message: String) : Remote<Nothing>
    data class Ready<T>(val value: T) : Remote<T>
}

class RemoteHandle<T>(val state: Remote<T>, val retry: () -> Unit)

/** 다시 로그인·닉네임 설정이 필요할 때 부를 처리 (앱 경로에서 넣어 준다) */
val LocalAccountProblemHandler = compositionLocalOf<(AccountProblem) -> Unit> { {} }

/**
 * [key] 가 바뀌거나 '다시 시도'를 누르면 [load] 를 부른다. [cached] 가 있으면 불러오지 않고 바로 보여준다.
 * 계정 문제는 [LocalAccountProblemHandler] 로 넘기고, 나머지 실패는 사용자용 문구로 바꾼다.
 */
@Composable
fun <T> rememberRemote(key: Any?, cached: T? = null, load: suspend () -> T): RemoteHandle<T> {
    val context = LocalContext.current
    val onAccountProblem = LocalAccountProblemHandler.current
    var attempt by remember(key) { mutableIntStateOf(0) }
    val start: Remote<T> = if (cached != null) Remote.Ready(cached) else Remote.Loading
    val state by produceState(start, key, attempt) {
        if (cached != null && attempt == 0) return@produceState
        value = Remote.Loading
        value = try {
            Remote.Ready(load())
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccountProblem) {
            onAccountProblem(e)
            Remote.Failed(e.userMessage(context))
        } catch (e: Exception) {
            Remote.Failed(e.userMessage(context))
        }
    }
    return RemoteHandle(state) { attempt++ }
}

/** 실패를 화면에 보여줄 문구로. 서버가 준 문구가 있으면 그대로 쓴다 */
fun Throwable.userMessage(context: Context): String = when (this) {
    is AccountProblem.SessionExpired -> context.getString(R.string.error_session_expired)
    is AccountProblem.NicknameRequired -> context.getString(R.string.error_nickname_required)
    is ApiException -> message ?: context.getString(R.string.error_server)
    is IOException -> context.getString(R.string.error_network)
    else -> context.getString(R.string.error_server)
}

/**
 * 불러오는 중이면 진행 표시, 실패하면 이유와 '다시 시도', 성공하면 [content].
 * [onBack] 을 주면 불러오는 동안에도 뒤로 갈 수 있게 위 막대를 둔다.
 */
@Composable
fun <T> RemoteContent(
    handle: RemoteHandle<T>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (val state = handle.state) {
        is Remote.Ready -> content(state.value)
        else -> Scaffold(
            modifier,
            topBar = {
                if (onBack != null) AppTopBar("", stringResource(R.string.back), onBack, Modifier.statusBarsPadding())
            },
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets).padding(WattagamSpacing.Screen), contentAlignment = Alignment.Center) {
                if (state is Remote.Failed) {
                    RemoteError(state.message, handle.retry)
                } else {
                    val loading = stringResource(R.string.loading)
                    CircularProgressIndicator(Modifier.semantics { contentDescription = loading }, color = WattagamColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun RemoteError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
        StatusCard(stringResource(R.string.error_load_title), message, Modifier.fillMaxWidth())
        WattagamButton(stringResource(R.string.retry), onRetry, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun RemoteErrorPreview() {
    WattagamTheme {
        RemoteContent(RemoteHandle<Unit>(Remote.Failed("서버에 연결하지 못했어요. 인터넷 연결을 확인하고 다시 시도해주세요."), {}), onBack = {}) {}
    }
}
