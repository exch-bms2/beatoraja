import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import java.time.Duration
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.Locale

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
// A deliberate build-time trust decision, never inferred from arbitrary lib/ or ir/ contents.
val includeTrustedIr = providers.gradleProperty("includeTrustedIr").map { it.toBooleanStrict() }.getOrElse(false)
val trustedIrJars = fileTree("trusted_ir") { include("*.jar", "*.JAR") }.files.sortedBy { it.name }
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
    // Override libGDX 1.14.2's original LWJGL version; keep all Java/native modules aligned.
    for (module in listOf("lwjgl", "lwjgl-glfw", "lwjgl-jemalloc", "lwjgl-openal", "lwjgl-opengl", "lwjgl-stb")) {
        implementation("org.lwjgl:$module:3.4.3") { isTransitive = false }
        for (platform in listOf("windows", "linux", "macos", "macos-arm64")) {
            runtimeOnly("org.lwjgl:$module:3.4.3:natives-$platform") { isTransitive = false }
        }
    }
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop") { isTransitive = false }
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-desktop") { isTransitive = false }
    // JInput remains independent from the rendering backend.
    for (platform in listOf("linux", "windows", "osx")) {
        runtimeOnly("net.java.jinput:jinput-platform:2.0.5:natives-$platform") { isTransitive = false }
    }
    // Compress requires Commons IO, Codec and Lang3; resolve its non-optional dependencies.
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("commons-dbutils:commons-dbutils:1.8.1")
    // SQLite's optional GraalVM/SLF4J dependencies are not needed; keep a single SLF4J API, without a provider.
    implementation("org.xerial:sqlite-jdbc:3.54.1.0") { isTransitive = false }
    runtimeOnly("org.slf4j:slf4j-api:2.0.20") { isTransitive = false }
    implementation("com.github.junrar:junrar:8.1.2")
    // Select only FFmpeg, not JavaCV's camera/OpenCV dependencies or the platform POMs' full native set.
    implementation("org.bytedeco:javacpp:1.5.14") { isTransitive = false }
    implementation("org.bytedeco:javacv:1.5.14") { isTransitive = false }
    runtimeOnly("org.bytedeco:javacv-platform:1.5.14") { isTransitive = false }
    implementation("org.bytedeco:ffmpeg:8.1.2-1.5.14") { isTransitive = false }
    runtimeOnly("org.bytedeco:ffmpeg-platform:8.1.2-1.5.14") { isTransitive = false }
    // Match the LWJGL3 desktop targets; JavaCPP now also needs its own JNI native JARs.
    for (classifier in listOf("linux-x86_64", "macosx-x86_64", "macosx-arm64", "windows-x86_64")) {
        runtimeOnly("org.bytedeco:javacpp:1.5.14:$classifier") { isTransitive = false }
        runtimeOnly("org.bytedeco:ffmpeg:8.1.2-1.5.14:$classifier") { isTransitive = false }
    }
}

