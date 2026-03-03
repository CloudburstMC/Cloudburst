package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

import java.awt.Color;

@FunctionalInterface
public interface MapColorHandler {

    Color execute(Block block);
}
