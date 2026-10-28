import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import java.time.Duration

plugins {
    application
}

group = "bms.player"
version = providers.gradleProperty("releaseVersion").getOrElse("0.9.0-SNAPSHOT")

// Ant owns build/ and release/. Neither build system cleans the other's output.
layout.buildDirectory.set(layout.projectDirectory.dir("build-gradle"))

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src"))
        resources.exclude("**/*.java", "**/*.class", "META-INF/MANIFEST.MF", "**/.DS_Store")
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(emptyList<String>())
    }
}

repositories {
    mavenCentral()
}

// Retain unrelated custom libraries; controllers use the 2.2.4 API with direct JInput polling.
val bundledJars = listOf(
    "java-discord-rpc-2.0.1-all.jar",
    "jbms-parser.jar", "jbmstable-parser.jar", "jflac-codec-1.5.3.jar", "jportaudio.jar",
    "tarsosdsp-latest.jar"
)
val luaJar = listOf("luaj-jse-3.0.2-custom.jar", "luaj-jse.jar")
    .firstOrNull { file("lib/$it").isFile } ?: "luaj-jse-3.0.2-custom.jar"
val localLibraries = (bundledJars + luaJar).map { file("lib/$it") }
dependencies {
    implementation(files(localLibraries))
    implementation("com.badlogicgames.gdx:gdx:1.14.2")
    implementation("com.badlogicgames.gdx:gdx-freetype:1.14.2")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2") { isTransitive = false }
    implementation("com.badlogicgames.jlayer:jlayer:1.0.1-gdx") { isTransitive = false }
    implementation("org.jcraft:jorbis:0.0.17") { isTransitive = false }
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:2.2.4")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-desktop:2.2.4")
    implementation("net.java.jinput:jinput:2.0.5")
    // Regression oracle only: never include LWJGL2 in the application or distribution.
    testImplementation("org.lwjgl.lwjgl:lwjgl:2.9.3") { isTransitive = false }
    // Match the LWJGL3 version required by libGDX 1.14.2; keep native modules aligned.
    for (module in listOf("lwjgl", "lwjgl-glfw", "lwjgl-jemalloc", "lwjgl-openal", "lwjgl-opengl", "lwjgl-stb")) {
        implementation("org.lwjgl:$module:3.3.3") { isTransitive = false }
        for (platform in listOf("windows", "linux", "macos", "macos-arm64")) {
            runtimeOnly("org.lwjgl:$module:3.3.3:natives-$platform") { isTransitive = false }
        }
    }
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop") { isTransitive = false }
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-desktop") { isTransitive = false }
    // JInput remains independent from the rendering backend.
    for (platform in listOf("linux", "windows", "osx")) {
        runtimeOnly("net.java.jinput:jinput-platform:2.0.5:natives-$platform") { isTransitive = false }
    }
    // Preserve the existing runtime set instead of importing new transitive dependencies.
    implementation("org.apache.commons:commons-compress:1.16.1") { isTransitive = false }
    implementation("commons-dbutils:commons-dbutils:1.6") { isTransitive = false }
    implementation("org.xerial:sqlite-jdbc:3.46.1.0") { isTransitive = false }
    runtimeOnly("org.slf4j:slf4j-api:1.7.36") { isTransitive = false }
    implementation("com.github.junrar:junrar:7.6.0") { isTransitive = false }
    implementation("org.bytedeco:javacpp:1.3.3") { isTransitive = false }
    implementation("org.bytedeco:javacv:1.3.3") { isTransitive = false }
    runtimeOnly("org.bytedeco:javacv-platform:1.3.3") { isTransitive = false }
    implementation("org.bytedeco.javacpp-presets:ffmpeg:3.2.1-1.3") { isTransitive = false }
    runtimeOnly("org.bytedeco.javacpp-presets:ffmpeg-platform:3.2.1-1.3") { isTransitive = false }
    // Match the existing native set, not the platform POM's broader dependency graph.
    for (classifier in listOf("linux-x86", "linux-x86_64", "macosx-x86_64", "windows-x86", "windows-x86_64")) {
        runtimeOnly("org.bytedeco.javacpp-presets:ffmpeg:3.2.1-1.3:$classifier") { isTransitive = false }
    }
}

