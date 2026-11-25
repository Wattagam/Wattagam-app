import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// 카카오 네이티브 앱 키는 Git 에 올리지 않고 개인 local.properties 에 둔다 (KAKAO_NATIVE_APP_KEY=...)
// 키가 없으면 카카오 로그인 없이 샘플 흐름으로 동작한다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY", "")
// 왔다감 서버 주소 (예: WATTAGAM_API_URL=https://api.example.com). 비어 있으면 서버 없이 샘플 데이터로 동작한다.
val wattagamApiUrl: String = localProperties.getProperty("WATTAGAM_API_URL", "").trim().trimEnd('/')
// 출시용 서명 키 정보도 Git 에 올리지 않는 keystore.properties 에 둔다 (storeFile, storePassword, keyAlias, keyPassword).
// 파일이 없으면 출시용 빌드는 서명 없이 만들어진다 (설치 불가).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.wattagam.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.wattagam.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "WATTAGAM_API_URL", "\"$wattagamApiUrl\"")
        manifestPlaceholders["kakaoScheme"] = "kakao$kakaoNativeAppKey"
    }
    signingConfigs {
        keystoreProperties.getProperty("storeFile")?.let { path ->
            create("release") {
                storeFile = rootProject.file(path)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // 카카오·ARCore·직렬화 라이브러리의 난독화 규칙을 검증하기 전까지는 코드 줄이기를 끈다
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.serialization.core)
    implementation(libs.serialization.json)
    implementation(libs.okhttp)
    // 방명록 사진(서버 이미지 주소) 표시
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.kakao.user)
    implementation(libs.kakao.map)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.exifinterface)
    implementation(libs.arsceneview)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.test.junit4)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
}
