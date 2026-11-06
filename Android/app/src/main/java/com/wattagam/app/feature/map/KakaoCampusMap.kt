package com.wattagam.app.feature.map

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.wattagam.app.R

/** 서울캠퍼스 네 장소가 한 화면에 들어오는 중심과 확대 수준 */
private val CampusCenter = LatLng.from(37.50540, 126.95730)
private const val CAMPUS_ZOOM = 16

/**
 * 02 · 카카오맵으로 그리는 캠퍼스 지도. 네 장소에 번호·이름 라벨을 달고, 라벨을 누르면 장소 설명을 연다.
 * 지도 오류(인증 실패 등)가 나면 [onError] 로 알려 안내 지도 그림으로 바꾸게 한다.
 */
@Composable
fun KakaoCampusMap(
    places: List<MapPlaceUiState>,
    onPlaceClick: (placeId: String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnPlaceClick by rememberUpdatedState(onPlaceClick)
    val currentOnError by rememberUpdatedState(onError)
    val mapView = remember { MapView(context) }

    // 지도는 화면 수명주기에 맞춰 멈추고 다시 켜야 한다 (안 하면 SDK 가 비정상 종료될 수 있음)
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception) = currentOnError()
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            val labels = kakaoMap.labelManager ?: return
                            val styles = labels.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.bc_pin).setTextStyles(30, Color.BLACK)))
                            places.forEach { place ->
                                labels.layer?.addLabel(
                                    LabelOptions.from(place.id, LatLng.from(place.latitude, place.longitude))
                                        .setStyles(styles)
                                        .setTexts(LabelTextBuilder().setTexts("${place.order}. ${place.name}"))
                                        .setTag(place.id)
                                        .setClickable(true),
                                )
                            }
                            kakaoMap.setOnLabelClickListener { _, _, label ->
                                val placeId = label.tag as? String ?: return@setOnLabelClickListener false
                                currentOnPlaceClick(placeId)
                                true
                            }
                        }

                        override fun getPosition(): LatLng = CampusCenter
                        override fun getZoomLevel(): Int = CAMPUS_ZOOM
                    },
                )
            }
        },
    )
}
