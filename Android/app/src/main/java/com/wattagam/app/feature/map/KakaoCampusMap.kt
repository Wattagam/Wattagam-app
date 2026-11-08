package com.wattagam.app.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.CompetitionType
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamColors

/** 서울캠퍼스 네 장소가 한 화면에 들어오는 중심과 확대 수준 */
private val CampusCenter = LatLng.from(37.50540, 126.95730)
private const val CAMPUS_ZOOM = 16
private const val MY_LOCATION_LAYER = "my_location"
private const val MY_LOCATION_LABEL = "me"

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
    myLocation: MapPoint? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnPlaceClick by rememberUpdatedState(onPlaceClick)
    val currentOnError by rememberUpdatedState(onError)
    val mapView = remember { MapView(context) }
    var readyMap by remember { mutableStateOf<KakaoMap?>(null) }

    // 내 위치 점: 지도가 준비되고 위치가 바뀔 때마다 옮긴다. 장소 핀과 겹쳐도 숨지 않게 경쟁 없는 별도 층에 둔다
    LaunchedEffect(readyMap, myLocation) {
        val labels = readyMap?.labelManager ?: return@LaunchedEffect
        val layer = labels.getLayer(MY_LOCATION_LAYER)
            ?: labels.addLayer(LabelLayerOptions.from(MY_LOCATION_LAYER).setCompetitionType(CompetitionType.None).setClickable(false))
            ?: return@LaunchedEffect
        val point = myLocation
        val existing = layer.getLabel(MY_LOCATION_LABEL)
        when {
            point == null -> existing?.let { layer.remove(it) }
            existing != null -> existing.moveTo(LatLng.from(point.latitude, point.longitude))
            else -> layer.addLabel(
                LabelOptions.from(MY_LOCATION_LABEL, LatLng.from(point.latitude, point.longitude))
                    .setStyles(labels.addLabelStyles(LabelStyles.from(LabelStyle.from(myLocationBitmap(context)).setApplyDpScale(false)))),
            )
        }
    }

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
                            places.forEach { place ->
                                // 장소마다 번호·이름·도장 상태가 그려진 핀 이미지를 쓴다. 핀 아래 꼭짓점이 장소 좌표에 오도록 한다
                                val style = LabelStyle.from(placePinBitmap(context, place.order, place.name, place.stamped))
                                    .setAnchorPoint(0.5f, 1f)
                                    .setApplyDpScale(false)
                                labels.layer?.addLabel(
                                    LabelOptions.from(place.id, LatLng.from(place.latitude, place.longitude))
                                        .setStyles(labels.addLabelStyles(LabelStyles.from(style)))
                                        .setTag(place.id)
                                        .setClickable(true),
                                )
                            }
                            readyMap = kakaoMap
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

/**
 * 지도 핀 이미지. 흰 말풍선 안에 번호 원과 장소 이름을 넣고, 아래 꼬리 끝이 장소 위치를 가리킨다.
 * 도장을 받은 장소는 번호 원을 파란색으로 채우고, 아직이면 테두리만 그린다 (색만으로 구분하지 않게 이름 옆에 '획득' 표시).
 */
private fun placePinBitmap(context: Context, order: Int, name: String, stamped: Boolean): Bitmap {
    val d = context.resources.displayMetrics.density
    val primary = WattagamColors.Primary.toArgb()
    val ink = WattagamColors.Ink.toArgb()
    val base = runCatching { ResourcesCompat.getFont(context, R.font.noto_sans_kr) }.getOrNull() ?: Typeface.DEFAULT
    val bold = Typeface.create(base, Typeface.BOLD)

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 14 * d; color = ink }
    // '획득'은 도장 인주색 (장소 목록·도장판의 도장 자국과 같은 색)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 11 * d; color = WattagamColors.StampInk.toArgb() }
    val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bold; textSize = 13 * d; textAlign = Paint.Align.CENTER; color = if (stamped) android.graphics.Color.WHITE else primary
    }
    val badge = if (stamped) context.getString(R.string.earned) else null

    val shadow = 4 * d
    val height = 36 * d
    val tail = 8 * d
    val circle = 24 * d
    val padding = 6 * d
    val gap = 8 * d
    val textWidth = namePaint.measureText(name) + (badge?.let { gap * 0.75f + badgePaint.measureText(it) } ?: 0f)
    val width = padding + circle + gap + textWidth + padding * 2

    val bitmap = createBitmap((width + shadow * 2).toInt(), (height + tail + shadow * 2).toInt())
    val canvas = Canvas(bitmap)
    val left = shadow
    val top = shadow
    val bubble = RectF(left, top, left + width, top + height)
    val centerX = left + width / 2

    // 말풍선과 꼬리를 한 윤곽으로 합쳐 테두리가 이음새 없이 그려지게 한다
    val bubblePath = Path().apply { addRoundRect(bubble, height / 2, height / 2, Path.Direction.CW) }
    val tailPath = Path().apply {
        moveTo(centerX - tail * 0.8f, bubble.bottom - tail)
        lineTo(centerX, bubble.bottom + tail)
        lineTo(centerX + tail * 0.8f, bubble.bottom - tail)
        close()
    }
    bubblePath.op(tailPath, Path.Op.UNION)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        setShadowLayer(shadow * 0.8f, 0f, d, 0x33000000)
    }
    canvas.drawPath(bubblePath, fill)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d; color = primary }
    canvas.drawPath(bubblePath, border)

    val cx = left + padding + circle / 2
    val cy = top + height / 2
    if (stamped) {
        canvas.drawCircle(cx, cy, circle / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary })
    } else {
        canvas.drawCircle(cx, cy, circle / 2 - 0.75f * d, border)
    }
    canvas.drawText(order.toString(), cx, cy - (numberPaint.ascent() + numberPaint.descent()) / 2, numberPaint)

    val textX = left + padding + circle + gap
    val baseline = cy - (namePaint.ascent() + namePaint.descent()) / 2
    canvas.drawText(name, textX, baseline, namePaint)
    if (badge != null) canvas.drawText(badge, textX + namePaint.measureText(name) + gap * 0.75f, baseline, badgePaint)
    return bitmap
}

/** 내 위치 점: 파란 원에 흰 테두리, 옅은 파란 테두리로 지도 위에서 잘 보이게 한다 */
private fun myLocationBitmap(context: Context): Bitmap {
    val d = context.resources.displayMetrics.density
    val size = (28 * d).toInt()
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val primary = WattagamColors.Primary.toArgb()
    canvas.drawCircle(c, c, 14 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary; alpha = 60 })
    canvas.drawCircle(c, c, 8 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
    canvas.drawCircle(c, c, 6 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary })
    return bitmap
}
