plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("com.google.dagger.hilt.android")
	id("com.google.devtools.ksp")
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
	}

	buildTypes {
		release {
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
	implementation("org.opencv:opencv:4.10.0")
	implementation("com.google.mlkit:face-detection:16.1.7")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")

	testImplementation("junit:junit:4.13.2")
	androidTestImplementation("androidx.test.ext:junit:1.2.1")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
