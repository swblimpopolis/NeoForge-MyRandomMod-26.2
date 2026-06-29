---
name: NeoGradle & ModDevGradle Build Setup
description: This skill should be used when the user wants to configure, troubleshoot, or extend the Gradle build system for a NeoForge Minecraft mod. Covers both ModDevGradle (net.neoforged.moddev) and NeoGradle (net.neoforged.gradle.userdev), project setup, run configurations, access transformers, jar-in-jar, Parchment mappings, dependency management, multi-project setups, and CI/CD considerations.
version: 1.0.0
---

# NeoGradle & ModDevGradle Build Setup

You are an expert in NeoForge's Gradle toolchain. You know when to recommend ModDevGradle vs NeoGradle, how to configure runs, access transformers, dependencies, and advanced subsystems correctly.

## Documentation References

Always fetch fresh docs — plugin versions and DSL options evolve:

- **Getting started guide**: `https://docs.neoforged.net/docs/gettingstarted/`
- **ModDevGradle docs**: `https://docs.neoforged.net/toolchain/docs/plugins/mdg/`
- **NeoGradle docs**: `https://docs.neoforged.net/toolchain/docs/plugins/ng/`
- **MDK mirrors** (version-specific starters): `https://github.com/NeoForgeMDKs`
  - `MDK-<mc_version>` → NeoGradle
  - `MDK-<mc_version>-mdg` → ModDevGradle
- **Mod project generator**: `https://neoforged.net/` → "Get Started"

See `references/neogradle-links.md` for curated links.

## Choosing a Plugin

| | **ModDevGradle** | **NeoGradle** |
|---|---|---|
| Plugin ID | `net.neoforged.moddev` | `net.neoforged.gradle.userdev` |
| Buildscript complexity | Simple | Verbose but flexible |
| Multi-version support | No (one NeoForge version) | Yes |
| Vanilla-only mode | Yes (NeoForm, no loader) | No |
| Recommended for | **New mods** | Legacy / multi-version / complex setups |
| Gradle config cache | Yes | Partial |

**Default recommendation: ModDevGradle.** Only switch to NeoGradle if the user needs multi-version support or is migrating a legacy project.

---

## ModDevGradle Setup

### `settings.gradle`
```groovy
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}
```

### `build.gradle`
```groovy
plugins {
    id 'net.neoforged.moddev' version '1.0.11'
}

neoForge {
    version = project.neo_version

    validateAccessTransformers = true   // catch AT errors at build time

    runs {
        client { client() }
        server { server() }
        data {
            data()
            programArguments.addAll '--mod', project.mod_id,
                '--all',
                '--output', file('src/generated/resources/').absolutePath,
                '--existing', file('src/main/resources/').absolutePath
        }
        gameTestServer { gameTestServer() }
    }

    mods {
        "${mod_id}" {
            sourceSet sourceSets.main
        }
    }
}
```

### `gradle.properties`
```properties
mod_id=modid
mod_version=1.0.0
neo_version=21.1.0
minecraft_version=1.21.1
```

### Key DSL options (`neoForge {}`)

| Option | Purpose |
|--------|---------|
| `version` | NeoForge version string |
| `validateAccessTransformers` | Fail build on invalid AT entries |
| `accessTransformers.from(file(...))` | Declare AT file |
| `parchment.mappingsVersion` | Enable Parchment parameter names |
| `parchment.minecraftVersion` | MC version for Parchment data |

### Access Transformers (ModDevGradle)

```groovy
neoForge {
    accessTransformers.from file('src/main/resources/META-INF/accesstransformer.cfg')
}
```

### Jar-in-Jar (ModDevGradle)

```groovy
dependencies {
    jarJar("some.library:artifact:[1.0,2.0)") {
        version { prefer "1.2.3" }
    }
}
```

### Parchment Mappings (ModDevGradle)

