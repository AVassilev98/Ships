package me.antonvassilev.ships;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.TorchBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlockState;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.*;
import static java.lang.Math.min;
import static java.lang.Math.max;
import java.util.stream.Collectors;

public class Vessel {

    final private static int MAX_VESSEL_SZ = 5000;
    // size of workList -> max number of blocks to check for largest case (single column of blocks)
    final private static int MAX_SEARCH_SPACE = (MAX_VESSEL_SZ * 4) + 2;

    // Metadata key constants
    public static final String VESSEL_NAME_METADATA_KEY = "VESSEL_NAME";
    public static final String VESSEL_CONTROL_TYPE_METADATA_KEY = "VESSEL_CONTROL_TYPE";

    private final Plugin owningPlugin;
    private final World world;
    private final String name;
    private final ArrayList<BlockInfo> m_blocks = new ArrayList<BlockInfo>();
    private final LicenseSign licenseSign;
    private EngineSign engineSign;
    private int xBlockOffset = 0;
    private int yBlockOffset = 0;
    private int zBlockOffset = 0;
    private final HashMap<Block, SteeringSign> steeringSigns = new HashMap<>();

    Vessel(Plugin owningPlugin, String name, Block startBlock) {
        this.name = name;
        this.owningPlugin = owningPlugin;
        this.world = startBlock.getWorld();

        // Engine sign isn't necessary to create vessel, can be added after.
        this.engineSign = null;

        // Set the metadata for the licenseSign block, then discover vessel blocks.
        startBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        startBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, LicenseSign.METADATA_VALUE));

        this.licenseSign = new LicenseSign(startBlock.getState());
        this.xBlockOffset = this.licenseSign.state.getX();
        this.yBlockOffset = this.licenseSign.state.getY();
        this.zBlockOffset = this.licenseSign.state.getZ();
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
        CraftBlockState blockState = (CraftBlockState) block.getState();
        this.engineSign = new EngineSign(blockState);
        this.m_blocks.add(new BlockInfo(blockState));
    }

    public void moveEngineMetadata(int x, int y, int z) {
        this.engineSign.state.getBlock().removeMetadata(
                VESSEL_CONTROL_TYPE_METADATA_KEY, owningPlugin
        );
        this.engineSign.state.getBlock().removeMetadata(
                VESSEL_NAME_METADATA_KEY, owningPlugin
        );

        int newX = this.engineSign.state.getX() + x;
        int newY = this.engineSign.state.getY() + y;
        int newZ = this.engineSign.state.getZ() + z;

        Block newEngineBlock = this.world.getBlockAt(newX, newY, newZ);
        newEngineBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, EngineSign.METADATA_VALUE));
        newEngineBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
    }

    void setStatePosition(CraftBlockState block, int x, int y, int z) {
        Field positionField = null;
        try {
            positionField = CraftBlockState.class.getDeclaredField("position");
            positionField.setAccessible(true);
            BlockPos newPosition = new BlockPos(x, y, z);
            positionField.set((CraftBlockState)block, newPosition);
        }
        catch (Exception e) {
            Bukkit.getLogger().severe(e.toString());
            return;
        }
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

            CraftBlockState block = (CraftBlockState) curBlock.getState();
            m_blocks.add(new BlockInfo(block));
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
        switch (engineSign.getMovementDirection()) {
            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:
            case NORTH_EAST:
                m_blocks.sort(Comparator.comparing(BlockInfo::getPriority).thenComparing(BlockInfo::getX));
                moveBlocks(-engineSign.velocity, 0, 0);
                break;
            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:
            case SOUTH_WEST:
                m_blocks.sort(Comparator.comparing(BlockInfo::getPriority).thenComparing(BlockInfo::getX).reversed());
                moveBlocks(engineSign.velocity, 0, 0);
                break;
            case SOUTH:
            case SOUTH_SOUTH_WEST:
            case SOUTH_SOUTH_EAST:
            case SOUTH_EAST:
                m_blocks.sort(Comparator.comparing(BlockInfo::getPriority).thenComparing(BlockInfo::getZ));
                moveBlocks(0, 0, -engineSign.velocity);
                break;
            case NORTH:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case NORTH_WEST:
                m_blocks.sort(Comparator.comparing(BlockInfo::getPriority).thenComparing(BlockInfo::getZ).reversed());
                moveBlocks(0, 0, engineSign.velocity);
                break;
        }
    }
    public void moveUp() {
        m_blocks.sort(Comparator.comparing(BlockInfo::getPriority).thenComparing(BlockInfo::getY).reversed());
        moveBlocks(0, 1, 0);
    }
    public void moveDown() {
        m_blocks.sort(Comparator.comparing(BlockInfo::getPriority).thenComparing(BlockInfo::getY));
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
        this.xBlockOffset += x;
        this.yBlockOffset += y;
        this.zBlockOffset += z;

        moveEngineMetadata(x, y, z);

        Bukkit.getLogger().info("Moving blocks!");
        for (BlockInfo block : m_blocks) {
            block.getState().getLocation().getBlock().setType(Material.AIR);
        }

        ListIterator<BlockInfo> ri = m_blocks.listIterator(m_blocks.size());
        while (ri.hasPrevious())
        {
            CraftBlockState block = ri.previous().getState();
            setStatePosition(block, block.getX() + x, block.getY() + y, block.getZ() + z);
            block.update(true);
        }
    }

    //
    // Containers for the different types of signs
    //
    static class ShipSign {
        protected final BlockState state;
        public ShipSign(BlockState state) {
            this.state = state;
        }
    }

    static class EngineSign extends ShipSign {
        int velocity;
        private static final int MAX_VELOCITY = 10;
        public static final String METADATA_VALUE = "ENGINE";
        public EngineSign(BlockState state) {
            super(state);
            this.velocity = 1;
        }

        public void incrementVelocity() {
            this.velocity = min(this.velocity + 1, MAX_VELOCITY);
        }

        public void decrementVelocity() {
            this.velocity = max(this.velocity + 1, 1);
        }

        public BlockFace getMovementDirection() {
            BlockData signData = state.getBlockData();
            if (signData instanceof Rotatable) {
                return ((Rotatable) signData).getRotation();
            }
            return BlockFace.NORTH;
        }
    }

    static class LicenseSign extends ShipSign {
        public static final String METADATA_VALUE = "LICENSE";
        public LicenseSign(BlockState state) {
            super(state);
        }
    }

    static class SteeringSign extends ShipSign {
        public static final String METADATA_VALUE = "STEERING";
        private final Direction direction;
        private final Block block;
        public SteeringSign(Block block, String directionStr) {
            super(block.getState());
            this.block = block;
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

    private class BlockInfo {
        private CraftBlockState state;
        private final int priority;
        public BlockInfo(CraftBlockState state) {
            this.state = state;
            BlockData blockData = state.getBlockData();
            Material block = state.getType();
            if (blockData instanceof Sign ||
                    blockData instanceof Switch ||
                    blockData instanceof Rail ||
                    blockData instanceof RedstoneWire ||
                    blockData instanceof Ageable ||
                    block == Material.TORCH ||
                    block == Material.WALL_TORCH ||
                    block == Material.REDSTONE_TORCH ||
                    block == Material.REDSTONE_WALL_TORCH ||
                    block == Material.SOUL_TORCH ||
                    block == Material.SOUL_WALL_TORCH
            ) {
                Bukkit.getLogger().info("Found Attachable");
                this.priority = 1;
            }
            else {
                this.priority = 0;
            }
        }

        public int getPriority()
        {
            return this.priority;
        }
        public CraftBlockState getState()
        {
            return this.state;
        }
        public int getX()
        {
            return this.state.getX();
        }
        public int getY()
        {
            return this.state.getY();
        }
        public int getZ()
        {
            return this.state.getZ();
        }
    };

    /**
     * Enum class that supports string initialization, and allows fetching enum
     * by string value.
     */
    enum ShipSignType {
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
