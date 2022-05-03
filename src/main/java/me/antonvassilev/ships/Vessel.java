package me.antonvassilev.ships;

import net.kyori.adventure.text.BlockNBTComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.*;
import static java.lang.Math.min;
import static java.lang.Math.max;

public class Vessel {
    final private static int MAX_VESSEL_SZ = 5000;
    // size of workList -> max number of blocks to check for largest case (single column of blocks)
    final private static int MAX_SEARCH_SPACE = (MAX_VESSEL_SZ * 4) + 2;

    // Metadata key constants
    public static final String VESSEL_NAME_METADATA_KEY = "VESSEL_NAME";
    public static final String VESSEL_CONTROL_TYPE_METADATA_KEY = "VESSEL_CONTROL_TYPE";

    private final Plugin owningPlugin;
    private final String name;
    private final ArrayList<Block> m_blocks = new ArrayList<>();
    private final LicenseSign licenseSign;
    private EngineSign engineSign;

    Vessel(Plugin owningPlugin, String name, Block startBlock)
    {
        this.name = name;
        this.owningPlugin = owningPlugin;

        // Engine sign isn't necessary to create vessel, can be added after.
        this.engineSign = null;

        // Set the metadata for the licenseSign block, then discover vessel blocks.
        startBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        startBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, LicenseSign.METADATA_KEY));
        this.licenseSign = new LicenseSign(startBlock.getState());
        discoverVesselFromBlock(startBlock);
    }

    /**
     * Sets the metadata on an engine sign block and adds it to the vessel.
     * @param eventBlock Block
     */
    public void addEngineSign(Block eventBlock) {
        eventBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, EngineSign.METADATA_KEY));
        eventBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        this.engineSign = new EngineSign(eventBlock.getState());
    }

    private void discoverVesselFromBlock(Block start_block)
    {
        HashSet<Block> visitedBlocks = new HashSet<>();
        ArrayDeque<Block> workList = new ArrayDeque<>(MAX_SEARCH_SPACE);
        workList.add(start_block);

        while (!workList.isEmpty())
        {
            Block curBlock = workList.remove();
            if (curBlock.isLiquid() ||
                curBlock.isEmpty() ||
                visitedBlocks.contains(curBlock)
            )
            {
                continue;
            }

            // Set the metadata on each vessel block.
            curBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                    new FixedMetadataValue(owningPlugin, name));

            m_blocks.add(curBlock);
            if(m_blocks.size() >= MAX_VESSEL_SZ)
            {
                return;
            }

            visitedBlocks.add(curBlock);
            Bukkit.getLogger().info("Adding block of material: " + curBlock.getType() + " at x: " + curBlock.getX() + " y: " + curBlock.getY());

            workList.add(curBlock.getRelative(BlockFace.UP));
            workList.add(curBlock.getRelative(BlockFace.DOWN));
            workList.add(curBlock.getRelative(BlockFace.EAST));
            workList.add(curBlock.getRelative(BlockFace.WEST));
            workList.add(curBlock.getRelative(BlockFace.NORTH));
            workList.add(curBlock.getRelative(BlockFace.SOUTH));
        }
    }

    //
    // Movement
    //
    public void incrementVelocity()
    {
        engineSign.incrementVelocity();
    }

    public void decrementVelocity()
    {
        engineSign.decrementVelocity();
    }

    public void moveForward() {
        // TODO: Forward is not always +x direction.

        switch (engineSign.getMovementDirection())
        {
            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:
            case NORTH_EAST:
                m_blocks.sort(Comparator.comparing(Block::getX));
                moveBlocks(-engineSign.velocity, 0, 0);
                break;
            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:
            case SOUTH_WEST:
                m_blocks.sort(Comparator.comparing(Block::getX).reversed());
                moveBlocks(engineSign.velocity, 0, 0);
                break;
            case SOUTH:
            case SOUTH_SOUTH_WEST:
            case SOUTH_SOUTH_EAST:
            case SOUTH_EAST:
                m_blocks.sort(Comparator.comparing(Block::getZ));
                moveBlocks(0, 0, -engineSign.velocity);
                break;
            case NORTH:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case NORTH_WEST:
                m_blocks.sort(Comparator.comparing(Block::getZ).reversed());
                moveBlocks(0, 0, engineSign.velocity);
                break;
        }
    }
    public void moveUp() {
        m_blocks.sort(Comparator.comparing(Block::getY).reversed());
        moveBlocks(0, 1, 0);
    }
    public void moveDown() {
        m_blocks.sort(Comparator.comparing(Block::getY));
    }

    public void rotateRight() {
        // TODO: Implement
    }
    public void rotateLeft() {
        // TODO: Implement
    }

    private void moveBlocks(int x, int y, int z) {
        for (Block block : m_blocks) {
            Location blockLoc = block.getLocation();
            Location newLoc = blockLoc.add(x, y, z);
            newLoc.getBlock().setBlockData(block.getBlockData());
            block.setType(Material.AIR);
        }
    }

    //
    // Containers for the different types of signs
    //
    static class ShipSign {
        protected final BlockState block;
        public ShipSign(BlockState block) {
            this.block = block;
        }
    }

    static class EngineSign extends ShipSign {
        int velocity;
        private static final int MAX_VELOCITY = 10;
        public static final String METADATA_KEY = "ENGINE";
        public EngineSign(BlockState block) {
            super(block);
            this.velocity = 1;
        }
        public void incrementVelocity()
        {
            this.velocity = min(this.velocity + 1, MAX_VELOCITY);
        }
        public void decrementVelocity()
        {
            this.velocity = max(this.velocity + 1, 1);
        }
        public BlockFace getMovementDirection()
        {
            BlockData signData = block.getBlockData();
            if (signData instanceof Rotatable) {
                return ((Rotatable) signData).getRotation();
            }
            return BlockFace.NORTH;
        }
    }

    static class LicenseSign extends ShipSign {
        public static final String METADATA_KEY = "LICENSE";
        public LicenseSign(BlockState block) {
            super(block);
        }
    }
}
