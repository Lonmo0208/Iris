plugins {
    id("idea")
    id("net.neoforged.moddev") version "2.0.28-beta"
    id("java-library")
}

val MINECRAFT_VERSION: String by rootProject.extra
val PARCHMENT_VERSION: String? by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
val SODIUM_DEPENDENCY_NEO: Any by rootProject.extra
val EMB_DEPENDENCY: Any by rootProject.extra
val MOD_VERSION: String by rootProject.extra

base {
    archivesName = "iris-neoforge"
}

sourceSets {
}

repositories {
    maven("https://maven.rover656.dev/releases")
    maven("https://cursemaven.com")
    maven("https://modmaven.dev/")
    maven("https://maven.taumc.org/releases")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

tasks.jar {

    from(rootDir.resolve("LICENSE.md"))

    filesMatching("neoforge.mods.toml") {
        expand(mapOf("version" to MOD_VERSION))
    }

    manifest.attributes["Main-Class"] = "net.irisshaders.iris.LaunchWarn"
}

// NeoGradle compiles the game, but we don't want to add our common code to the game's code
val notNeoTask: (Task) -> Boolean = { it: Task -> !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

tasks.withType<JavaCompile>().matching(notNeoTask).configureEach {
    source(project(":common").sourceSets.main.get().allSource)
    source(project(":common").sourceSets.getByName("vendored").allSource)
    source(project(":common").sourceSets.getByName("api").allSource)
    source(project(":common").sourceSets.getByName("desktop").allSource)
}

tasks.withType<Javadoc>().matching(notNeoTask).configureEach {
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.withType<ProcessResources>().matching(notNeoTask).configureEach {
    from(project(":common").sourceSets.main.get().resources)
}

tasks.jar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")

neoForge {
    // Specify the version of NeoForge to use.
    version = NEOFORGE_VERSION

    if (PARCHMENT_VERSION != null) {
        parchment {
            minecraftVersion = MINECRAFT_VERSION
            mappingsVersion = PARCHMENT_VERSION
        }
    }

    runs {
        create("client") {
            client()
            environment("LD_PRELOAD", "/usr/lib/librenderdoc.so")
        }
    }

    mods {
        create("sodium") {
            sourceSet(sourceSets.main.get())
        }
    }
}

fun includeDep(dependency:ConfigurableFileCollection){
    includeDep(dependency)
    dependencies.additionalRuntimeClasspath(dependency)
}

fun includeDep(dependency: String, closure: Action<ExternalModuleDependency>) {
    dependencies.implementation(dependency, closure)
    dependencies.jarJar(dependency, closure)
}

fun includeDep(dependency: String) {
    dependencies.implementation(dependency)
    dependencies.jarJar(dependency)
}

fun includeAdditional(dependency: String) {
    includeDep(dependency)
    dependencies.additionalRuntimeClasspath(dependency)
}

tasks.named("compileTestJava").configure {
    enabled = false
}

dependencies {
    implementation(files(rootDir.resolve("acceleratedrendering-1.0.0.jar")))
    compileOnly(files(rootDir.resolve("DHApi.jar")))

    compileOnly(project.project(":common").sourceSets.main.get().output)
    compileOnly(project.project(":common").sourceSets.getByName("vendored").output)
    compileOnly(project.project(":common").sourceSets.getByName("headers").output)
    compileOnly(project.project(":common").sourceSets.getByName("api").output)
    includeDep("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308ded19")
    includeDep("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:3.4.0+acb05a3919")
    includeDep("org.sinytra.forgified-fabric-api:fabric-rendering-data-attachment-v1:0.3.48+73761d2e19")
    includeDep("org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:1.0.10+9afaaf8c19")
    //compileOnly (SODIUM_DEPENDENCY_NEO)
    implementation(EMB_DEPENDENCY)
    //implementation(files(rootDir.resolve("sodium-api.jar")))
    includeAdditional("io.github.douira:glsl-transformer:2.0.1")
    includeAdditional("org.anarres:jcpp:1.4.14")
    includeAdditional("org.taumc:glsl-transformation-lib:0.2.0-20.ge3cb096")
    jarJar(implementation("org.taumc:glsl-transformation-lib:0.2.0-20.ge3cb096")) {
    }
    additionalRuntimeClasspath("org.taumc:glsl-transformation-lib:0.2.0-20.ge3cb096") {
    }
    compileOnly("mekanism:Mekanism:1.21.1-10.7.0.55")
    runtimeOnly("mekanism:Mekanism:1.21.1-10.7.0.55")

    runtimeOnly("curse.maven:xycraft-653786:5601037")
    runtimeOnly("curse.maven:xycraft-machines-653791:5601045")
    runtimeOnly("curse.maven:xycraft-world-653789:5601038")

    compileOnly("curse.maven:just-dire-things-1002348:5894465")
    runtimeOnly("curse.maven:just-dire-things-1002348:5894465")

    runtimeOnly("curse.maven:the-bumblezone-forge-362479:5895588")

    compileOnly("curse.maven:the-twilight-forest-227639:5759335")
    runtimeOnly("curse.maven:the-twilight-forest-227639:5759335")

    compileOnly("curse.maven:framedblocks-441647:5863919")
    runtimeOnly("curse.maven:framedblocks-441647:5863919")

    runtimeOnly("curse.maven:immersive-engineering-231951:5828000")

    runtimeOnly("curse.maven:supplementaries-412082:5902251")
    runtimeOnly("curse.maven:selene-499980:5902944")

    compileOnly("curse.maven:cc-tweaked-282001:5714512")
    runtimeOnly("curse.maven:cc-tweaked-282001:5714512")

    runtimeOnly("curse.maven:ars-nouveau-401955:5955247")
    runtimeOnly("curse.maven:curios-continuation-1037991:5888964")
    runtimeOnly("curse.maven:geckolib-388172:5874016")

    runtimeOnly("curse.maven:forbidden-arcanus-309858:5966516")
    runtimeOnly("curse.maven:valhelsia-core-416935:5847440")

    runtimeOnly("curse.maven:enchanted-witchcraft-560363:5921287")
    runtimeOnly("curse.maven:stateobserver-701213:5888686")
    runtimeOnly("curse.maven:smartbrainlib-661293:5723837")

    runtimeOnly("curse.maven:creeper-overhaul-561625:5725480")
    runtimeOnly("curse.maven:resourceful-config-714059:5753339")
    runtimeOnly("curse.maven:resourceful-lib-570073:5793500")
    runtimeOnly("curse.maven:geckolib-388172:5874016")

    runtimeOnly("curse.maven:tesseract-379232:5657067")
    runtimeOnly("curse.maven:supermartijn642s-config-lib-438332:5546996")
    runtimeOnly("curse.maven:supermartijn642s-core-lib-454372:5713682")

    runtimeOnly("maven.modrinth:caxton:0.6.0-alpha.5+1.21.1-NEOFORGE")

    runtimeOnly("curse.maven:industrial-foregoing-266515:6030556")
    runtimeOnly("curse.maven:titanium-287342:5897690")

    compileOnly("curse.maven:mekanism-covers-1119874:6020891")
    runtimeOnly("curse.maven:mekanism-covers-1119874:6020891")

    runtimeOnly("curse.maven:modern-industrialization-405388:6049273")
    runtimeOnly("curse.maven:cloth-config-348521:5729127")

    runtimeOnly("maven.modrinth:extended-industrialization:1.11.0-beta-1.21.1")
    runtimeOnly("maven.modrinth:tesseract-api:1.6.5-beta-1.21.1")

    compileOnly("curse.maven:applied-energistics-2-223794:6014429")
    runtimeOnly("curse.maven:applied-energistics-2-223794:6014429")

    compileOnly("com.enderio:enderio-machines:7.1.2-alpha")
    runtimeOnly("curse.maven:ender-io-64578:6050753")

    runtimeOnly("curse.maven:eternal-starlight-1080592:6065746")

    runtimeOnly("curse.maven:mahou-tsukai-342543:6062974")

    runtimeOnly("curse.maven:refurbished-furniture-897116:5893840")
    runtimeOnly("curse.maven:framework-549225:5911998")

    runtimeOnly("curse.maven:ender-storage-1-8-245174:6045204")
    compileOnly("curse.maven:codechicken-lib-1-8-242818:6061637")
    runtimeOnly("curse.maven:codechicken-lib-1-8-242818:6061637")

    runtimeOnly("curse.maven:pneumaticcraft-repressurized-281849:6042033")

    compileOnly("curse.maven:stellarview-865273:6102803")
    //runtimeOnly("curse.maven:stellarview-865273:6102803")

    runtimeOnly("curse.maven:ping-222967:5644847")

    runtimeOnly("curse.maven:modular-routers-250294:5937500")

    compileOnly("curse.maven:data-essence-1189360:6508079")
    runtimeOnly("curse.maven:data-essence-1189360:6508079")
    runtimeOnly("curse.maven:databank-1181408:6508070")

    compileOnly("curse.maven:runology-988198:6126814")
    runtimeOnly("curse.maven:runology-988198:6126814")
    runtimeOnly("curse.maven:modonomicon-538392:6338537")

    //runtimeOnly("curse.maven:touhou-little-maid-355044:5896992") crashes due to missing sodium class

    runtimeOnly("curse.maven:jei-238222:5846880")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
