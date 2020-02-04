plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(files("lib/jfugue-5.0.9.jar"))
    implementation(files("lib/TarsosDSP-2.4.jar"))
    implementation(files("lib/tinysound-1.1.1.jar"))
}

application {
    mainClassName = "com.pitchenga.Pitchenga"
}
