package me.antonvassilev.ships;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.*;

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
                new FixedMetadataValue(owningPlugin, "LICENSE"));
        this.licenseSign = new LicenseSign(startBlock);
        discoverVesselFromBlock(startBlock);
    }

    public void addEngineSign(Block eventBlock) {
        eventBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, "ENGINE"));
        eventBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        this.engineSign = new EngineSign(eventBlock);
    }

    private void discoverVesselFromBlock(Block start_block)
    {
        HashSet<Block> visitedBlocks = new HashSet<>();
        ArrayDeque<Block> workList = new ArrayDeque<>(MAX_SEARCH_SPACE);
        workList.add(start_block);

        while (!workList.isEmpty())
        {
            Block curBlock = workList.remove();
            Material curMat = curBlock.getType();
            if (curMat == Material.AIR ||
                curMat == Material.CAVE_AIR ||
                curMat == Material.VOID_AIR ||
                curMat == Material.WATER ||
                curMat == Material.LAVA ||
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

    public void moveForward()
    {
        m_blocks.sort(
            new Comparator<Block>() {
                @Override
                public int compare(Block o1, Block o2) {
                    if (o1.getX() < o2.getX())
                    {
                        return 1;
                    }
                    else if (o1.getX() > o2.getX())
                    {
                        return -1;
                    }
                    return 0;
                }
            }
        );
        for (Block block : m_blocks)
        {
            Location blockLoc = block.getLocation();
            Location newLoc = blockLoc.add(1, 0, 0);
            newLoc.getBlock().setBlockData(block.getBlockData());
            block.setType(Material.AIR);
        }
    }

    public void rotateRight()
    {
        // TODO: Implement
    }
    public void rotateLeft()
    {
        // TODO: Implement
    }
    public void moveUp()
    {
        // TODO: Implement
    }
    public void moveDown()
    {
        // TODO: Implement
    }

    public String getName() {
        return name;
    }

    //
    // Containers for the different types of signs
    //
    static class ShipSign {
        private final Block block;
        public ShipSign(Block block) {
            this.block = block;
        }
    }

    static class EngineSign extends ShipSign {
        public EngineSign(Block block) {
            super(block);
        }
    }

    static class LicenseSign extends ShipSign {
        public LicenseSign(Block block) {
            super(block);
        }
    }
}