val java17 = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }
val verifyBuildEnvironment by tasks.registering {
    group = "verification"
    description = "Verify the existing local libraries and JavaFX-enabled JDK 17."
    doLast {
        val missing = localLibraries.filterNot { it.isFile }
        check(missing.isEmpty()) { "Missing bundled JARs: ${missing.joinToString()}. Restore lib/ before building." }
        if (includeTrustedIr) {
            check(trustedIrJars.isNotEmpty()) { "-PincludeTrustedIr=true requires JARs in trusted_ir/" }
            check(trustedIrJars.size <= 128 && trustedIrJars.map { it.name.lowercase(Locale.ROOT) }.distinct().size == trustedIrJars.size) {
                "Too many or duplicate trusted IR filenames"
            }
            for (jar in trustedIrJars) check(Files.isRegularFile(jar.toPath(), LinkOption.NOFOLLOW_LINKS)
                    && jar.name.matches(Regex("[A-Za-z0-9][A-Za-z0-9._ ()-]{0,123}\\.[jJ][aA][rR]"))) {
                "Invalid trusted IR input (regular file with portable name required): $jar"
            }
        }
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
    if (includeTrustedIr) systemProperty("beatoraja.ir.classpath", trustedIrJars.joinToString(File.pathSeparator) { it.absolutePath })
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

// Separate build tooling: neither ordinary distributions nor fatJar require test/.
val fatJarTools = sourceSets.create("fatJarTools") {
    java.setSrcDirs(listOf("gradle/fat-jar"))
    resources.setSrcDirs(emptyList<String>())
}
dependencies { testImplementation(fatJarTools.output) }
val prepareFatJarContent by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Merge the explicit runtime dependencies using Java 17 class/resource precedence."
    dependsOn(tasks.classes, tasks.named(fatJarTools.classesTaskName))
    classpath = fatJarTools.runtimeClasspath
    mainClass.set("bms.build.FatJarContent")
    val staging = layout.buildDirectory.dir("fat-jar-content")
    inputs.files(sourceSets.main.get().output, configurations.runtimeClasspath)
    inputs.property("runtimeOrder", provider { configurations.runtimeClasspath.get().files.map { it.name } })
    inputs.property("includeTrustedIr", includeTrustedIr)
    if (includeTrustedIr) inputs.files(trustedIrJars)
    outputs.dir(staging)
    args(staging.get().asFile.absolutePath, file("ir").absolutePath)
    doFirst {
        delete(staging)
        setArgs(listOf(staging.get().asFile.absolutePath, file("ir").absolutePath)
            + sourceSets.main.get().output.files.map { it.absolutePath }
            + "--dependencies" + configurations.runtimeClasspath.get().files.map { it.absolutePath }
            + if (includeTrustedIr) listOf("--trusted-ir") + trustedIrJars.map { it.absolutePath } else emptyList())
    }
}
val fatJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Build an executable all-dependencies JAR; external skins/natives/JRE remain required."
    archiveClassifier.set("all")
    from(prepareFatJarContent)
    from("LICENSE") { into("META-INF/beatoraja") }
    duplicatesStrategy = DuplicatesStrategy.FAIL
    manifest.attributes("Main-Class" to application.mainClass.get())
}

