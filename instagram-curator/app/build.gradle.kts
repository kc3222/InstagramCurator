plugins {
	id("com.android.application") version "8.7.2" apply false
	id("org.jetbrains.kotlin.android") version "1.9.25" apply false
	id("com.google.dagger.hilt.android") version "2.52" apply false
	// KSP version must match the Kotlin version (1.9.25).
	id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
}
