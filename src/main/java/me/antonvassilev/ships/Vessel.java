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
import java.util.stream.Collectors;

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
    private final HashMap<Block, SteeringSign> steeringSigns = new HashMap<>();

    Vessel(Plugin owningPlugin, String name, Block startBlock) {
        this.name = name;
        this.owningPlugin = owningPlugin;

        // Engine sign isn't necessary to create vessel, can be added after.
        this.engineSign = null;

        // Set the metadata for the licenseSign block, then discover vessel blocks.
        startBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        startBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, LicenseSign.METADATA_VALUE));
        this.licenseSign = new LicenseSign(startBlock);
        discoverVesselFromBlock(startBlock);
    }

    /**
     * Sets the metadata on an engine sign block and adds it to the vessel.
     * @param block Block
     */
    public void addEngineSign(Block block) {
        if(this.engineSign != null) {
            owningPlugin.getLogger().info("Engine already registered for ship.");
            return;
        }
        block.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, EngineSign.METADATA_VALUE));
        block.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        this.engineSign = new EngineSign(block);
    }

    public void addSteeringSign(Block block, String direction) {
        SteeringSign steeringSign = new SteeringSign(block, direction);
        this.steeringSigns.put(block, steeringSign);
    }

    private void discoverVesselFromBlock(Block start_block) {
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

    public void moveForward() {
        // TODO: Forward is not always +x direction.
        m_blocks.sort(Comparator.comparing(Block::getX));
        moveBlocks(1, 0, 0);
    }
    public void moveUp() {
        m_blocks.sort(Comparator.comparing(Block::getY).reversed());
        moveBlocks(0, 1, 0);
    }
    public void moveDown() {
        m_blocks.sort(Comparator.comparing(Block::getY));
        moveBlocks(0, -1, 0);
    }

    public void turn(Block block) {
        SteeringSign sign = this.steeringSigns.get(block);
        if(sign != null) {
            switch (sign.getDirection()) {
                case LEFT:
                    rotateLeft();
                    break;
                case RIGHT:
                    rotateRight();
                    break;
            }
        }
    }

    private void rotateRight() {
        // TODO: Implement
    }
    private void rotateLeft() {
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
        private final Block block;
        public ShipSign(Block block) {
            this.block = block;
        }
    }

    static class EngineSign extends ShipSign {
        public static final String METADATA_VALUE = "ENGINE";
        public EngineSign(Block block) {
            super(block);
        }
    }

    static class LicenseSign extends ShipSign {
        public static final String METADATA_VALUE = "LICENSE";
        public LicenseSign(Block block) {
            super(block);
        }
    }

    static class SteeringSign extends ShipSign {
        public static final String METADATA_VALUE = "STEERING";
        private final Direction direction;
        public SteeringSign(Block block, String directionStr) {
            super(block);
            this.direction = strToDirection(directionStr);
        }

        public Direction getDirection() {
            return direction;
        }

        public static Direction strToDirection(String val) {
            if(val.equals("<")) return Direction.LEFT;
            if(val.equals(">")) return Direction.RIGHT;
            throw new IllegalArgumentException("Unsupported steering direction");
        }

        enum Direction {
            LEFT,
            RIGHT
        }
    }

    /**
     * Enum class that supports string initialization, and allows fetching enum
     * by string value.
     */
    enum ShipSignType
    {
        LICENSE("[name]"),
        STEERING("[steer]"),
        ENGINE("[move]"),
        UNKNOWN("unknown");

        private static final Map<String, ShipSignType> strToShipSignTypeFromStringMap =
                Arrays.stream(ShipSignType.values()).collect(Collectors.toMap(
                        ShipSignType::getValue,
                        ShipSignType::getShipSignType
                ));

        private static final Map<ShipSignType, String> shipSignTypeToStringMap =
                Arrays.stream(ShipSignType.values()).collect(Collectors.toMap(
                        ShipSignType::getShipSignType,
                        ShipSignType::getValue
                ));

        private final ShipSignType shipSignType;
        private final String value;

        ShipSignType(String str) {
            this.shipSignType = this;
            this.value = str;
        }

        public ShipSignType getShipSignType() { return shipSignType; }

        public String getValue() { return value; }

        public static ShipSignType shipSignTypeFromStringFromString(String str) {
            ShipSignType type = strToShipSignTypeFromStringMap.get(str);
            if (type != null) return type;
            return UNKNOWN;
        }

        public static Optional<String> shipSignTypeToString(ShipSignType signType) {
            return Optional.ofNullable(shipSignTypeToStringMap.get(signType));
        }
    }
}