```groovy
neoForge {
    parchment {
        mappingsVersion = "2024.01.07"
        minecraftVersion = "1.21.1"
    }
}
```

---

## NeoGradle Setup

### `build.gradle`
```groovy
plugins {
    id 'net.neoforged.gradle.userdev' version '<neogradle_version>'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation "net.neoforged:neoforge:<neoforge_version>"
}

runs {
    configureEach { run ->
        run.modSource sourceSets.main    // include main sourceset in all runs
    }
    client { }
    server { }
    datagen { }
    gameTestServer { }
}
```

**Note:** When using version catalogs, NeoGradle can't auto-detect the NeoForge version at config time. Declare runs manually as shown above.

### Runs DSL (full options)

```groovy
runs {
    someRun {
        runType 'client'                          // inherit from built-in type
        isIsSingleInstance true                   // only one instance at a time
        mainClass 'com.example.Main'
        arguments 'arg1', 'arg2'
        jvmArguments '-Xmx4G'
        environmentVariables 'KEY': 'value'
        systemProperties 'forge.logging.console.level': 'debug'
        workingDirectory file('runs/client')
        shouldBuildAllProjects true
        isClient true                             // mark as client run
        isServer true
        isDataGenerator true
        isGameTest true
        isJUnit true
    }
}
```

### Access Transformers (NeoGradle)

```groovy
accessTransformers {
    file 'src/main/resources/META-INF/accesstransformer.cfg'
}
```

Consume ATs from another dependency:
```groovy
accessTransformers {
    consume 'some.group:module:1.0.0'
}
// or via dependencies block:
dependencies {
    accessTransformer 'some.group:module:1.0.0'
}
```

### Interface Injections (NeoGradle)

```groovy
interfaceInjections {
    file 'src/main/resources/META-INF/interfaceinjection.json'
    consume 'some.group:module:1.0.0'
}
```

### Jar-in-Jar (NeoGradle)

Version range required — single version uses identical bounds:
```groovy
dependencies {
    jarJar("some.library:artifact:[1.0,2.0)") {
        version { prefer "1.2.3" }
    }
    // Exact version (single):
    jarJar("some.library:artifact:[1.2.3]") {
        version { prefer "1.2.3" }
    }
}
```

Consuming jar-in-jar from a dependency:
```groovy
dependencies {
    implementation "project.with:jar-in-jar:1.2.3"
}
```

### Parchment Mappings (NeoGradle)

`gradle.properties`:
```properties
neogradle.subsystems.parchment.minecraftVersion=1.21.1
neogradle.subsystems.parchment.mappingsVersion=2024.01.07
```

Or via DSL:
```groovy
subsystems {
    parchment {
        minecraftVersion = "1.21.1"
        mappingsVersion = "2024.01.07"
        addRepository = true     // auto-adds ParchmentMC repo
        enabled = true
    }
}
```

---

## Common Tasks

```bash
./gradlew runClient          # Launch game client
./gradlew runServer          # Launch dedicated server (accept EULA + set online-mode=false first)
./gradlew runData            # Run data generators
./gradlew runGameTestServer  # Headless GameTest server
./gradlew build              # Build JAR → build/libs/<mod_id>-<version>.jar
./gradlew cleanCache         # Clear NeoGradle's centralized cache
```

---

## Dependency Management

### Adding other mods as dependencies

```groovy
repositories {
    maven { url 'https://maven.somemod.com' }
}
dependencies {
    // Compile + runtime (full dependency)
    implementation "com.example:somemod:1.2.3"

    // Compile-only (soft dep — not bundled, not required at runtime)
    compileOnly "com.example:somemod:1.2.3"

    // Runtime-only (present in dev env but not compile classpath)
    runtimeOnly "com.example:somemod:1.2.3"
}
```

### Run-specific dependencies (NeoGradle, pre-21.9)

```groovy
runs {
    client {
        dependencies {
            runtime 'some:library:1.2.3'
        }
    }
}
```

