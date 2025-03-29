plugins {
    java
    application
}

repositories {
//    mavenCentral()
//    maven {
//        url = uri("https://mvnrepository.com/artifact/")
//    }
//    google()
}

dependencies {
    //implementation 'colt:colt:1.2.0'
    implementation(files("lib/TarsosDSP-2.4.jar"))
    implementation(files("lib/JTransforms-2.4.jar"))
    implementation(files("lib/commons-math3-3.2.jar"))
    implementation(files("lib/commons-lang3-3.1.jar"))
    implementation(files("lib/jlayer-1.0.1.4.jar"))
//    implementation(files("lib/macify-1.6.jar"))
//    implementation(files("lib/mp3spi-1.9.5.4.jar"))
//    implementation(files("lib/tritonus-share-0.3.7.4.jar"))
//    implementation("edu.emory.mathcs:JTransforms:2.4")
//    implementation("org.apache.commons:commons-math3:3.2")
//    implementation("org.apache.commons:commons-lang3:3.1")
//    implementation("org.simplericity.macify:macify:1.6")
//    implementation("javazoom:jlayer:1.0.1")
//    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
//    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")

//    macBundleApp(":appbundler:1.0")

    implementation(files("lib/jogamp/gluegen-rt-2.5.0.jar"))
//    implementation(files("lib/jogamp/gluegen-rt-2.5.0-sources.zip"))
    implementation(files("lib/jogamp/gluegen-rt-2.5.0-natives-macosx-universal.jar"))
    implementation(files("lib/jogamp/jogl-all-2.5.0.jar"))
//    implementation(files("lib/jogamp/jogl-all-2.5.0-sources.zip"))
    implementation(files("lib/jogamp/jogl-all-2.5.0-natives-macosx-universal.jar"))
    //fixme: if (os = muzdie) {
//    implementation(files("lib/jogamp/gluegen-rt-natives-windows-amd64.jar"))
//    implementation(files("lib/jogamp/jogl-all-natives-windows-amd64.jar"))
//    implementation(files("lib/jogamp/gluegen-rt-natives-linux-amd64.jar"))
//    implementation(files("lib/jogamp/jogl-all-natives-linux-amd64.jar"))

//    implementation("org.jogl-all.jogl:jogl-all-natives-macosx-universal:v2.4.0-rc-20210111")
//    macRuntime(":jogl-all-natives-macosx-universal:")
//    implementation("org.jogamp.jogl:jogl-all:2.3.2")
//    implementation("org.jogamp.gluegen:gluegen-rt:2.3.2")
//    implementation("org.jogamp.gluegen:gluegen-rt:")
//    macRuntime(":gluegen-rt-natives-macosx-universal:")
//    test("junit:junit:4.10")
}

tasks {
    distZip {
        enabled = false
    }
    distTar {
        enabled = false
    }
}

application {
//    mainClass.set("com.pitchenga.Pitchenga")
    mainClass.set("com.pitchenga.Ptchng")
    applicationDefaultJvmArgs = listOf(
        "-Xmx1g",
        "-Xms128m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=.",
        "-XX:+ExitOnOutOfMemoryError",
        "-Dcom.pitchenga.debug=true",
        "-Dapple.awt.application.appearance=system",
        "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
        "--add-exports=java.desktop/sun.awt=ALL-UNNAMED"
    )
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass,
            "NSRequiresAquaSystemAppearance" to "False",
        )
    }
    val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Exec>("packageMacApp") {
    dependsOn("jar")
    doFirst {
        delete("build/Pitchenga.app")
    }

    commandLine(
        "jpackage",
        "--input", "build/libs",
        "--main-jar", "pitchenga.jar",
        "--main-class", application.mainClass.get(),
        "--name", "Pitchenga",
        "--dest", "build/",
        "--type", "app-image",
        "--icon", "src/main/resources/pitchenga.icns",
        "--app-version", "1.0.0",
        "--vendor", "Your Name",
        "--mac-package-name", "Pitchenga",
        "--mac-package-identifier", "com.pitchenga",
        "--java-options", "-Xmx1g",
        "--java-options", "-Xms128m",
        "--java-options", "-XX:+HeapDumpOnOutOfMemoryError",
        "--java-options", "-XX:HeapDumpPath=.",
        "--java-options", "-XX:+ExitOnOutOfMemoryError",
        "--java-options", "-Dcom.pitchenga.debug=false",
        "--java-options", "-Dapple.awt.application.appearance=system",
        "--java-options", "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt=ALL-UNNAMED"
    )
}

tasks.register<Copy>("copyMacApp") {
    dependsOn("packageMacApp")
    from(layout.buildDirectory.dir("."))
    into(System.getProperty("user.home") + "/Documents/ptchng/")
    include("Pitchenga.app/**")
}

tasks.assemble {
    finalizedBy("copyMacApp")
}