val java17 = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }
val verifyBuildEnvironment by tasks.registering {
    group = "verification"
    description = "Verify the existing local libraries and JavaFX-enabled JDK 17."
    doLast {
        val missing = localLibraries.filterNot { it.isFile }
        check(missing.isEmpty()) { "Missing bundled JARs: ${missing.joinToString()}. Restore lib/ before building." }
        val home = java17.get().metadata.installationPath.asFile
        check(home.resolve("jmods/javafx.controls.jmod").isFile && home.resolve("jmods/javafx.fxml.jmod").isFile) {
            "A JavaFX-enabled JDK 17 is required (for example Liberica Full JDK 17). " +
                "Set JAVA_HOME and, if necessary, -Dorg.gradle.java.installations.paths=<JDK home>. Selected: $home"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(verifyBuildEnvironment)
    options.encoding = "UTF-8"
    options.release.set(17)
}
tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(java17)
}
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

application {
    mainClass.set("bms.player.beatoraja.DesktopLauncher")
    applicationName = "beatoraja"
    applicationDefaultJvmArgs = listOf("-Xms1g", "-Xmx4g", "-Djava.library.path=./natives")
}
tasks.named<JavaExec>("run") {
    dependsOn(verifyBuildEnvironment)
    workingDir(providers.gradleProperty("runWorkingDir").getOrElse(projectDir.absolutePath))
    standardInput = System.`in`
    for (name in listOf("frameDiagnostics", "frameDiagnosticsIntervalMillis", "frameDiagnosticsLongFrameMillis")) {
        providers.gradleProperty(name).orNull?.let {
            systemProperty(when (name) {
                "frameDiagnosticsIntervalMillis" -> "beatoraja.frameDiagnostics.intervalMillis"
                "frameDiagnosticsLongFrameMillis" -> "beatoraja.frameDiagnostics.longFrameMillis"
                else -> "beatoraja.frameDiagnostics"
            }, it)
        }
    }
    providers.gradleProperty("audioDiagnostics").orNull?.let { systemProperty("beatoraja.audioDiagnostics", it) }
    providers.gradleProperty("audioDiagnosticsIntervalMillis").orNull?.let {
        systemProperty("beatoraja.audioDiagnostics.intervalMillis", it)
    }
    providers.gradleProperty("windowDiagnostics").orNull?.let {
        systemProperty("beatoraja.windowDiagnostics", it)
    }
    providers.gradleProperty("inputDiagnostics").orNull?.let {
        systemProperty("beatoraja.inputDiagnostics", it)
    }
    providers.gradleProperty("inputDiagnosticsIntervalMillis").orNull?.let {
        systemProperty("beatoraja.inputDiagnostics.intervalMillis", it)
    }
    providers.gradleProperty("memoryDiagnosticsOutput").orNull?.let {
        systemProperty("beatoraja.memoryDiagnostics.output", it)
    }
    providers.gradleProperty("memoryDiagnosticsSettleMillis").orNull?.let {
        systemProperty("beatoraja.memoryDiagnostics.settleMillis", it)
    }
}
tasks.named<Jar>("jar") {
    manifest.attributes("Main-Class" to application.mainClass.get())
}

// The existing regression tests are standalone Java programs, not JUnit tests.
val regressionClasses = listOf(
    "bms.player.beatoraja.MemoryDiagnosticsTest",
    "bms.player.beatoraja.video.EnduranceStatisticsTest",
    "bms.player.beatoraja.skin.SkinTextClipboardTest",
    "bms.player.beatoraja.input.TextEditingControlTest",
    "bms.player.beatoraja.input.TextInputSessionTest",
    "bms.player.beatoraja.input.MacOsImeBridgeTest",
    "bms.player.beatoraja.skin.StageViewportTest",
    "bms.player.beatoraja.input.WindowInputLifecycleTest",
    "bms.player.beatoraja.input.InputShutdownTest",
    "bms.player.beatoraja.input.MidiInputLifecycleTest",
    "bms.player.beatoraja.video.VideoFrameCopyTest",
    "bms.player.beatoraja.FramePacingDiagnosticsTest",
    "bms.player.beatoraja.audio.AudioBackendLifecycleTest",
    "bms.player.beatoraja.audio.AudioDiagnosticsTest",
    "bms.player.beatoraja.audio.AudioBackendTestAssetsTest",
    "bms.player.beatoraja.input.MouseScratchGeometryTest",
    "bms.player.beatoraja.WindowModeControllerTest",
    "bms.player.beatoraja.SkinProjectionTest",
    "bms.player.beatoraja.input.WindowDiagnosticsTest",
    "bms.player.beatoraja.play.KeyboardJudgeCompatibilityTest",
    "bms.player.beatoraja.input.KeyboardEventQueueTest",
    "bms.player.beatoraja.input.InputDiagnosticsTest",
    "bms.player.beatoraja.input.ControllerInputDiagnosticsTest",
    "bms.player.beatoraja.input.ControllerEventQueueTest",
    "bms.player.beatoraja.input.MouseScratchDiagnosticsTest",
    "bms.player.beatoraja.input.MouseMotionQueueTest",
    "bms.player.beatoraja.external.DesktopActionsTest",
    "bms.player.beatoraja.ResourcePoolTest",
    "bms.player.beatoraja.InputDataValidationTest",
    "bms.player.beatoraja.Lwjgl3MigrationTest",
    "bms.player.beatoraja.DistributionDiagnosticsTest",
    "bms.player.beatoraja.TwitterIntegrationRemovalTest",
    "bms.player.beatoraja.CourseDataAccessorTest",
    "bms.player.beatoraja.play.PracticeConfigurationTest",
    "bms.player.beatoraja.play.PlayTimeTrackerTest",
    "bms.player.beatoraja.ir.IRScorePlaytimeTest",
    "bms.player.beatoraja.ir.IRConnectionManagerTest",
    "bms.player.beatoraja.select.BarSorterTest",
    "bms.player.beatoraja.select.BarSorterConfigurationTest",
    "bms.player.beatoraja.select.ModeFilterTest",
    "bms.player.beatoraja.audio.PortAudioStreamLifecycleTest",
    "bms.player.beatoraja.video.FFmpegProcessorLifecycleTest",
    "bms.player.beatoraja.launcher.ConfigurationRangeTest",
    "bms.player.beatoraja.launcher.SkinConfigurationViewTest",
    "bms.player.beatoraja.skin.SkinResourceLifecycleTest",
    "bms.player.beatoraja.skin.BitmapFontCacheTest",
    "bms.player.beatoraja.skin.LibGdxCompatibilityTest",
    "bms.player.beatoraja.skin.lr2.LR2SkinResourceLifecycleTest",
    "bms.player.beatoraja.skin.json.JSONSkinResourceLifecycleTest",
    "bms.player.beatoraja.skin.json.JSONSkinLoaderTest",
    "bms.player.beatoraja.skin.json.SkinFontSourceCacheTest",
    "bms.player.beatoraja.input.SkinMouseInputTest",
    "bms.player.beatoraja.input.JInputControllerCompatibilityTest",
    "bms.player.beatoraja.skin.lua.MainStateInputLuaApiTest",
    "bms.player.beatoraja.skin.lua.SkinLuaIoSymlinkTest",
    "bms.player.beatoraja.skin.lua.SkinCompatibilityApiTest",
    "bms.player.beatoraja.skin.lua.SkinHttpLimitsTest",
    "bms.player.beatoraja.song.archive.SongArchivesTest",
    "bms.player.beatoraja.song.archive.RarSongArchiveTest"
)
val regressionTasks = regressionClasses.map { className ->
    tasks.register<JavaExec>("regression${className.substringAfterLast('.')}") {
        group = "verification"
        description = "Run $className."
        dependsOn(tasks.testClasses)
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set(className)
        workingDir(projectDir)
        enableAssertions = true
        maxHeapSize = "1g"
        systemProperty("java.awt.headless", "true")
    }
}
val regressionTest by tasks.registering {
    group = "verification"
    description = "Run all standalone regression tests (fail on any non-zero exit)."
    dependsOn(regressionTasks)
}
tasks.named<Test>("test") {
    // Do not silently skip the main()-based tests or require a new test dependency.
    exclude("**/*")
    dependsOn(regressionTest)
}

// Both launchers must resolve relative config/skin/native paths from the install root.
tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        val unixMarker = "exec \"\$JAVACMD\" \"\$@\""
        val unixText = unixScript.readText()
        check(unixText.contains(unixMarker)) { "Gradle Unix launcher template changed" }
        unixScript.writeText(unixText.replace(unixMarker, "cd \"\$APP_HOME\" || exit 1\n\n$unixMarker"))
        val windowsMarker = ":execute"
        val windowsText = windowsScript.readText()
        check(windowsText.contains(windowsMarker)) { "Gradle Windows launcher template changed" }
        windowsScript.writeText(windowsText.replace(windowsMarker, "$windowsMarker\r\ncd /d \"%APP_HOME%\"\r\nif errorlevel 1 exit /b 1"))
    }
}

