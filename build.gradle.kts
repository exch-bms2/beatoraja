import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

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
    providers.gradleProperty("inputDiagnostics").orNull?.let {
        systemProperty("beatoraja.inputDiagnostics", it)
    }
    providers.gradleProperty("inputDiagnosticsIntervalMillis").orNull?.let {
        systemProperty("beatoraja.inputDiagnostics.intervalMillis", it)
    }
}
tasks.named<Jar>("jar") {
    manifest.attributes("Main-Class" to application.mainClass.get())
}

// The existing regression tests are standalone Java programs, not JUnit tests.
val regressionClasses = listOf(
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
distributions {
    main {
        contents {
            from(projectDir) { include(distributionFiles) }
            from(prepareRuntimeDirectories)
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

tasks.register<JavaExec>("lwjgl3WindowSmokeTest") {
    group = "verification"
    description = "Open an isolated LWJGL3 test window and verify real GL pixels and resizing."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("bms.player.beatoraja.Lwjgl3WindowSmokeTest")
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
