---
name: Minecraft NeoForge Mod Development
description: This skill should be used when the user wants to create, modify, or debug a Minecraft mod using NeoForge (not Forge). Covers MDK setup with ModDevGradle/NeoGradle, DeferredRegister, event bus system, sided logic, CustomPacketPayload networking (1.21+), data generation, config, data attachments (capability replacement), access transformers, and key differences from Forge. Targets NeoForge 1.20.1+ and 1.21.x+.
version: 1.0.0
---

# Minecraft NeoForge Mod Development

You are an expert NeoForge mod developer. You target NeoForge specifically (not Forge — use the forge skill for that). You follow current NeoForge conventions: ModDevGradle for new projects, `CustomPacketPayload` for networking, data attachments over capabilities.

## ⚠️ This Project's Context (READ FIRST)

This project targets a version **newer than the examples below**. Minecraft adopted a
`year.release.patch` versioning scheme in 2026, so the old `1.21` line became `26.x`.

| Property | This project's value |
|----------|----------------------|
| `mod_id` | `myrandommod` |
| `mod_name` | My Random Mod |
| `mod_group_id` / base package | `com.swblimpopolis.myrandommod` |
| `minecraft_version` | `26.2` |
| `minecraft_version_range` | `[26.2]` |
| `neo_version` | `26.2.0.7-beta` |
| Gradle plugin | ModDevGradle (`net.neoforged.moddev`) |
| Source of truth | `gradle.properties` in the project root |

**How to use the rest of this skill:**

1. The **patterns** below (DeferredRegister, two-bus events, `CustomPacketPayload`,
   data attachments, data components, datagen, AT) are the correct *architecture* for
   26.x and carry forward from 1.21 — trust them as a starting shape.
2. The **exact version strings, method signatures, and DSL options** in the examples are
   pinned to `1.21.1` / `neo 21.1.0` and **may be stale for 26.2-beta**. Do **not** copy
   version numbers or assume signatures from them.
3. **26.2 is a beta.** APIs can shift between snapshots and official docs lag. Before
   writing or changing code, **verify against live sources for 26.2** (see the docs links
   in the protocols), and prefer reading the actual installed beta / project files over
   memory. When the live docs and these examples disagree, the live docs win.
4. Always read `gradle.properties` for current values rather than hardcoding any of the
   above — they may change as the beta advances.

## Core Protocols

### 1. Always Fetch Fresh Documentation

NeoForge evolves faster than Forge. API changed significantly at 1.21. Always fetch docs for the exact version before writing code:

- **NeoForge docs**: `https://docs.neoforged.net/docs/` — version selector on the page
- **NeoForge MDK org** (version-specific starters): `https://github.com/NeoForgeMDKs`
  - Naming: `MDK-<mc_version>` (NeoGradle) or `MDK-<mc_version>-mdg` (ModDevGradle)
- **NeoForge javadoc**: `https://javadoc.neoforged.net/`
- **Mod generator** (project scaffold): `https://neoforged.net/` → "Get Started"
- **NeoForge changelogs / migration notes**: `https://neoforged.net/news/`

See `references/neoforge-links.md` for curated links.

DO NOT assume API signatures from memory — the networking, capability, and event bus APIs changed between 1.20.1 and 1.21.

### 2. Gradle Setup

**Two plugin options — ask user which they use:**

| Plugin | ID | Best for |
|--------|----|----------|
| **ModDevGradle** (recommended) | `net.neoforged.moddev` | New mods, simpler buildscripts |
| **NeoGradle** | `net.neoforged.gradle.*` | Multi-version, legacy projects |

**ModDevGradle `build.gradle`:**
```groovy
plugins {
    id 'net.neoforged.moddev' version '1.0.11'
}

neoForge {
    version = project.neo_version

    runs {
        client { client() }
        server { server() }
        data {
            data()
            programArguments.addAll '--mod', project.mod_id,
                '--all', '--output', file('src/generated/resources/').absolutePath,
                '--existing', file('src/main/resources/').absolutePath
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}
```

**`gradle.properties`:**
```properties
mod_id=modid
mod_version=1.0.0
neo_version=21.1.0
mc_version=1.21.1
```

**Gradle tasks:**
```bash
./gradlew runClient         # Launch game client
./gradlew runServer         # Launch dedicated server
./gradlew runData           # Generate data
./gradlew build             # Build JAR
```

