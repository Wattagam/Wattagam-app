package com.wattagam.app.feature.place

import androidx.annotation.DrawableRes
import com.wattagam.app.R

/**
 * 장소 대표 사진 (서버에는 사진 필드가 없어 앱에 넣어 둔다). 서버·샘플 모두 같은 코스 순서를 쓴다:
 * 1 310관, 2 도서관, 3 청룡연못. 사진이 없는 장소는 null.
 */
@DrawableRes
fun placePhoto(courseOrder: Int): Int? = when (courseOrder) {
    1 -> R.drawable.place_310
    2 -> R.drawable.place_library
    3 -> R.drawable.place_pond
    else -> null
}