### LocalRuntime (dev-only, not exposed)

```groovy
dependencies {
    // Available locally in runs but not exported to dependents
    localRuntime "com.example:testmod:1.0.0"
}
```

---

## Multi-Project / Sibling Projects (NeoGradle)

### Including sibling mod sourceset in runs

```groovy
runs {
    client {
        modSources {
            add project.sourceSets.main
            add project(':api').sourceSets.main
        }
    }
}
```

### Sibling with shared mod identifier (fat jar)

```groovy
sourceSets {
    main {
        run {
            modIdentifier 'shared-mod-id'
        }
    }
}
```

### Non-NeoGradle sibling

Sibling JAR manifest must declare:
```
FMLModType: GAMELIBRARY
Automatic-Module-Name: unique.module.name
```
Each project must use unique packages — no package overlap allowed.

---

## CI/CD Considerations

Both plugins detect the `CI=true` environment variable and switch to **binary patch mode** (skips decompilation):
- **ModDevGradle**: recompilation disabled when `CI=true`
- **NeoGradle**: controlled by `neogradle.subsystems.decompiler.enabled=false`

This significantly speeds up CI builds. No source jar is produced in this mode.

### Cache tuning (NeoGradle)

```properties
# gradle.properties
net.neoforged.gradle.caching.enabled=true         # default true
net.neoforged.gradle.caching.maxCacheSize=50      # artifact count before auto-clean
net.neoforged.gradle.caching.logCacheHits=true    # debug cache usage
```

Clean manually: `./gradlew cleanCache`

### Decompiler / recompiler memory (NeoGradle)

```properties
neogradle.subsystems.decompiler.maxMemory=4g
neogradle.subsystems.decompiler.maxThreads=4
neogradle.subsystems.recompiler.maxMemory=2g
neogradle.subsystems.recompiler.shouldFork=true
```

---

## DevLogin (Dev-time Authentication)

Allows using real Minecraft account in dev client runs.

Enable globally:
```properties
neogradle.subsystems.devLogin.enabled=true
```

Per-run (NeoGradle):
```groovy
runs {
    client {
        devLogin {
            enabled true
            profile 'YourMCUsername'
        }
    }
}
```

---

## IDE Setup

### IntelliJ IDEA

After importing the Gradle project, run:
```bash
./gradlew genIntellijRuns   # NeoGradle only — MDG auto-configures
```

For IDEA unit test gutter icons (NeoGradle):
```properties
neogradle.subsystems.conventions.ide.idea.reconfigure-unit-test-templates=true
```

Disable IDEA "Build and run using" override if runs aren't picking up resource changes:
```properties
neogradle.subsystems.conventions.ide.idea.compiler-detection=false
```

### Eclipse

```bash
./gradlew genEclipseRuns
```

---

## Conventions System (NeoGradle — Override Defaults)

All conventions can be individually disabled:

```properties
# Disable ALL conventions
neogradle.subsystems.conventions.enabled=false

# Disable auto-inclusion of main sourceset in runs
neogradle.subsystems.conventions.sourcesets.automatic-inclusion=false

# Disable auto-creation of default run configurations
neogradle.subsystems.conventions.runs.create-default-run-per-type=false
```

Equivalent manual setup when auto-inclusion is disabled:
```groovy
runs {
    configureEach { run ->
        run.modSource sourceSets.main
    }
}
```

---

## SourceSet Inheritance (NeoGradle)

```groovy
sourceSets {
    test {
        inherit.from sourceSets.main    // inherits compile deps
        depends.on sourceSets.main      // adds main output to test classpath
    }
}
```

Only works within the same project.

---

## Vanilla / NeoForm Mode (ModDevGradle only)

For cross-loader projects that don't use NeoForge loader APIs:

```groovy
neoForge {
    neoFormVersion = "1.21.1-20240808.144430"   // NeoForm version, no loader
    // omit 'version' to stay vanilla
}
```