// The existing regression tests are standalone Java programs, not JUnit tests.
val regressionClasses = listOf(
    "bms.player.beatoraja.build.CommonsCompatibilityTest",
    "bms.player.beatoraja.build.DatabaseRuntimeCompatibilityTest",
    "bms.player.beatoraja.build.LwjglRuntimeCompatibilityTest",
    "bms.player.beatoraja.ir.IsolatedIRConnectionTest",
    "bms.player.beatoraja.select.IRTableLoaderTest",
    "bms.player.beatoraja.select.PreviewMusicProcessorTest",
    "bms.player.beatoraja.result.IRSubmissionRetryTest",
    "bms.player.beatoraja.build.PackagedGameFixtureTest",
    "bms.player.beatoraja.build.GlTestDiagnosticsTest",
    "bms.player.beatoraja.MemoryDiagnosticsTest",
    "bms.player.beatoraja.video.EnduranceStatisticsTest",
    "bms.player.beatoraja.skin.SkinTextClipboardTest",
    "bms.player.beatoraja.input.TextEditingControlTest",
    "bms.player.beatoraja.input.TextInputSessionTest",
    "bms.player.beatoraja.input.MacOsImeBridgeTest",
    "bms.player.beatoraja.input.WindowsImeBridgeTest",
    "bms.player.beatoraja.input.X11ImeBridgeTest",
    "bms.player.beatoraja.skin.StageViewportTest",
    "bms.player.beatoraja.input.WindowInputLifecycleTest",
    "bms.player.beatoraja.input.InputShutdownTest",
    "bms.player.beatoraja.input.MidiInputLifecycleTest",
    "bms.player.beatoraja.video.VideoFrameCopyTest",
    "bms.player.beatoraja.FramePacingDiagnosticsTest",
    "bms.player.beatoraja.audio.AudioBackendLifecycleTest",
    "bms.player.beatoraja.audio.AudioDiagnosticsTest",
    "bms.player.beatoraja.audio.AudioBackendTestAssetsTest",
    "bms.player.beatoraja.build.AudioTestDistributionTest",
    "bms.player.beatoraja.build.FatJarContentTest",
    "bms.player.beatoraja.input.MouseScratchGeometryTest",
    "bms.player.beatoraja.WindowModeControllerTest",
    "bms.player.beatoraja.StartupMonitorTest",
    "bms.player.beatoraja.SkinProjectionTest",
    "bms.player.beatoraja.skin.SkinObjectOffsetCenterTest",
    "bms.player.beatoraja.skin.json.OffsetCenterLoaderTest",
    "bms.player.beatoraja.play.NoteOffsetCenterTest",
    "bms.player.beatoraja.input.WindowDiagnosticsTest",
    "bms.player.beatoraja.input.WaylandCompatibilityTest",
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
    "bms.player.beatoraja.ir.IRWorkerSecurityTest",
    "bms.player.beatoraja.ir.BundledIRIntegrationTest",
    "bms.player.beatoraja.ir.BundledIRTest",
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
    "bms.player.beatoraja.input.ControllerBackendTest",
    "bms.player.beatoraja.input.SdlControllerManagerTest",
    "bms.player.beatoraja.skin.lua.MainStateInputLuaApiTest",
    "bms.player.beatoraja.skin.lua.SkinLuaIoSymlinkTest",
    "bms.player.beatoraja.skin.lua.SkinCompatibilityApiTest",
    "bms.player.beatoraja.skin.lua.SkinHttpLimitsTest",
    "bms.player.beatoraja.song.archive.SongArchivesTest",
    "bms.player.beatoraja.song.archive.ZipSongArchiveTest",
    "bms.player.beatoraja.song.archive.SongArchiveDepthTest",
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
val includeAudioTestAssets = providers.gradleProperty("includeAudioTestAssets")
    .map { it.toBooleanStrict() }.getOrElse(false)
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
val x11ImeDirectory = layout.buildDirectory.dir("generated/x11-ime")
val buildX11Ime by tasks.registering(Exec::class) {
    group = "build"
    description = "Build opt-in X11-only GLFW/JNI prototype on Linux (requires CMake, X11 headers, Python and curl)."
    onlyIf { System.getProperty("os.name").startsWith("Linux") }
    inputs.files("native/linux/x11-ime.c", "scripts/prepare-x11-ime.py", "scripts/build-x11-ime.sh")
    outputs.dir(x11ImeDirectory)
    environment("JAVA_HOME", java17.get().metadata.installationPath.asFile.absolutePath)
    commandLine("bash", file("scripts/build-x11-ime.sh").absolutePath, x11ImeDirectory.get().asFile.absolutePath)
}
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
    if (providers.gradleProperty("x11Ime").getOrElse("false").toBoolean()) {
        check(System.getProperty("os.name").startsWith("Linux")) { "-Px11Ime=true requires Linux/X11" }
        dependsOn(buildX11Ime)
        val library = x11ImeDirectory.get().file("libglfw-beatoraja-x11-ime.so").asFile.absolutePath
        systemProperty("beatoraja.x11Ime", "true")
        systemProperty("beatoraja.x11Ime.library", library)
        systemProperty("org.lwjgl.glfw.libname", library)
    }
    if (providers.gradleProperty("windowsIme").getOrElse("false").toBoolean()) {
        systemProperty("beatoraja.windowsIme", "true")
    }
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
            if (includeTrustedIr) from(trustedIrJars) { into("ir") }
            if (includeAudioTestAssets) from(generateAudioBackendTestAssets) { into("audio-test") }
            if (System.getProperty("os.name").startsWith("Mac")) from(buildMacOsIme) { into("natives") }
            if (providers.gradleProperty("x11Ime").getOrElse("false").toBoolean()) {
                check(System.getProperty("os.name").startsWith("Linux")) { "X11 IME distribution must be built on Linux" }
                from(buildX11Ime) { into("natives") }
            }
        }
    }
}
tasks.named<Zip>("distZip") {
    dependsOn(verifyBuildEnvironment)
}

val verifyDistributionTaskGraph by tasks.registering {
    group = "verification"
    description = "Verify that ordinary distributions do not depend on test compilation or audio fixture generation."
    doLast {
        for (name in listOf("distZip", "distTar", "installDist")) {
            val dependencies = linkedSetOf<Task>()
            fun visit(task: Task) {
                if (dependencies.add(task)) task.taskDependencies.getDependencies(task).forEach(::visit)
            }
            visit(tasks.getByName(name))
            val names = dependencies.map { it.name }.toSet()
            check(("generateAudioBackendTestAssets" in names) == includeAudioTestAssets) {
                "$name audio fixture dependency does not match includeAudioTestAssets=$includeAudioTestAssets"
            }
            check(("testClasses" in names) == includeAudioTestAssets) {
                "$name test compilation dependency does not match includeAudioTestAssets=$includeAudioTestAssets"
            }
            if (!includeAudioTestAssets) {
                check(names.intersect(setOf("compileTestJava", "processTestResources", "test", "regressionTest")).isEmpty()) {
                    "$name unexpectedly depends on test tasks: $names"
                }
            }
        }
        val fatDependencies = linkedSetOf<Task>()
        fun visitFat(task: Task) {
            if (fatDependencies.add(task)) task.taskDependencies.getDependencies(task).forEach(::visitFat)
        }
        visitFat(fatJar.get())
        check(fatDependencies.none { it.name in setOf("testClasses", "compileTestJava", "test", "regressionTest") }) {
            "fatJar must not depend on test/"
        }
        logger.lifecycle("Distribution task graphs verified (includeAudioTestAssets=$includeAudioTestAssets)")
    }
}

val distributionSmokeTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Verify installed JARs/resources and start a Java 17 packaged-runtime probe."
    dependsOn(tasks.installDist, tasks.distZip, tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.DistributionSmokeTest")
    args(layout.buildDirectory.dir("install/beatoraja").get().asFile.absolutePath,
        sourceSets.test.get().output.classesDirs.asPath, java17.get().executablePath.asFile.absolutePath,
        tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath,
        includeAudioTestAssets.toString())
    workingDir(projectDir)
}
tasks.check { dependsOn(distributionSmokeTest, verifyDistributionTaskGraph) }

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

// Optional CI diagnostics never change the normal headless build or application runtime.
val packagedGameObserverJar = tasks.register<Jar>("packagedGameObserverJar") {
    dependsOn(tasks.testClasses)
    archiveFileName.set("packaged-game-test-agent.jar")
    destinationDirectory.set(layout.buildDirectory.dir("test-agents"))
    from(sourceSets.test.get().output)
    include("bms/player/beatoraja/build/PackagedGameAgent*.class", "bms/player/beatoraja/build/PackagedGameSupport.class")
    manifest.attributes("Premain-Class" to "bms.player.beatoraja.build.PackagedGameAgent")
}
tasks.register<JavaExec>("fatJarSmokeTest") {
    group = "verification"
    description = "Verify merged dependency/native bytes, java -jar native startup and real IR worker isolation."
    dependsOn(fatJar, tasks.distZip, tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.FatJarSmokeTest")
    args(fatJar.get().archiveFile.get().asFile.absolutePath,
        tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath,
        java17.get().executablePath.asFile.absolutePath,
        layout.buildDirectory.dir("fat-jar-smoke").get().asFile.absolutePath)
    doFirst {
        args(configurations.runtimeClasspath.get().files.map { it.absolutePath })
        args("--trusted-ir")
        if (includeTrustedIr) args(trustedIrJars.map { it.absolutePath })
    }
    timeout.set(Duration.ofMinutes(5))
}
tasks.register<JavaExec>("fatJarGameIntegrationTest") {
    systemProperty("beatoraja.packagedGameTest.controllerBackend", providers.gradleProperty("controllerBackendTest").getOrElse("JINPUT"))
    group = "verification"
    description = "Real JavaFX Launcher -> GLFW game -> exit, with only a Fat JAR on the production classpath."
    dependsOn(fatJar, tasks.distZip, tasks.testClasses, packagedGameObserverJar)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.PackagedGameIntegrationTest")
    args(tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath,
        java17.get().executablePath.asFile.absolutePath, packagedGameObserverJar.get().archiveFile.get().asFile.absolutePath,
        layout.buildDirectory.dir("fat-jar-game").get().asFile.absolutePath,
        fatJar.get().archiveFile.get().asFile.absolutePath)
    timeout.set(Duration.ofMinutes(5))
}
tasks.register<JavaExec>("packagedGameIntegrationTest") {
    systemProperty("beatoraja.packagedGameTest.controllerBackend", providers.gradleProperty("controllerBackendTest").getOrElse("JINPUT"))
    group = "verification"
    description = "Real extracted-ZIP JavaFX START, MainController gameplay, input shutdown and child-process exit."
    dependsOn(tasks.distZip, tasks.testClasses, packagedGameObserverJar)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.PackagedGameIntegrationTest")
    args(tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath,
        java17.get().executablePath.asFile.absolutePath, packagedGameObserverJar.get().archiveFile.get().asFile.absolutePath,
        layout.buildDirectory.dir("packaged-game").get().asFile.absolutePath)
    timeout.set(Duration.ofMinutes(5))
}
val glTestOutput = providers.gradleProperty("glTestOutput").getOrElse("build-gradle/ci/gl")
tasks.register<JavaExec>("glDiagnosticFailureTest") {
    group = "verification"
    description = "Verify failure PNG/trace capture and exception preservation using a real GL context."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.GlTestDiagnosticsFailureTest")
    systemProperty("beatoraja.glTest.output", file(glTestOutput).absolutePath)
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}
val glIntegrationTasks = listOf("lwjgl3BgaSkinIntegrationTest", "lwjgl3StageFramebufferTest",
    "lwjgl3TextInputTest", "lwjgl3WindowInputLifecycleTest", "lwjgl3StateEnduranceTest",
    "lwjgl3StateEnduranceFailureTest", "glDiagnosticFailureTest", "packagedGameIntegrationTest")
tasks.withType<JavaExec>().configureEach {
    if (name in glIntegrationTasks) {
        if (providers.gradleProperty("glTestOutput").isPresent) {
            val output = file(glTestOutput)
            systemProperty("beatoraja.glTest.output", output.absolutePath)
            jvmArgs("-XX:ErrorFile=${output.resolve("hs_err_pid%p.log").absolutePath}")
            doFirst { output.mkdirs() }
        }
        systemProperty("beatoraja.glTest.requireMesa", providers.gradleProperty("glTestRequireMesa").getOrElse("false"))
        // Explicit timeouts keep a stuck native window from blocking the entire CI job.
        if (name in listOf("lwjgl3WindowInputLifecycleTest", "glDiagnosticFailureTest")) timeout.set(Duration.ofMinutes(3))
    }
}
tasks.register("linuxGlIntegrationTest") {
    group = "verification"
    description = "Optional GL integration suite; CI runs this under Linux Xvfb/Mesa, never as part of build."
    dependsOn(glIntegrationTasks)
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
tasks.register<JavaExec>("windowsImeCallbackTest") {
    group = "verification"
    description = "Check six-argument SUBCLASSPROC FFI marshaling on the host ABI (not Windows IME)."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.WindowsImeCallbackTest")
}
val x11TestSupported = System.getProperty("os.name").startsWith("Linux") ||
    (System.getProperty("os.name").startsWith("Mac") && providers.gradleProperty("x11Headers").isPresent)
val x11ImeTestLibrary = layout.buildDirectory.file("x11-ime-test/" +
    if (System.getProperty("os.name").startsWith("Mac")) "libbeatoraja-x11-ime-test.dylib" else "libbeatoraja-x11-ime-test.so")
val buildX11ImeTest by tasks.registering(Exec::class) {
    group = "verification"
    description = "Compile real Xlib/JNI declarations with fake X server calls; not a Linux runtime test."
    onlyIf { x11TestSupported }
    inputs.files("native/linux/x11-ime.c", "test/native/linux/x11-ime-test.c")
    inputs.property("x11Headers", providers.gradleProperty("x11Headers").getOrElse(""))
    outputs.file(x11ImeTestLibrary)
    doFirst { x11ImeTestLibrary.get().asFile.parentFile.mkdirs() }
    val home = java17.get().metadata.installationPath.asFile
    val mac = System.getProperty("os.name").startsWith("Mac")
    val includes = providers.gradleProperty("x11Headers").getOrElse("").split(File.pathSeparator)
        .filter { it.isNotEmpty() }.map { "-I$it" }
    commandLine(listOf("cc", "-std=c99", "-D_POSIX_C_SOURCE=200809L", "-Wall", "-Wextra", "-Werror=implicit-function-declaration") +
        (if (mac) listOf("-dynamiclib") else listOf("-shared", "-fPIC")) + includes +
        listOf("-I$home/include", "-I$home/include/" + if (mac) "darwin" else "linux",
            file("test/native/linux/x11-ime-test.c").absolutePath, "-o", x11ImeTestLibrary.get().asFile.absolutePath))
}
tasks.register<JavaExec>("x11ImeNativeTest") {
    group = "verification"
    description = "Exercise production XIM C callbacks/JNI with a fake server, never a real IME."
    onlyIf { x11TestSupported }
    dependsOn(tasks.testClasses, buildX11ImeTest)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.X11ImeNativeTest")
    args(x11ImeTestLibrary.get().asFile.absolutePath)
}
tasks.register<Exec>("x11ImePatchTest") {
    group = "verification"
    description = "Check guarded GLFW source hooks without downloading GLFW or requiring a display."
    commandLine("python3", file("test/native/linux/test-prepare.py").absolutePath)
}
val x11ImeArtifactInspectorTest = tasks.register<JavaExec>("x11ImeArtifactInspectorTest") {
    group = "verification"
    description = "Test native-artifact validation using synthetic fixtures on every OS."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.X11ImeArtifactTest")
    args("--self-test")
}
regressionTest { dependsOn(x11ImeArtifactInspectorTest) }
tasks.register<JavaExec>("x11ImeArtifactTest") {
    group = "verification"
    description = "Linux x64 ELF/exports/dependencies and extracted optional-ZIP JNI ABI verification, no display."
    onlyIf { System.getProperty("os.name").startsWith("Linux") }
    dependsOn(tasks.testClasses, buildX11Ime, distributionSmokeTest)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.build.X11ImeArtifactTest")
    args(x11ImeDirectory.get().asFile.absolutePath,
        tasks.named<Zip>("distZip").get().archiveFile.get().asFile.absolutePath,
        java17.get().executablePath.asFile.absolutePath,
        layout.buildDirectory.dir("x11-artifact-check").get().asFile.absolutePath)
    doFirst { check(providers.gradleProperty("x11Ime").getOrElse("false").toBoolean()) { "Use -Px11Ime=true to verify the opt-in ZIP" } }
    timeout.set(Duration.ofMinutes(3))
}
tasks.register<JavaExec>("x11ImeWindowTest") {
    group = "verification"
    description = "Opt-in real Linux/X11 window/XIC lifetime smoke; not Japanese conversion automation."
    val enabled = System.getProperty("os.name").startsWith("Linux") && providers.gradleProperty("x11Ime").getOrElse("false").toBoolean()
    onlyIf { enabled }
    dependsOn(tasks.testClasses)
    if (enabled) {
        dependsOn(buildX11Ime)
        val library = x11ImeDirectory.get().file("libglfw-beatoraja-x11-ime.so").asFile.absolutePath
        systemProperty("beatoraja.x11Ime", "true")
        systemProperty("beatoraja.x11Ime.library", library)
        systemProperty("org.lwjgl.glfw.libname", library)
    }
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.X11ImeWindowTest")
    timeout.set(Duration.ofMinutes(3))
}
tasks.register<JavaExec>("windowsImeWindowTest") {
    group = "verification"
    description = "Windows-only synthetic HWND/IMM smoke test; does not automate a Japanese input source."
    onlyIf { System.getProperty("os.name").startsWith("Windows") }
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.input.WindowsImeWindowTest")
    timeout.set(Duration.ofMinutes(3))
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

tasks.register<JavaExec>("lwjgl3StartupMonitorSmokeTest") {
    group = "verification"
    description = "Verify explicit startup monitor selection, logical window size and optional fullscreen restoration."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3StartupMonitorSmokeTest")
    providers.gradleProperty("monitorTestIndex").orNull?.let { systemProperty("beatoraja.monitorTest.index", it) }
    systemProperty("beatoraja.monitorTest.fullscreen", providers.gradleProperty("monitorTestFullscreen").getOrElse("false"))
    systemProperty("beatoraja.monitorTest.startFullscreen", providers.gradleProperty("monitorTestStartFullscreen").getOrElse("false"))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
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
tasks.register<JavaExec>("lwjgl3WaylandPolicyTest") {
    group = "verification"
    description = "Real GL with the Wayland input policy and synthetic cursor endpoints, not a compositor test."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3WaylandCompatibilityTest")
    systemProperty("beatoraja.windowDiagnostics", "true")
    systemProperty("beatoraja.inputDiagnostics", "true")
    timeout.set(Duration.ofMinutes(3))
    if (System.getProperty("os.name").startsWith("Mac")) jvmArgs("-XstartOnFirstThread")
}
tasks.register<JavaExec>("nativeWaylandWindowTest") {
    group = "verification"
    description = "Linux-only native Wayland window/input smoke, requiring an actual compositor."
    onlyIf { System.getProperty("os.name").startsWith("Linux") }
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3WaylandCompatibilityTest")
    systemProperty("beatoraja.test.nativeWayland", "true")
    systemProperty("beatoraja.test.waylandFullscreen", providers.gradleProperty("waylandFullscreen").getOrElse("false"))
    systemProperty("beatoraja.windowDiagnostics", "true")
    systemProperty("beatoraja.inputDiagnostics", "true")
    timeout.set(Duration.ofMinutes(3))
}
