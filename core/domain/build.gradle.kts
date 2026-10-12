// Domena: modeli, pravila in vmesniki (porti). Cisti Kotlin/JVM - brez Androida,
// zato prevajalnik prepreci, da bi domena kdaj uvozila Android ali podatkovno plast.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // Skupni testni podatki (testHabit, FakeDayClock ...) za teste domene in aplikacije
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    // @Inject konstruktorji; tovarne generira Dagger ze tu, Hilt v :app jih le poveze
    api(libs.dagger)
    ksp(libs.dagger.compiler)

    testFixturesApi(libs.kotlinx.coroutines.core)
    testFixturesApi(libs.kotlinx.coroutines.test)
    testFixturesApi(libs.junit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
