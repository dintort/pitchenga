plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
//    jcenter()
//    maven {
//        url = uri("https://mvnrepository.com/artifact/")
//    }
//    mavenCentral()
//    google()
}

dependencies {
    //implementation 'colt:colt:1.2.0'
    implementation(files("lib/TarsosDSP-2.4.jar"))
    implementation(files("lib/JTransforms-2.4.jar"))
    implementation(files("lib/commons-math3-3.2.jar"))
    implementation(files("lib/commons-lang3-3.1.jar"))
    implementation(files("lib/jlayer-1.0.1.4.jar"))
    implementation(files("lib/macify-1.6.jar"))
    implementation(files("lib/mp3spi-1.9.5.4.jar"))
    implementation(files("lib/tritonus-share-0.3.7.4.jar"))
//    implementation("edu.emory.mathcs:JTransforms:2.4")
//    implementation("org.apache.commons:commons-math3:3.2")
//    implementation("org.apache.commons:commons-lang3:3.1")
//    implementation("org.simplericity.macify:macify:1.6")
//    implementation("javazoom:jlayer:1.0.1")
//    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
//    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")

//    macBundleApp(":appbundler:1.0")

    implementation(files("lib/jogamp/gluegen-rt.jar"))
    implementation(files("lib/jogamp/jogl-all.jar"))
    implementation(files("lib/jogamp/gluegen-java-src.zip"))
    implementation(files("lib/jogamp/jogl-java-src.zip"))

    //fixme: if (os = muzdie) {
    implementation(files("lib/jogamp/gluegen-rt-natives-windows-amd64.jar"))
    implementation(files("lib/jogamp/jogl-all-natives-windows-amd64.jar"))
//    implementation(files("lib/jogamp/gluegen-rt-natives-linux-amd64.jar"))
//    implementation(files("lib/jogamp/jogl-all-natives-linux-amd64.jar"))
    implementation(files("lib/jogamp/gluegen-rt-natives-macosx-universal.jar"))
    implementation(files("lib/jogamp/jogl-all-natives-macosx-universal.jar"))

//    implementation("org.jogl-all.jogl:jogl-all-natives-macosx-universal:v2.4.0-rc-20210111")
//    macRuntime(":jogl-all-natives-macosx-universal:")
//    implementation("org.jogamp.jogl:jogl-all:2.3.2")
//    implementation("org.jogamp.gluegen:gluegen-rt:2.3.2")
//    implementation("org.jogamp.gluegen:gluegen-rt:")
//    macRuntime(":gluegen-rt-natives-macosx-universal:")
//    test("junit:junit:4.10")
}

application {
    mainClassName = "com.pitchenga.Pitchenga"
}