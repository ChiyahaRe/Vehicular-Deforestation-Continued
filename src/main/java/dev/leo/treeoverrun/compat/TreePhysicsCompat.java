/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback
 *  dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle
 *  dev.ryanhcode.sable.physics.callback.FragileBlockCallback
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Holder
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.leo.treeoverrun.compat;

import dev.leo.treeoverrun.compat.ChainedFragileBlockCallback;
import dev.leo.treeoverrun.compat.DebrisGroundContact;
import dev.leo.treeoverrun.compat.TreeDebrisImpactCompat;
import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class TreePhysicsCompat {
    public static final double LOG_COLLISION_TRIGGER = 2.0;
    private static Boolean loaderPresent;
    private static Boolean loaded;
    private static FragileBlockCallback logCallback;
    private static Method treeManagerGet;
    private static Method serverTreeManagerGetTree;
    private static TagKey<Block> producesDustOnImpact;
    private static Constructor<?> collisionDustOptionsConstructor;
    private static Holder<SoundEvent> treeImpactSound;
    private static final Set<UUID> DEBRIS_IMPACT_SOUND_PLAYED;
    private static final int DEBRIS_IMPACT_PRESENTATIONS_PER_TICK = 12;
    private static final int DEBRIS_IMPACT_SOUNDS_PER_TICK = 0;
    private static long impactPresentationTick;
    private static int impactPresentationsUsed;
    private static long impactSoundTick;
    private static int impactSoundsUsed;

    private TreePhysicsCompat() {
    }

    public static void bindLoaderPresent(boolean present) {
        loaderPresent = present;
    }

    public static boolean isLoaded() {
        if (loaderPresent != null) {
            return loaderPresent;
        }
        if (loaded == null) {
            try {
                Class.forName("com.farcr.treephysics.collision_callback.LogCallback");
                loaded = true;
            }
            catch (ClassNotFoundException ignored) {
                loaded = false;
            }
        }
        return loaded;
    }

    public static void tickDebrisImpacts(SubLevelPhysicsSystem physicsSystem) {
        TreeDebrisImpactCompat.tick(physicsSystem);
    }

    public static void onDebrisRegistered(ServerLevel level, ServerSubLevel debris, Vector3dc inheritVelocity) {
        TreeDebrisImpactCompat.onDebrisRegistered(level, debris, inheritVelocity);
    }

    public static FragileBlockCallback logCallback() {
        if (!TreePhysicsCompat.isLoaded()) {
            return null;
        }
        if (logCallback == null) {
            try {
                Class<?> callbackClass = Class.forName("com.farcr.treephysics.collision_callback.LogCallback");
                logCallback = (FragileBlockCallback)callbackClass.getField("INSTANCE").get(null);
            }
            catch (ReflectiveOperationException exception) {
                return null;
            }
        }
        return logCallback;
    }

    public static BlockSubLevelCollisionCallback chainedLogCallback(BlockSubLevelCollisionCallback primary) {
        FragileBlockCallback treePhysicsLog = TreePhysicsCompat.logCallback();
        if (treePhysicsLog == null) {
            return primary;
        }
        return ChainedFragileBlockCallback.of((FragileBlockCallback)primary, treePhysicsLog);
    }

    public static void handleDebrisImpactCompat(ServerLevel level, ServerSubLevel debris, Vector3d impactPos, double impactSpeed) {
        if (!TreePhysicsCompat.isLoaded() || impactSpeed < 2.0) {
            return;
        }
        Vector3d worldPoint = impactPos;
        BlockState ground = TreePhysicsCompat.findSolidImpactBlock(level, worldPoint);
        if (ground == null) {
            DebrisGroundContact.Contact contact = DebrisGroundContact.find(level, debris);
            if (contact == null) {
                return;
            }
            ground = contact.groundBlock();
            worldPoint = contact.worldPoint();
        }
        TreePhysicsCompat.spawnDebrisImpactPresentation(level, debris, worldPoint, ground, impactSpeed);
    }

    public static void spawnDebrisImpactPresentation(ServerLevel level, ServerSubLevel debris, Vector3d worldPoint, BlockState groundBlock, double impactSpeed) {
        if (!TreePhysicsCompat.isLoaded()) {
            return;
        }
        if (TreePhysicsCompat.tryConsumeImpactPresentation(level)) {
            TreePhysicsCompat.spawnImpactDust(level, worldPoint, groundBlock);
        }
        if (impactSpeed >= 2.0) {
            TreePhysicsCompat.tryPlayDebrisImpactSound(level, debris);
        }
    }

    public static void clearDebrisCompatState(UUID debrisId) {
        DEBRIS_IMPACT_SOUND_PLAYED.remove(debrisId);
        TreeDebrisImpactCompat.clearTracking(debrisId);
    }

    private static void spawnImpactDust(ServerLevel level, Vector3d impactPos, BlockState ground) {
        if (ground == null || ground.isAir()) {
            return;
        }
        if (TreePhysicsCompat.producesDustOnImpact(ground) && TreePhysicsCompat.trySpawnTreePhysicsDust(level, impactPos, ground)) {
            return;
        }
        TreePhysicsCompat.spawnVanillaBlockDust(level, impactPos, ground);
    }

    private static boolean trySpawnTreePhysicsDust(ServerLevel level, Vector3d impactPos, BlockState ground) {
        try {
            if (collisionDustOptionsConstructor == null) {
                Class<?> optionsClass = Class.forName("com.farcr.treephysics.particle.collision_dust.CollisionDustParticleOptions");
                collisionDustOptionsConstructor = optionsClass.getConstructor(BlockState.class);
            }
            ParticleOptions options = (ParticleOptions)collisionDustOptionsConstructor.newInstance(ground);
            level.sendParticles(options, impactPos.x, impactPos.y, impactPos.z, 6, 0.0, 0.0, 0.0, 1.0);
            return true;
        }
        catch (ReflectiveOperationException ignored) {
            collisionDustOptionsConstructor = null;
            return false;
        }
    }

    private static void spawnVanillaBlockDust(ServerLevel level, Vector3d impactPos, BlockState ground) {
        level.sendParticles((ParticleOptions)new BlockParticleOption(ParticleTypes.BLOCK, ground), impactPos.x, impactPos.y, impactPos.z, 10, 0.25, 0.05, 0.25, 0.12);
    }

    static void tryPlayDebrisImpactSound(ServerLevel level, ServerSubLevel debris) {
    }

    private static boolean tryConsumeImpactPresentation(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (impactPresentationTick != gameTime) {
            impactPresentationTick = gameTime;
            impactPresentationsUsed = 0;
        }
        if (impactPresentationsUsed >= 12) {
            return false;
        }
        ++impactPresentationsUsed;
        return true;
    }

    private static boolean tryConsumeImpactSound(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (impactSoundTick != gameTime) {
            impactSoundTick = gameTime;
            impactSoundsUsed = 0;
        }
        if (impactSoundsUsed >= 0) {
            return false;
        }
        ++impactSoundsUsed;
        return true;
    }

    public static double impactSpeedAt(ServerLevel level, BlockPos pos) {
        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.currentlySteppingSystem;
        if (system == null || system.getLevel() != level) {
            return 0.0;
        }
        ServerSubLevel debris = TreeOverrunHandler.resolveDebrisAt(level, pos, system);
        if (debris != null) {
            return TreePhysicsCompat.subLevelSpeed(system, debris);
        }
        ServerSubLevel vehicle = TreeOverrunHandler.findImpactingVehicle(system, pos, 0.0);
        if (vehicle != null) {
            return TreePhysicsCompat.subLevelSpeed(system, vehicle);
        }
        SubLevel tree = TreePhysicsCompat.treePhysicsSubLevel(level, pos);
        if (tree instanceof ServerSubLevel) {
            ServerSubLevel serverTree = (ServerSubLevel)tree;
            return TreePhysicsCompat.subLevelSpeed(system, serverTree);
        }
        return 0.0;
    }

    private static double subLevelSpeed(SubLevelPhysicsSystem system, ServerSubLevel subLevel) {
        if (SubLevelPhysicsSystem.IN_PHYSICS_STEP) {
            return 0.0;
        }
        RigidBodyHandle handle = system.getPhysicsHandle(subLevel);
        if (handle == null || !handle.isValid()) {
            return 0.0;
        }
        return handle.getLinearVelocity(new Vector3d()).length();
    }

    private static BlockState findSolidImpactBlock(ServerLevel level, Vector3d impactPos) {
        for (double offsetY = 0.0; offsetY <= 2.0; offsetY += 0.25) {
            BlockPos sample = BlockPos.containing((double)impactPos.x, (double)(impactPos.y - offsetY), (double)impactPos.z);
            BlockState state = level.getBlockState(sample);
            if (state.isAir() || !state.isFaceSturdy((BlockGetter)level, sample, Direction.UP)) continue;
            return state;
        }
        return null;
    }

    private static boolean producesDustOnImpact(BlockState state) {
        if (producesDustOnImpact == null) {
            producesDustOnImpact = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"treephysics", (String)"produces_dust_on_impact"));
        }
        return state.is(producesDustOnImpact);
    }

    private static SubLevel treePhysicsSubLevel(ServerLevel level, BlockPos pos) {
        if (!TreePhysicsCompat.isLoaded()) {
            return null;
        }
        try {
            Object manager;
            if (treeManagerGet == null) {
                Class<?> treeManagerClass = Class.forName("com.farcr.treephysics.client.TreeManager");
                treeManagerGet = treeManagerClass.getMethod("get", Level.class);
            }
            if ((manager = treeManagerGet.invoke(null, level)) == null) {
                return null;
            }
            if (serverTreeManagerGetTree == null) {
                serverTreeManagerGetTree = manager.getClass().getMethod("getTree", BlockPos.class);
            }
            return (SubLevel)serverTreeManagerGetTree.invoke(manager, pos);
        }
        catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static {
        DEBRIS_IMPACT_SOUND_PLAYED = ConcurrentHashMap.newKeySet();
        impactPresentationTick = Long.MIN_VALUE;
        impactSoundTick = Long.MIN_VALUE;
    }
}

