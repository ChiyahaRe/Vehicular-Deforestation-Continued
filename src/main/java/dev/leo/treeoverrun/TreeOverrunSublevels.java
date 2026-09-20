/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.platform.SableEventPlatform
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.leo.treeoverrun;

import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TreeOverrunSublevels {
    public static final String MOD_ID = "tree_overrun_sublevels";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"tree_overrun_sublevels");

    private TreeOverrunSublevels() {
    }

    public static void init() {
        SableEventPlatform.INSTANCE.onPostPhysicsTick(TreeOverrunHandler::onPostPhysicsTick);
    }
}

