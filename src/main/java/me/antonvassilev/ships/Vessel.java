package me.antonvassilev.ships;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftSign;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;


public class Vessel {
    // Metadata key constants
    public static final String VESSEL_NAME_METADATA_KEY = "VESSEL_NAME";
    public static final String VESSEL_CONTROL_TYPE_METADATA_KEY = "VESSEL_CONTROL_TYPE";
    final private static int MAX_VESSEL_SZ = 5000;
    // size of workList -> max number of blocks to check for largest case (single column of blocks)
    final private static int MAX_SEARCH_SPACE = (MAX_VESSEL_SZ * 4) + 2;
    private final Plugin owningPlugin;
    private final World world;
    private final String name;
    private final ArrayList<BlockInfo> m_blocks = new ArrayList<BlockInfo>();
    private final LicenseSign licenseSign;
    private SteeringSign steeringSign;
    private EngineSign engineSign;
    private int xBlockOffset = 0;
    private int yBlockOffset = 0;
    private int zBlockOffset = 0;

    Vessel(Plugin owningPlugin, String name, Block startBlock) {
        this.name = name;
        this.owningPlugin = owningPlugin;
        this.world = startBlock.getWorld();

        // Engine sign isn't necessary to create vessel, can be added after.
        this.engineSign = null;
        this.steeringSign = null;

        // Set the metadata for the licenseSign block, then discover vessel blocks.
        startBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        startBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, LicenseSign.METADATA_VALUE));
        this.licenseSign = new LicenseSign((CraftSign) startBlock.getState());
        this.xBlockOffset = this.licenseSign.getX();
        this.yBlockOffset = this.licenseSign.getY();
        this.zBlockOffset = this.licenseSign.getZ();

        discoverVesselFromLicense();
    }

    /**
     * Sets the metadata on an engine sign block and adds it to the vessel.
     *
     * @param block Block
     */
    public void addEngineSign(Block block) {
        if (this.engineSign != null) {
            owningPlugin.getLogger().info("Engine already registered for ship.");
            return;
        }
        block.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, EngineSign.METADATA_VALUE));
        block.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
        CraftBlockState blockState = (CraftBlockState) block.getState();
        this.engineSign = new EngineSign((CraftSign) blockState);
        this.m_blocks.add(new BlockInfo(this.engineSign));
    }

    public void moveEngineMetadata(int x, int y, int z) {
        this.engineSign.getBlock().removeMetadata(
                VESSEL_CONTROL_TYPE_METADATA_KEY, owningPlugin
        );
        this.engineSign.getBlock().removeMetadata(
                VESSEL_NAME_METADATA_KEY, owningPlugin
        );

        int newX = this.engineSign.getX() + x;
        int newY = this.engineSign.getY() + y;
        int newZ = this.engineSign.getZ() + z;

        Block newEngineBlock = this.world.getBlockAt(newX, newY, newZ);
        newEngineBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, EngineSign.METADATA_VALUE));
        newEngineBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
    }

    public void rotateEngineMetadata(Rotation rotation) {
        int sinFactor = 0;
        switch (rotation) {
            case LEFT:
                sinFactor = 1;
                break;
            case RIGHT:
                sinFactor = -1;
                break;
        }

        this.engineSign.getBlock().removeMetadata(
                VESSEL_CONTROL_TYPE_METADATA_KEY, owningPlugin
        );
        this.engineSign.getBlock().removeMetadata(
                VESSEL_NAME_METADATA_KEY, owningPlugin
        );

        int oldX = this.engineSign.getX() - xBlockOffset;
        int oldZ = this.engineSign.getZ() - zBlockOffset;

        int newX = (-oldZ * sinFactor) + xBlockOffset;
        int newZ = (oldX * sinFactor) + zBlockOffset;

        Block newEngineBlock = this.world.getBlockAt(newX, this.engineSign.getY(), newZ);
        newEngineBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, EngineSign.METADATA_VALUE));
        newEngineBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
    }

    public void moveSteeringMetadata(int x, int y, int z) {
        this.steeringSign.getBlock().removeMetadata(
                VESSEL_CONTROL_TYPE_METADATA_KEY, owningPlugin
        );
        this.steeringSign.getBlock().removeMetadata(
                VESSEL_NAME_METADATA_KEY, owningPlugin
        );

        int newX = this.steeringSign.getX() + x;
        int newY = this.steeringSign.getY() + y;
        int newZ = this.steeringSign.getZ() + z;

        Block newEngineBlock = this.world.getBlockAt(newX, newY, newZ);
        newEngineBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, SteeringSign.METADATA_VALUE));
        newEngineBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
    }

    public void rotateSteeringMetadata(Rotation rotation) {
        int sinFactor = 0;
        switch (rotation) {
            case LEFT:
                sinFactor = 1;
                break;
            case RIGHT:
                sinFactor = -1;
                break;
        }

        this.steeringSign.getBlock().removeMetadata(
                VESSEL_CONTROL_TYPE_METADATA_KEY, owningPlugin
        );
        this.steeringSign.getBlock().removeMetadata(
                VESSEL_NAME_METADATA_KEY, owningPlugin
        );

        int oldX = this.steeringSign.getX() - xBlockOffset;
        int oldZ = this.steeringSign.getZ() - zBlockOffset;

        int newX = (-oldZ * sinFactor) + xBlockOffset;
        int newZ = (oldX * sinFactor) + zBlockOffset;

        Block newEngineBlock = this.world.getBlockAt(newX, this.steeringSign.getY(), newZ);
        newEngineBlock.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, SteeringSign.METADATA_VALUE));
        newEngineBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));
    }


    void setStatePosition(CraftBlockState block, int x, int y, int z) {
        Field positionField = null;
        try {
            positionField = CraftBlockState.class.getDeclaredField("position");
            positionField.setAccessible(true);
            BlockPos newPosition = new BlockPos(x, y, z);
            positionField.set(block, newPosition);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.toString());
            return;
        }
    }

    public void addSteeringSign(Block block, String direction) {
        block.setMetadata(VESSEL_CONTROL_TYPE_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, SteeringSign.METADATA_VALUE));
        block.setMetadata(VESSEL_NAME_METADATA_KEY,
                new FixedMetadataValue(owningPlugin, name));

        this.steeringSign = new SteeringSign((CraftSign) block.getState());
        this.m_blocks.add(new BlockInfo(this.steeringSign));
    }

    private void discoverVesselFromLicense() {
        HashSet<Block> visitedBlocks = new HashSet<>();
        ArrayDeque<BlockState> workList = new ArrayDeque<>(MAX_SEARCH_SPACE);
        workList.add(this.licenseSign);

        // Start block is always License sign and should be added to m_blocks when created

        while (!workList.isEmpty()) {
            BlockState curBlockState = workList.remove();
            Block curBlock = curBlockState.getBlock();
            if (curBlock.isLiquid() ||
                    curBlock.isEmpty() ||
                    visitedBlocks.contains(curBlock)
            ) {
                continue;
            }

            // Set the metadata on each vessel block.
            curBlock.setMetadata(VESSEL_NAME_METADATA_KEY,
                    new FixedMetadataValue(owningPlugin, name));

            m_blocks.add(new BlockInfo((CraftBlockState) curBlockState));
            if (m_blocks.size() >= MAX_VESSEL_SZ) {
                return;
            }

            visitedBlocks.add(curBlock);
            Bukkit.getLogger().info("Adding block of material: " + curBlock.getType() + " at x: " + curBlock.getX() + " y: " + curBlock.getY());

            workList.add(curBlock.getRelative(BlockFace.UP).getState());
            workList.add(curBlock.getRelative(BlockFace.DOWN).getState());
            workList.add(curBlock.getRelative(BlockFace.EAST).getState());
            workList.add(curBlock.getRelative(BlockFace.WEST).getState());
            workList.add(curBlock.getRelative(BlockFace.NORTH).getState());
            workList.add(curBlock.getRelative(BlockFace.SOUTH).getState());
        }
    }

    //
    // Movement
    //
    public void incrementVelocity() {
        engineSign.incrementVelocity();
    }

    public void decrementVelocity() {
        engineSign.decrementVelocity();
    }

    public void moveForward() {
        m_blocks.sort(Comparator.comparing(BlockInfo::getPriority));
        switch (engineSign.getMovementDirection()) {
            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:
            case NORTH_EAST:
                moveBlocks(-engineSign.velocity, 0, 0);
                break;
            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:
            case SOUTH_WEST:
                moveBlocks(engineSign.velocity, 0, 0);
                break;
            case SOUTH:
            case SOUTH_SOUTH_WEST:
            case SOUTH_SOUTH_EAST:
            case SOUTH_EAST:
                moveBlocks(0, 0, -engineSign.velocity);
                break;
            case NORTH:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case NORTH_WEST:
                moveBlocks(0, 0, engineSign.velocity);
                break;
        }
    }

    public void moveUp() {
        m_blocks.sort(Comparator.comparing(BlockInfo::getPriority));
        moveBlocks(0, 1, 0);
    }

    public void moveDown() {
        m_blocks.sort(Comparator.comparing(BlockInfo::getPriority));
    }

    public BlockFace getRightFace(BlockFace blockFace) {
        return getLeftFace(blockFace).getOppositeFace();
    }

    public BlockFace getLeftFace(BlockFace blockFace) {
        switch (blockFace) {
            case NORTH:
                return BlockFace.EAST;
            case SOUTH:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.SOUTH;
            case WEST:
                return BlockFace.NORTH;
            case UP:
                return BlockFace.UP;
            case DOWN:
                return BlockFace.DOWN;
            case NORTH_EAST:
                return BlockFace.SOUTH_EAST;
            case NORTH_WEST:
                return BlockFace.NORTH_EAST;
            case SOUTH_EAST:
                return BlockFace.SOUTH_WEST;
            case SOUTH_WEST:
                return BlockFace.NORTH_WEST;
            case WEST_NORTH_WEST:
                return BlockFace.NORTH_NORTH_EAST;
            case NORTH_NORTH_WEST:
                return BlockFace.EAST_NORTH_EAST;
            case NORTH_NORTH_EAST:
                return BlockFace.EAST_SOUTH_EAST;
            case EAST_NORTH_EAST:
                return BlockFace.SOUTH_SOUTH_EAST;
            case EAST_SOUTH_EAST:
                return BlockFace.SOUTH_SOUTH_WEST;
            case SOUTH_SOUTH_EAST:
                return BlockFace.WEST_SOUTH_WEST;
            case SOUTH_SOUTH_WEST:
                return BlockFace.WEST_NORTH_WEST;
            case WEST_SOUTH_WEST:
                return BlockFace.NORTH_NORTH_WEST;
            case SELF:
                return BlockFace.SELF;
        }

        return BlockFace.SELF;
    }

    public void rotateBlockTexture(BlockState state, Rotation rotation) {
        BlockData data = state.getBlockData();
        if (data instanceof Rotatable) {
            owningPlugin.getLogger().info("Found rotatable!");
            Rotatable rotatableData = (Rotatable) data;
            BlockFace orientation = rotatableData.getRotation();
            if (orientation == BlockFace.DOWN ||
                    orientation == BlockFace.UP ||
                    orientation == BlockFace.SELF) {
                return;
            }
            switch (rotation) {
                case RIGHT:
                    rotatableData.setRotation(getRightFace(orientation));
                    break;
                case LEFT:
                    rotatableData.setRotation(getLeftFace(orientation));
                    break;
            }
        } else if (data instanceof Orientable) {
            owningPlugin.getLogger().info("Found orientable!");

            Orientable orientableData = (Orientable) data;
            Axis currentAxis = orientableData.getAxis();
            if (currentAxis == Axis.X) {
                orientableData.setAxis(Axis.Z);
            } else if (currentAxis == Axis.Z) {
                orientableData.setAxis(Axis.X);
            }
        } else if (data instanceof Directional) {
            owningPlugin.getLogger().info("Found directional!");

            Directional rotatableData = (Directional) data;
            BlockFace orientation = rotatableData.getFacing();
            if (orientation == BlockFace.DOWN ||
                    orientation == BlockFace.UP ||
                    orientation == BlockFace.SELF) {
                return;
            }
            switch (rotation) {
                case RIGHT:
                    rotatableData.setFacing(getRightFace(orientation));
                    break;
                case LEFT:
                    rotatableData.setFacing(getLeftFace(orientation));
                    break;
            }
        }
        state.setBlockData(data);
    }

    public void rotateVessel(Rotation rotation) {
        m_blocks.sort(Comparator.comparing(BlockInfo::getPriority));
        int sinFactor = 0;
        switch (rotation) {
            case LEFT:
                sinFactor = 1;
                break;
            case RIGHT:
                sinFactor = -1;
                break;
        }

        rotateEngineMetadata(rotation);
        rotateSteeringMetadata(rotation);
        rotateEntities(rotation);

        for (BlockInfo block : m_blocks) {
            block.getState().getLocation().getBlock().setType(Material.AIR);
        }

        for (BlockInfo block : m_blocks) {
            int oldX = block.getX() - xBlockOffset;
            int oldZ = block.getZ() - zBlockOffset;

            int newX = (-oldZ * sinFactor) + xBlockOffset;
            int newZ = (oldX * sinFactor) + zBlockOffset;

            setStatePosition(block.getState(), newX, block.getY(), newZ);
            BlockState bs = block.getState();
            rotateBlockTexture(bs, rotation);
            block.getState().update(true, true);
        }
    }

    public void rotateRight() {
        rotateVessel(Rotation.RIGHT);
    }

    public void rotateLeft() {
        rotateVessel(Rotation.LEFT);
    }

    private void moveBlocks(int x, int y, int z) {
        this.xBlockOffset += x;
        this.yBlockOffset += y;
        this.zBlockOffset += z;

        moveEngineMetadata(x, y, z);
        moveSteeringMetadata(x, y, z);
        moveEntities(x, y, z);

        Bukkit.getLogger().info("Moving blocks!");
        for (BlockInfo block : m_blocks) {
            block.getState().getLocation().getBlock().setType(Material.AIR);
        }

        ListIterator<BlockInfo> ri = m_blocks.listIterator(m_blocks.size());
        while (ri.hasPrevious()) {
            CraftBlockState block = ri.previous().getState();
            setStatePosition(block, block.getX() + x, block.getY() + y, block.getZ() + z);
            block.update(true);
        }
    }

    private void moveEntities(int x, int y, int z) {
        Set<Chunk> vesselChunks = new HashSet<>();
        for (BlockInfo block : m_blocks) {
            Chunk chunk = block.getState().getLocation().getChunk();
            vesselChunks.add(chunk);
        }
        for (Chunk chunk : vesselChunks) {
            Entity[] chunkEntities = chunk.getEntities();
            for (Entity entity : chunkEntities) {
                Location oldLoc = entity.getLocation();
                Location newLoc = oldLoc.add(x, y, z);
                entity.teleport(newLoc);
            }
        }
    }

    private void rotateEntities(Rotation rotation) {
        float yawDelta = 0.0f;
        int sinFactor = 0;
        switch (rotation) {
            case LEFT:
                sinFactor = 1;
                yawDelta = 90.0f;
                break;
            case RIGHT:
                sinFactor = -1;
                yawDelta = -90.0f;
                break;
        }

        Set<Chunk> vesselChunks = new HashSet<>();
        for (BlockInfo block : m_blocks) {
            Chunk chunk = block.getState().getLocation().getChunk();
            vesselChunks.add(chunk);
        }
        for (Chunk chunk : vesselChunks) {
            Entity[] chunkEntities = chunk.getEntities();
            for (Entity entity : chunkEntities) {
                Location oldLoc = entity.getLocation();
                int oldX = oldLoc.getBlockX() - xBlockOffset;
                int oldZ = oldLoc.getBlockZ() - zBlockOffset;

                int newX = (-oldZ * sinFactor) + xBlockOffset;
                int newZ = (oldX * sinFactor) + zBlockOffset;
                Vector oldVelocity = entity.getVelocity();

                Location newLoc = new Location(this.world, newX, oldLoc.getY(), newZ);

                float newYaw = entity.getLocation().getYaw() + yawDelta;
                float pitch = entity.getLocation().getPitch();
                newLoc.setYaw(newYaw);
                newLoc.setPitch(pitch);

                entity.teleport(newLoc);
                entity.setVelocity(oldVelocity);
            }
        }
    }

    private enum Rotation {
        LEFT,
        RIGHT
    }


    /**
     * Enum class that supports string initialization, and allows fetching enum
     * by string value.
     */
    enum ShipSignType {
        LICENSE("[name]", "LICENSE"),
        STEERING("[steer]", "STEERING"),
        ENGINE("[move]", "ENGINE"),
        UNKNOWN("unknown", "UNKNOWN");

        private static final Map<String, ShipSignType> strToShipSignTypeMap =
                Arrays.stream(ShipSignType.values()).collect(Collectors.toMap(
                        ShipSignType::getValue,
                        ShipSignType::getShipSignType
                ));

        private static final Map<String, ShipSignType> metadataValueToSignTypeMap =
                Arrays.stream(ShipSignType.values()).collect(Collectors.toMap(
                        ShipSignType::getMetadataValue,
                        ShipSignType::getShipSignType
                ));

        private static final Map<ShipSignType, String> shipSignTypeToStringMap =
                Arrays.stream(ShipSignType.values()).collect(Collectors.toMap(
                        ShipSignType::getShipSignType,
                        ShipSignType::getValue
                ));

        private final ShipSignType shipSignType;
        private final String value;
        private final String metadataValue;

        ShipSignType(String str, String metadataValue) {
            this.shipSignType = this;
            this.value = str;
            this.metadataValue = metadataValue;
        }

        public static ShipSignType shipSignTypeFromString(String str) {
            ShipSignType type = strToShipSignTypeMap.get(str);
            if (type != null) return type;
            return UNKNOWN;
        }

        public static Optional<String> strToShipSignType(ShipSignType signType) {
            return Optional.ofNullable(shipSignTypeToStringMap.get(signType));
        }

        public static Optional<ShipSignType> metadataStringToShipSignType(String metadataValue) {
            return Optional.ofNullable(metadataValueToSignTypeMap.get(metadataValue));
        }

        public ShipSignType getShipSignType() {
            return shipSignType;
        }

        public String getValue() {
            return value;
        }

        public String getMetadataValue() {
            return metadataValue;
        }
    }

    //
    // Containers for the different types of signs
    //
    static class ShipSign extends CraftSign {
        final static int NUM_LINES = 4;

        public ShipSign(CraftSign sign) {
            super(sign.getWorld(), new SignBlockEntity(sign.getPosition(), sign.getHandle()));
        }

        @Override
        public boolean update(boolean force, boolean applyPhysics) {
            return super.update(force, applyPhysics);
        }
    }

    static class EngineSign extends ShipSign {
        public static final String METADATA_VALUE = "ENGINE";
        private static final int MAX_VELOCITY = 10;
        int velocity;

        public EngineSign(CraftSign sign) {
            super(sign);
            setLine(0, "[Ship]");
            setLine(1, "[move]");
            setColor(DyeColor.RED);
            update();
            this.velocity = 1;
        }

        public void incrementVelocity() {
            this.velocity = min(this.velocity + 1, MAX_VELOCITY);
        }

        public void decrementVelocity() {
            this.velocity = max(this.velocity + 1, 1);
        }

        public BlockFace getMovementDirection() {
            BlockData signData = this.getBlockData();
            if (signData instanceof Rotatable) {
                return ((Rotatable) signData).getRotation();
            }
            return BlockFace.NORTH;
        }
    }

    static class LicenseSign extends ShipSign {
        public static final String METADATA_VALUE = "LICENSE";

        public LicenseSign(CraftSign sign) {
            super(sign);
            setLine(0, "[Ship]");
            setLine(1, "[name]");
            setColor(DyeColor.BLUE);
            update();
        }
    }

    static class SteeringSign extends ShipSign {
        public static final String METADATA_VALUE = "STEERING";

        public SteeringSign(CraftSign sign) {
            super(sign);
            setLine(0, "[Ship]");
            setLine(1, "[steer]");
            setColor(DyeColor.GREEN);
            update();
        }
    }

    private static class BlockInfo {
        private final CraftBlockState state;
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
                this.priority = 0;
            } else {
                this.priority = 1;
            }
        }

        public CraftBlockState getState() {
            return this.state;
        }

        public int getPriority() {
            return this.priority;
        }

        public int getX() {
            return this.state.getX();
        }

        public int getY() {
            return this.state.getY();
        }

        public int getZ() {
            return this.state.getZ();
        }
    }
}
