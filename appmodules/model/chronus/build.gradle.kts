import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.serialization)
}

kotlin {
	applyDefaultHierarchyTemplate()
	jvm()
	iosArm64() // iOS
	iosSimulatorArm64() // macOS Apple Silicon
	iosX64() // macOS Intel

	androidTarget {
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.target.get()))
		}
	}

	sourceSets {
		commonMain.dependencies {
			api(libs.kotlinx.datetime)
			api(libs.kotlinx.coroutines.core)
			implementation(libs.kotlinx.serialization)
		}
	}
}

android {
	namespace = "model.chronus"
	compileSdk = libs.versions.androidsdk.target.get().toInt()

	defaultConfig {
		minSdk = libs.versions.androidsdk.min.get().toInt()
	}

	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
		targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
	}
}
