#!/bin/zsh
# Builds the patched jar by recompiling the changed classes against the local dependency jars
# and substituting them into the original 0.6.0 jar. Keeping the untouched classes as-is means
# the output matches the original jar entry for entry, apart from the classes listed in PATCHED.
#
# There is no Gradle setup because upstream's build script was never published. Point the
# variables below at your own copies.
set -eu

# --- configure these -------------------------------------------------------------------------
PRISM="${PRISM:-$HOME/Library/Application Support/PrismLauncher}"
INSTANCE="${INSTANCE:-aero}"
ORIGINAL_JAR="${ORIGINAL_JAR:-$HOME/Downloads/tree-overrun-sublevels-1.21.1-0.6.0.jar}"
MC_JAR="${MC_JAR:-$PRISM/libraries/net/minecraft/client/1.21.1-20240808.144430/client-1.21.1-20240808.144430-srg.jar}"
# ---------------------------------------------------------------------------------------------

MODS="$PRISM/instances/$INSTANCE/minecraft/mods"
LIBS="$PRISM/libraries"
BUILD="${BUILD:-$PWD/build}"
OUT="${OUT:-$PWD/tree-overrun-sublevels-1.21.1-0.6.1.jar}"

# Classes this patch changes. Everything else is copied from the original jar untouched.
PATCHED=(
  physics/TreeLogCollisionCallback
  physics/TreeOverrunHandler
  physics/TreeLeafFragileCallback
  compat/ChainedFragileBlockCallback
  compat/TreePhysicsCompat
)

# Resources this patch changes, as paths inside src/main/resources.
PATCHED_RESOURCES=(
  META-INF/neoforge.mods.toml
)

rm -rf "$BUILD"
mkdir -p "$BUILD/deps" "$BUILD/classes" "$BUILD/jar" "$BUILD/stub/org/jetbrains/annotations"

echo "==> unpacking sable jar-in-jar dependencies"
( cd "$BUILD/deps" && unzip -o -q "$MODS/sable-neoforge-1.21.1-2.0.5.jar" "META-INF/jarjar/*.jar" \
  && cp META-INF/jarjar/*.jar . )

# org.jetbrains.annotations is not shipped in the launcher libraries; @Nullable is CLASS-retention
# and advisory only, so a stub is enough to compile against.
cat > "$BUILD/stub/org/jetbrains/annotations/Nullable.java" <<'EOF'
package org.jetbrains.annotations;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,ElementType.FIELD,ElementType.PARAMETER,ElementType.LOCAL_VARIABLE,ElementType.TYPE_USE})
public @interface Nullable {}
EOF
javac -nowarn -d "$BUILD/stub-out" "$BUILD/stub/org/jetbrains/annotations/Nullable.java"

CP="$MODS/sable-neoforge-1.21.1-2.0.5.jar"
CP="$CP:$BUILD/deps/dev.ryanhcode.sable.sable-sable_rapier-1.21.1-2.0.5.jar"
CP="$CP:$BUILD/deps/sable-companion-common-1.21.1-1.6.0.jar"
CP="$CP:$MC_JAR"
CP="$CP:$LIBS/org/joml/joml/1.10.5/joml-1.10.5.jar"
CP="$CP:$LIBS/it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar"
CP="$CP:$LIBS/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
CP="$CP:$LIBS/com/mojang/datafixerupper/8.0.16/datafixerupper-8.0.16.jar"
CP="$CP:$BUILD/stub-out"
CP="$CP:$ORIGINAL_JAR"

echo "==> compiling"
# The neoforge/ and mixin/ packages are unchanged and need NeoForge on the classpath, so they are
# resolved from the original jar instead of being rebuilt.
( cd src/main/java && javac -nowarn -proc:none -d "$BUILD/classes" -cp "$CP" \
    $(find dev/leo/treeoverrun -name '*.java' -not -path '*/neoforge/*' -not -path '*/mixin/*') )

echo "==> assembling jar"
( cd "$BUILD/jar" && unzip -q "$ORIGINAL_JAR" )
for c in "${PATCHED[@]}"; do
  cp "$BUILD/classes/dev/leo/treeoverrun/$c.class" "$BUILD/jar/dev/leo/treeoverrun/$c.class"
done
# nested classes must be replaced alongside their outer class
cp "$BUILD"/classes/dev/leo/treeoverrun/physics/TreeAssemblyQueue*.class \
   "$BUILD/jar/dev/leo/treeoverrun/physics/"
for r in "${PATCHED_RESOURCES[@]}"; do
  cp "src/main/resources/$r" "$BUILD/jar/$r"
done

rm -f "$OUT"
( cd "$BUILD/jar" && zip -q -r -X "$OUT" . )

echo "==> verifying"
if javap -c -p -cp "$OUT" dev.leo.treeoverrun.physics.TreeLogCollisionCallback \
     | grep -qE 'getLinearVelocity|queryIntersecting|getPhysicsHandle'; then
  echo "FAIL: the collision callback still calls into the physics scene" >&2
  exit 1
fi
for c in physics.TreeLogCollisionCallback physics.TreeOverrunHandler physics.TreeAssemblyQueue \
         physics.TreeLeafFragileCallback compat.ChainedFragileBlockCallback compat.TreePhysicsCompat; do
  if javap -c -p -cp "$OUT" "dev.leo.treeoverrun.$c" | grep -q 'getCurrentlySteppingSystem'; then
    echo "FAIL: $c still calls the throwing getCurrentlySteppingSystem()" >&2
    exit 1
  fi
done
diff <(unzip -l "$ORIGINAL_JAR" | awk '{print $4}' | sort) \
     <(unzip -l "$OUT" | awk '{print $4}' | sort) > /dev/null \
  || { echo "FAIL: jar entries differ from the original" >&2; exit 1; }
# the four-argument sable$onCollision does not exist before Sable 2.0.5, so loading against an
# older Sable would fail at runtime rather than at dependency resolution
unzip -p "$OUT" META-INF/neoforge.mods.toml | grep -q 'versionRange = "\[2.0.5,)"' \
  || { echo "FAIL: the sable dependency range was not applied" >&2; exit 1; }

echo "==> ok: $OUT"
