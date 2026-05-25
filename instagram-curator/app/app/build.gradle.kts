import java.util.Properties

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("com.google.dagger.hilt.android")
	id("com.google.devtools.ksp")
}

/**
 * Loads a properties file from the rootProject directory (instagram-curator/app/).
 * Returns an empty Properties if the file is missing. Used for the per-buildType
 * backend URL split (dev.properties / prod.properties — both gitignored).
 */
fun loadProps(fileName: String): Properties {
	val props = Properties()
	val file = rootProject.file(fileName)
	if (file.exists()) {
		file.inputStream().use { props.load(it) }
	}
	return props
}

android {
	namespace = "com.instacurator.app"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.instacurator.app"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = "0.1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}

		// Restrict to arm64-v8a during dev to keep OpenCV's native libs lean.
		ndk {
			abiFilters += listOf("arm64-v8a")
		}

		// API_BASE_URL is provided per-buildType from dev.properties / prod.properties.
	}

	buildTypes {
		debug {
			val devUrl = loadProps("dev.properties").getProperty("apiBaseUrl")
				?: "http://10.0.2.2:8080/"
			buildConfigField("String", "API_BASE_URL", "\"$devUrl\"")
		}
		release {
			// Fallback is a deliberately unresolvable host so release builds
			// missing prod.properties fail fast at runtime with a clear DNS error.
			// (We can't error() here — release config is evaluated even for debug builds.)
			val prodUrl = loadProps("prod.properties").getProperty("apiBaseUrl")
				?: "https://MISSING-PROD-PROPERTIES.invalid/"
			buildConfigField("String", "API_BASE_URL", "\"$prodUrl\"")
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		compose = true
		// AGP 8 disables buildConfig by default; we need it for API_BASE_URL.
		buildConfig = true
	}

	composeOptions {
		// Compatible with Kotlin 1.9.25.
		// See https://developer.android.com/jetpack/androidx/releases/compose-kotlin
		kotlinCompilerExtensionVersion = "1.5.15"
	}

	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
		jniLibs {
			// Required for Android 15+ 16 KB page-size devices: keep .so files
			// uncompressed and page-aligned in the APK so the loader can mmap
			// them at the device's page size.
			useLegacyPackaging = false
		}
	}
}

dependencies {
	val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
	implementation(composeBom)
	androidTestImplementation(composeBom)

	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
	implementation("androidx.activity:activity-compose:1.9.3")
	implementation("androidx.core:core-splashscreen:1.0.1")

	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-graphics")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.foundation:foundation")
	implementation("androidx.compose.material3:material3")

	implementation("io.coil-kt:coil-compose:2.7.0")

	// Hilt
	implementation("com.google.dagger:hilt-android:2.52")
	ksp("com.google.dagger:hilt-compiler:2.52")
	implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

	// StateFlow.collectAsStateWithLifecycle()
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

	// Phase 2B pipeline
	// 4.13.0 ships .so files with 16 KB-aligned LOAD segments (including
	// the bundled libc++_shared.so), required for Android 15+ 16 KB devices.
	implementation("org.opencv:opencv:4.13.0")
	implementation("com.google.mlkit:face-detection:16.1.7")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

	// Phase 3 networking
	implementation("com.squareup.retrofit2:retrofit:2.11.0")
	implementation("com.squareup.retrofit2:converter-gson:2.11.0")
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

	// Material icons for the Save FAB.
	implementation("androidx.compose.material:material-icons-extended")

	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")

	testImplementation("junit:junit:4.13.2")
	androidTestImplementation("androidx.test.ext:junit:1.2.1")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