**`neoforge.mods.toml`** (`src/main/resources/META-INF/`):
```toml
modLoader = "javafml"
loaderVersion = "[4,)"
license = "MIT"

[[mods]]
modId = "modid"
version = "${file.jarVersion}"
displayName = "My Mod"

[[dependencies.modid]]
    modId = "neoforge"
    type = "required"
    versionRange = "[21.1,)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.modid]]
    modId = "minecraft"
    type = "required"
    versionRange = "[1.21.1,1.22)"
    ordering = "NONE"
    side = "BOTH"
```

### 3. Mod Entrypoint

```java
@Mod("modid")
public class MyMod {
    public static final Logger LOGGER = LogUtils.getLogger();

    public MyMod(IEventBus modBus) {              // IEventBus injected via constructor
        MyItems.ITEMS.register(modBus);
        MyBlocks.BLOCKS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // thread-safe deferred work
        });
    }
}
```

Key difference from Forge: **`IEventBus` is injected into the constructor** — no `FMLJavaModLoadingContext.get()`.

### 4. Registration with DeferredRegister

```java
public class MyItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems("modid");                 // typed helper

    public static final DeferredItem<Item> MY_ITEM =
        ITEMS.registerSimpleItem("my_item");

    public static final DeferredItem<Item> CUSTOM_ITEM =
        ITEMS.register("custom", () -> new MyItem(
            new Item.Properties().stacksTo(16)
        ));
}
```

```java
public class MyBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks("modid");                // typed helper

    public static final DeferredBlock<Block> MY_BLOCK =
        BLOCKS.registerSimpleBlock("my_block",
            BlockBehaviour.Properties.of().strength(1.5f));

    // Auto-registers BlockItem too:
    public static final DeferredItem<BlockItem> MY_BLOCK_ITEM =
        MyItems.ITEMS.registerSimpleBlockItem(MY_BLOCK);
}
```

**Typed `DeferredRegister` helpers (NeoForge additions):**

| Helper | Type |
|--------|------|
| `DeferredRegister.Items` | Items with `DeferredItem<T>` |
| `DeferredRegister.Blocks` | Blocks with `DeferredBlock<T>` |
| `DeferredRegister.DataComponents` | Data components |

Wire in mod constructor: `MyItems.ITEMS.register(modBus);`

**Custom registries:**
```java
public static final ResourceKey<Registry<MyType>> MY_REGISTRY_KEY =
    ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("modid", "my_type"));

// Register in NewRegistryEvent (mod bus)
@SubscribeEvent
public static void onNewRegistry(NewRegistryEvent event) {
    event.create(MY_REGISTRY_KEY);
}
```

**DO NOT query registries during registration.** Only safe after `RegisterEvent` completes.

### 5. Event System

NeoForge has two buses with explicit separation:

| Bus constant | Used for |
|-------------|---------|
| `NeoForge.EVENT_BUS` | Game events (world, entities, players) |
| Injected `IEventBus` (mod bus) | Lifecycle, registration events |

**Method 1 — `@EventBusSubscriber` (static, auto-discovered):**
```java
// Game bus
@EventBusSubscriber(modid = "modid")
public class MyGameEvents {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) { }
}

// Mod bus
@EventBusSubscriber(modid = "modid", bus = EventBusSubscriber.Bus.MOD)
public class MyModEvents {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) { }

    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) { }
}
```

**Method 2 — direct `addListener` (preferred for mod bus in constructor):**
```java
modBus.addListener(MyModEvents::onCommonSetup);
```

**Key lifecycle events (mod bus):**

| Event | Purpose |
|-------|---------|
| `FMLCommonSetupEvent` | Shared setup; use `enqueueWork()` |
| `FMLClientSetupEvent` | Client-only setup |
| `FMLDedicatedServerSetupEvent` | Server-only setup |
| `NewRegistryEvent` | Create custom registries |
| `RegisterPayloadHandlersEvent` | Register network packets |
| `EntityAttributeCreationEvent` | Entity attributes |
| `BuildCreativeModeTabContentsEvent` | Populate creative tabs |

**Mod bus events run in parallel** — use `enqueueWork(Runnable)` from parallel events for non-thread-safe code.

**Cancellable events:**
```java
@SubscribeEvent
public static void onBreak(BlockEvent.BreakEvent event) {
    if (condition) event.setCanceled(true);
}
```

### 6. Sided Logic

**Logical side** (use in game logic): `level.isClientSide()`
**Physical side** (use for class loading): `FMLEnvironment.dist`

```java
// Logical gate — in-game
if (!level.isClientSide()) { /* server logic */ }

// Physical gate — class loading safety
if (FMLEnvironment.dist == Dist.CLIENT) { /* client-only code */ }
```

**Client-only event class:**
```java
@EventBusSubscriber(value = Dist.CLIENT, modid = "modid", bus = Bus.MOD)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(MyEntities.MY_ENTITY.get(), MyRenderer::new);
    }
}
```

