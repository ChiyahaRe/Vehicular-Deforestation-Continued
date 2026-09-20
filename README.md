# Vehicular Deforestation Continued

*A Sable 2.0.5 compatibility fix for Tree Overrun Sublevels.*

An unofficial compatibility patch for **Sable Vehicular Deforestation**
(`tree_overrun_sublevels`) 1.21.1-0.6.0 by **Leonardoinc22**, updated to work with
**Sable 2.0.5**.

The original mod hard-locks the Minecraft server thread the moment a sub-level touches a
tree. This repository contains the fix and the reasoning behind it.

> **Status:** unofficial. The upstream distribution has been taken down; this was
> reconstructed by decompiling the last released jar. See *Provenance* below.

---

## Previous version's issue

Following an update to SableAPI, freezes have occurred to the point where the system is virtually unusable

## Cause

A re-entry deadlock in the native layer caused by **calling `Rapier3D.getLinearVelocity()`
on a different rigid body** within a collision callback invoked during the execution of `Rapier3D.step()`.

Thread dump during Freeze（Completely identical over 16 seconds with 8 samples、CPU usage 0.21 ms）:

```
Rapier3D.step                                     ← Running Physics step（Locking scene）
  BlockSubLevelCollisionCallback.onCollision
    FragileBlockCallback.sable$onCollision
      TreeLogCollisionCallback.onHit
        TreeLogCollisionCallback.lookupImpactingSubLevel
          TreeOverrunHandler.findImpactingVehicle
            RigidBodyHandle.getLinearVelocity
              Rapier3D.getLinearVelocity          ← Stopped Here
```

Since it is using very little CPU, it is confirmed that the stoppage was caused by an API change.

### Why this happen in newer Sable?

In short, it stopped working because changes were made to the arguments of `sable$onCollision`.


## Configurable

The collision velocity required for the motion scales with mass.

```
Required Velocity = max(0.2, minImpactSpeed × √(impactMassReference ÷ mass))
```


```toml
[impact]
	minImpactSpeed = 5.66        # 5.66 velocity required on 8kpg
	impactMassReference = 8.0
```


## Provenance

The original MOD distributor has been removed. The source code in this repository consists of the last distributed
`tree-overrun-sublevels-1.21.1-0.6.0.jar` file, decompiled using CFR 0.152,
with the above fixes applied. The decompiler’s banner has been left at the beginning of each file to indicate its origin.

The license specified in the `META-INF/neoforge.mods.toml` file of the original MOD is the **Unlicense**
(equivalent to the public domain), which permits redistribution and the publication of derivative works.

- Original mod: **Sable Vehicular Deforestation** by **Leonardoinc22**
- Original license: **Unlicense**
- Requires: [Sable](https://modrinth.com/mod/sable) 2.0.5, Minecraft 1.21.1, NeoForge 21.1.x
- Thanks to original Author **Leonardoinc22** by making my favorite mod.

This repository contains unofficial patches unrelated to the author and has not been approved by the author.