// Explicit allowlist: never ship local DBs/configs, private skins, caches or test videos.
val distributionFiles = file("gradle/distribution-files.txt").readLines()
    .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
val prepareRuntimeDirectories by tasks.registering {
    val directories = layout.buildDirectory.dir("runtime-directories")
    outputs.dir(directories)
    doLast {
        // The launcher writes table/default.json on first exit; no local table data is copied.
        check(directories.get().dir("table").asFile.mkdirs() || directories.get().dir("table").asFile.isDirectory)
    }
}
val generateAudioBackendTestAssets by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Generate deterministic WAV/BMS fixtures for manual audio backend comparisons (no audio output)."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.audio.AudioBackendTestAssets")
    val assets = layout.buildDirectory.dir("generated/audio-test")
    args(assets.get().asFile.absolutePath)
    outputs.dir(assets)
    doFirst { delete(assets) }
}
val macOsImeLibrary = layout.buildDirectory.file("generated/macos-ime/libbeatoraja-ime.dylib")
val buildMacOsIme by tasks.registering(Exec::class) {
    group = "build"
    description = "Build opt-in AppKit IME prototype for macOS x86_64 and arm64 (requires Xcode tools)."
    onlyIf { System.getProperty("os.name").startsWith("Mac") }
    inputs.file("native/macos/ime.m")
    outputs.file(macOsImeLibrary)
    doFirst { macOsImeLibrary.get().asFile.parentFile.mkdirs() }
    val home = java17.get().metadata.installationPath.asFile
    commandLine("xcrun", "clang", "-dynamiclib", "-fobjc-arc", "-fblocks", "-arch", "x86_64", "-arch", "arm64",
        "-mmacosx-version-min=11.0", "-I$home/include", "-I$home/include/darwin", "-framework", "AppKit",
        "-Wl,-install_name,@rpath/libbeatoraja-ime.dylib",
        file("native/macos/ime.m").absolutePath, "-o", macOsImeLibrary.get().asFile.absolutePath)
}
val macOsImeTestLibrary = layout.buildDirectory.file("macos-ime-test/libbeatoraja-ime-test.dylib")
val buildMacOsImeTest by tasks.registering(Exec::class) {
    group = "verification"
    onlyIf { System.getProperty("os.name").startsWith("Mac") }
    inputs.files("native/macos/ime.m", "test/native/macos/ime-test.m")
    outputs.file(macOsImeTestLibrary)
    doFirst { macOsImeTestLibrary.get().asFile.parentFile.mkdirs() }
    val home = java17.get().metadata.installationPath.asFile
    commandLine("xcrun", "clang", "-dynamiclib", "-fobjc-arc", "-fblocks", "-arch", "x86_64", "-arch", "arm64",
        "-mmacosx-version-min=11.0", "-I$home/include", "-I$home/include/darwin", "-framework", "AppKit",
        "-Wl,-install_name,@rpath/libbeatoraja-ime-test.dylib",
        file("test/native/macos/ime-test.m").absolutePath, "-o", macOsImeTestLibrary.get().asFile.absolutePath)
}
tasks.named<JavaExec>("run") {
    if (providers.gradleProperty("macosIme").getOrElse("false").toBoolean()) {
        dependsOn(buildMacOsIme)
        systemProperty("beatoraja.macosIme", "true")
        systemProperty("beatoraja.macosIme.library", macOsImeLibrary.get().asFile.absolutePath)
    }
}
distributions {
    main {
        contents {
            from(projectDir) { include(distributionFiles) }
            from(prepareRuntimeDirectories)
            from(generateAudioBackendTestAssets) { into("audio-test") }
            if (System.getProperty("os.name").startsWith("Mac")) from(buildMacOsIme) { into("natives") }
        }
    }
}
tasks.named<Zip>("distZip") {
    dependsOn(verifyBuildEnvironment)
}

val distributionSmokeTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Verify installed JARs/resources and start a Java 17 packaged-runtime probe."
    dependsOn(tasks.installDist, tasks.distZip, tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.DistributionSmokeTest")
    args(layout.buildDirectory.dir("install/beatoraja").get().asFile.absolutePath,
        sourceSets.test.get().output.classesDirs.asPath, java17.get().executablePath.asFile.absolutePath,
        tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath)
    workingDir(projectDir)
}
tasks.check { dependsOn(distributionSmokeTest) }

tasks.register<JavaExec>("packagedZipSmokeTest") {
    group = "verification"
    description = "Extract the distribution ZIP and verify native loading/exit using only its packaged classpath."
    dependsOn(tasks.distZip, tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.PackagedZipSmokeTest")
    args(tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath,
        java17.get().executablePath.asFile.absolutePath,
        layout.buildDirectory.dir("packaged-smoke").get().asFile.absolutePath)
    timeout.set(Duration.ofMinutes(3))
}

tasks.register<JavaExec>("lwjgl3BgaSkinIntegrationTest") {
    group = "verification"
    description = "Verify real FFmpeg/GL pixels, skin clipping/fonts and repeated resource disposal with generated video."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.video.Lwjgl3BgaSkinIntegrationTest")
    timeout.set(Duration.ofMinutes(10))
    args(layout.buildDirectory.dir("gl-integration").get().asFile.absolutePath)
    systemProperty("beatoraja.glIntegration.cycles", providers.gradleProperty("glIntegrationCycles").getOrElse("8"))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("ffmpegDecoderIsolationTest") {
    group = "verification"
    description = "Isolate RAW decode, legacy COLOR and aligned color conversion in separate JVMs."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.video.FFmpegDecoderIsolationTest")
    timeout.set(Duration.ofMinutes(10))
    args(layout.buildDirectory.dir("decoder-isolation").get().asFile.absolutePath)
    systemProperty("beatoraja.decoder.unsafeProbes", providers.gradleProperty("decoderUnsafeProbes").getOrElse("false"))
    systemProperty("beatoraja.decoder.width", providers.gradleProperty("decoderWidth").getOrElse("0"))
}

tasks.register<JavaExec>("ffmpegFrameConverterTest") {
    group = "verification"
    description = "Verify aligned BGR conversion layouts/formats without a GL context."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.video.BgrFrameConverterTest")
    timeout.set(Duration.ofMinutes(3))
}

tasks.register<JavaExec>("lwjgl3StateEnduranceTest") {
    group = "verification"
    description = "Stress synthetic SELECT/PLAY/RESULT transitions with real GL/video/fonts and memory/timing CSVs."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.video.Lwjgl3StateEnduranceTest")
    maxHeapSize = "768m"
    timeout.set(Duration.ofMinutes(providers.gradleProperty("enduranceTimeoutMinutes").getOrElse("120").toLong().coerceIn(1, 1440)))
    args(providers.gradleProperty("enduranceOutput").getOrElse(layout.buildDirectory.dir("state-endurance").get().asFile.absolutePath))
    systemProperty("beatoraja.endurance.cycles", providers.gradleProperty("enduranceCycles").getOrElse("20"))
    systemProperty("beatoraja.endurance.warmup", providers.gradleProperty("enduranceWarmup").getOrElse("3"))
    systemProperty("beatoraja.endurance.seconds", providers.gradleProperty("enduranceSeconds").getOrElse("0"))
    systemProperty("beatoraja.endurance.settleFrames", providers.gradleProperty("enduranceSettleFrames").getOrElse("4"))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3StateEnduranceFailureTest") {
    group = "verification"
    description = "Verify missing-frame detection, failure screenshot and resource cleanup with injected rendering loss."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.video.Lwjgl3StateEnduranceTest")
    maxHeapSize = "768m"
    timeout.set(Duration.ofMinutes(3))
    args(layout.buildDirectory.dir("state-endurance-failure").get().asFile.absolutePath)
    systemProperty("beatoraja.endurance.cycles", "2")
    systemProperty("beatoraja.endurance.warmup", "0")
    systemProperty("beatoraja.endurance.injectMissingFrame", "true")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3StageFramebufferTest") {
    group = "verification"
    description = "Verify Scene2D text hitboxes and nested preview framebuffer/scissor state across resizing."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.skin.Lwjgl3StageFramebufferTest")
    timeout.set(Duration.ofMinutes(3))
    systemProperty("beatoraja.viewportTest.fullscreen", providers.gradleProperty("viewportFullscreen").getOrElse("false"))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3TextInputTest") {
    group = "verification"
    description = "Verify Unicode callbacks, text focus/commit and isolated clipboard shortcuts in real GLFW."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.skin.Lwjgl3TextInputTest")
    timeout.set(Duration.ofMinutes(3))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}
tasks.register<JavaExec>("macOsImeIntegrationTest") {
    group = "verification"
    description = "Exercise real AppKit NSTextView/IME JNI callbacks with synthetic composition, not a real OS IME."
    onlyIf { System.getProperty("os.name").startsWith("Mac") }
    dependsOn(tasks.testClasses, buildMacOsImeTest)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.skin.Lwjgl3TextInputTest")
    systemProperty("beatoraja.test.macosIme", "true")
    systemProperty("beatoraja.macosIme.library", macOsImeTestLibrary.get().asFile.absolutePath)
    timeout.set(Duration.ofMinutes(3))
    jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3FramePacingSmokeTest") {
    group = "verification"
    description = "Measure real GLFW frames and input publication around a synthetic render stall."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3FramePacingSmokeTest")
    systemProperty("beatoraja.frameTest.vsync", providers.gradleProperty("frameTestVsync").getOrElse("false"))
    systemProperty("beatoraja.frameTest.fps", providers.gradleProperty("frameTestFps").getOrElse("60"))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3AudioDiagnosticsSmokeTest") {
    group = "verification"
    description = "Verify Sound/AudioDevice diagnostics with real OpenAL and silent PCM at 44100/48000 Hz."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.audio.Lwjgl3AudioDiagnosticsSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3WindowSmokeTest") {
    group = "verification"
    description = "Open an isolated LWJGL3 test window and verify real GL pixels and resizing."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3WindowSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3HiDpiSmokeTest") {
    group = "verification"
    description = "Verify logical coordinates, clipping pixels and repeated resizes in a real GLFW window."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3HiDpiSmokeTest")
    systemProperty("beatoraja.windowDiagnostics", "true")
    systemProperty("beatoraja.hidpiTest.fullscreen", providers.gradleProperty("hidpiTestFullscreen").getOrElse("false"))
    systemProperty("beatoraja.hidpiTest.startFullscreen", providers.gradleProperty("hidpiTestStartFullscreen").getOrElse("false"))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3LauncherSmokeTest") {
    group = "verification"
    description = "Verify JavaFX Launcher and a separate real LWJGL3 game process."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3LauncherProcessSmokeTest")
}

tasks.register<JavaExec>("lwjgl3InputDiagnosticsSmokeTest") {
    group = "verification"
    description = "Open an isolated GLFW window to verify opt-in input callback diagnostics."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.Lwjgl3InputDiagnosticsSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3KeyboardQueueSmokeTest") {
    group = "verification"
    description = "Verify queued keyboard edges and poll timestamps through real GLFW callbacks."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.Lwjgl3KeyboardQueueSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3InputShutdownSmokeTest") {
    group = "verification"
    description = "Verify input worker shutdown before controller and GLFW window disposal."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.Lwjgl3InputShutdownSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3WindowInputLifecycleTest") {
    group = "verification"
    description = "Verify focus loss, minimization and resume across keyboard/controller/mouse input."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.Lwjgl3WindowInputLifecycleTest")
    systemProperty("beatoraja.inputDiagnostics", "true")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3MouseScratchDiagnosticsSmokeTest") {
    group = "verification"
    description = "Verify mouse scratch observations through real GLFW callbacks and worker consumption."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.Lwjgl3MouseScratchDiagnosticsSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3MouseScratchQueueSmokeTest") {
    group = "verification"
    description = "Verify sampled mouse motion FIFO through real GLFW and the mapped judgment stream."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.Lwjgl3MouseScratchQueueSmokeTest")
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("lwjgl3MouseScratchGeometrySmokeTest") {
    group = "verification"
    description = "Verify scratch center rebasing through real GLFW resize, position and optional fullscreen transitions."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3MouseScratchGeometrySmokeTest")
    systemProperty("beatoraja.hidpiTest.fullscreen", providers.gradleProperty("hidpiTestFullscreen").getOrElse("false"))
    providers.gradleProperty("inputDiagnostics").orNull?.let { systemProperty("beatoraja.inputDiagnostics", it) }
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}