Never reference `net.minecraft.client.*` from classes loaded on dedicated server. Split into a separate client class loaded via `Dist.CLIENT`.

### 7. Networking (CustomPacketPayload — 1.21+)

NeoForge replaced `SimpleChannel` with `CustomPacketPayload`. Use this for all new mods on 1.21+.

**Define a payload (record preferred):**
```java
public record MyPayload(int value, String message) implements CustomPacketPayload {

    public static final Type<MyPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("modid", "my_payload"));

    public static final StreamCodec<ByteBuf, MyPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,        MyPayload::value,
            ByteBufCodecs.STRING_UTF8, MyPayload::message,
            MyPayload::new
        );

    @Override
    public Type<MyPayload> type() { return TYPE; }
}
```

**Register (mod bus, `RegisterPayloadHandlersEvent`):**
```java
@SubscribeEvent
public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("modid");

    // Client → Server
    registrar.playToServer(MyPayload.TYPE, MyPayload.STREAM_CODEC,
        (payload, ctx) -> ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.sender();
            // handle on main thread
        }));

    // Server → Client
    registrar.playToClient(OtherPayload.TYPE, OtherPayload.STREAM_CODEC,
        (payload, ctx) -> ctx.enqueueWork(() -> {
            // handle client-side
        }));

    // Both directions
    registrar.playBidirectional(BothPayload.TYPE, BothPayload.STREAM_CODEC, handler);
}
```

**Sending:**
```java
// Client → Server
PacketDistributor.sendToServer(new MyPayload(42, "hello"));

// Server → specific client
PacketDistributor.sendToPlayer(player, new OtherPayload(data));

// Server → all tracking entity
PacketDistributor.sendToPlayersTrackingEntity(entity, new OtherPayload(data));

// Server → all
PacketDistributor.sendToAllPlayers(new OtherPayload(data));
```

**For 1.20.1 NeoForge** (still uses SimpleChannel): fetch `https://docs.neoforged.net/docs/1.20.1/networking/` — the API matches Forge 1.20.1 there.

### 8. Data Generation

```java
@EventBusSubscriber(modid = "modid", bus = Bus.MOD)
public class DataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper efh = event.getExistingFileHelper();

        gen.addProvider(event.includeServer(), new MyRecipeProvider(output, lookup));
        gen.addProvider(event.includeServer(), new MyTagsProvider(output, lookup, efh));
        gen.addProvider(event.includeServer(), new MyLootTableProvider(output, lookup));
        gen.addProvider(event.includeClient(), new MyBlockStateProvider(output, efh));
        gen.addProvider(event.includeClient(), new MyItemModelProvider(output, efh));
        gen.addProvider(event.includeClient(), new MyLangProvider(output, "modid", "en_us"));
    }
}
```

**Recipe provider:**
```java
public class MyRecipeProvider extends RecipeProvider {
    public MyRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MyItems.MY_ITEM.get())
            .pattern("###")
            .define('#', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
            .save(output);
    }
}
```

Run: `./gradlew runData`

### 9. Configuration

```java
public class MyConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SOME_VALUE;
    public static final ModConfigSpec.BooleanValue FEATURE_ON;

    static {
        BUILDER.push("general");
        SOME_VALUE = BUILDER.comment("A value").defineInRange("someValue", 10, 1, 100);
        FEATURE_ON = BUILDER.comment("Toggle").define("featureOn", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
```

Register in mod constructor:
```java
container.registerConfig(ModConfig.Type.COMMON, MyConfig.SPEC);
// 'container' = ModContainer, injected into constructor alongside IEventBus
```

**Full constructor signature:**
```java
public MyMod(IEventBus modBus, ModContainer container) {
    container.registerConfig(ModConfig.Type.COMMON, MyConfig.SPEC);
    // ...
}
```

Config types:

| Type | Location | Synced |
|------|----------|--------|
| `CLIENT` | Client only | No |
| `COMMON` | Both sides | No |
| `SERVER` | Per-world | Yes → clients |
| `STARTUP` | Both | No (early) |

Access: `MyConfig.SOME_VALUE.get()`

### 10. Data Attachments (Replaces Capabilities)

Data attachments store arbitrary data on entities, block entities, chunks, levels. Item stack data → use `DataComponentType` instead.

**Register attachment type:**
```java
public class MyAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "modid");

    public static final Supplier<AttachmentType<Integer>> MY_DATA =
        ATTACHMENT_TYPES.register("my_data", () ->
            AttachmentType.builder(() -> 0)           // default value supplier
                .serialize(Codec.INT)                  // for persistence
                .build()
        );

    // Entity that should keep data on death:
    public static final Supplier<AttachmentType<MyData>> PERSISTENT =
        ATTACHMENT_TYPES.register("persistent", () ->
            AttachmentType.builder(MyData::new)
                .serialize(MyData.CODEC)
                .copyOnDeath()
                .build()
        );
}
```

Wire: `MyAttachments.ATTACHMENT_TYPES.register(modBus);`

**Use:**
```java
// Read (creates default if absent)
int val = entity.getData(MyAttachments.MY_DATA);

// Check
if (entity.hasData(MyAttachments.MY_DATA)) { }

// Write
entity.setData(MyAttachments.MY_DATA, 42);
```

**Sync to client** (entities): Send a `CustomPacketPayload` in `PlayerEvent.StartTracking` with the data.

**Player death**: Use `copyOnDeath()` flag, or handle `PlayerEvent.Clone` manually for conditional logic.

### 11. Data Components (Items — 1.20.5+)

Replaces NBT on items. Use instead of data attachments for item stacks.

```java
public class MyComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.createDataComponents("modid");

    public static final Supplier<DataComponentType<Integer>> CHARGE =
        COMPONENTS.registerComponentType("charge",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
        );
}
```

**Use:**
```java
// Read
int charge = stack.getOrDefault(MyComponents.CHARGE.get(), 0);

// Write (ItemStack is mutable)
stack.set(MyComponents.CHARGE.get(), 10);

// Remove
stack.remove(MyComponents.CHARGE.get());
```

### 12. Access Transformers

Last resort — see the `gtceu-addon` skill's AT policy for the decision framework. Exhaust subclassing, events, and data attachments first.

**Setup** (ModDevGradle auto-detects): `src/main/resources/META-INF/accesstransformer.cfg`

**Format:**
```
# Make field public and non-final
public-f net/minecraft/world/entity/LivingEntity someField

# Make method protected
protected net/minecraft/world/level/Level someMethod(I)V
```

After editing: refresh Gradle project in IDE.

Modifiers: `public`, `protected`, `default`, `private`. Finality: `+f` (add final), `-f` (remove final).

### 13. Key Differences from Forge

| Concern | Forge 1.20.x | NeoForge 1.21+ |
|---------|-------------|----------------|
| `IEventBus` access | `FMLJavaModLoadingContext.get().getModEventBus()` | Injected into `@Mod` constructor |
| `ModContainer` access | `ModLoadingContext.get().getActiveContainer()` | Injected into `@Mod` constructor |
| Networking | `SimpleChannel` / `FriendlyByteBuf` | `CustomPacketPayload` + `StreamCodec` |
| Capabilities | `Capability<T>` + `LazyOptional` | `AttachmentType<T>` |
| Item data | Raw NBT | `DataComponentType<T>` |
| Event bus annotation | `@Mod.EventBusSubscriber(bus=Bus.FORGE/MOD)` | `@EventBusSubscriber(bus=Bus.GAME/MOD)` |
| Gradle plugin | ForgeGradle | ModDevGradle (recommended) or NeoGradle |
| Mod bus dispatch | Sequential | Parallel (use `enqueueWork()`) |
| Config class | `ForgeConfigSpec` | `ModConfigSpec` |
| Packet sending | `PacketDistributor.PLAYER.with(...)` | `PacketDistributor.sendToPlayer(...)` |

### 14. Migration Protocol

When user asks to migrate Forge → NeoForge or upgrade NeoForge version:

1. Confirm source version and target version.
2. Fetch NeoForge migration / changelog: `https://neoforged.net/news/`
3. Key mechanical changes:
   - Replace `Capability` + `LazyOptional` → `AttachmentType` + `getData/setData`
   - Replace `SimpleChannel.registerMessage` → `RegisterPayloadHandlersEvent` + `CustomPacketPayload`
   - Replace `FriendlyByteBuf` manual encode/decode → `StreamCodec.composite`
   - Replace `FMLJavaModLoadingContext.get().getModEventBus()` → constructor injection
   - Replace raw NBT on items → `DataComponentType`
4. After rewriting: run `./gradlew runData` to regenerate resources.

## Tooling

```bash
./gradlew runClient          # In-game client test
./gradlew runServer          # Dedicated server test
./gradlew runData            # Generate data provider output
./gradlew runGameTestServer  # Headless GameTest
./gradlew build              # Build JAR to build/libs/
```

## Documentation References

See `references/neoforge-links.md` for version-specific docs, MDK repos, javadoc, and news/changelog links.
