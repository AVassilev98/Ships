package me.antonvassilev.ships;

import com.google.common.collect.ImmutableList;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SignClickEventHandler implements Listener {

    private final Plugin owningPlugin;
    private final HashMap<String, Vessel> vessels;

    private static final List<String> CLICKABLE_SIGNS_BY_METADATA_VALUES =
            ImmutableList.of(
                    Vessel.EngineSign.METADATA_VALUE, Vessel.SteeringSign.METADATA_VALUE);

    public SignClickEventHandler(Plugin owningPlugin, HashMap<String, Vessel> vessels) {
        this.owningPlugin = owningPlugin;
        this.vessels = vessels;
    }

    /**
     * Handles player right click events on signs. If the sign belongs to a ship,
     * dispatch the event to the handlers.
     * @param event PlayerInteractEvent
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick()) {
            // Make sure the block we clicked is a vessel engine.
            Optional<String> signType = ShipUtils.getMetadataStringFromBlock(
                    Vessel.VESSEL_CONTROL_TYPE_METADATA_KEY,
                    Objects.requireNonNull(event.getClickedBlock()), owningPlugin);
            if(!signType.isPresent() ||
                    !CLICKABLE_SIGNS_BY_METADATA_VALUES.contains(signType.get())) {
                return;
            }
            // Find the vessel by the name extracted from engine sign metadata and
            // then handle the type of sign.
            ShipUtils.getMetadataStringFromBlock(Vessel.VESSEL_NAME_METADATA_KEY,
                    event.getClickedBlock(), owningPlugin)
                    .map(vessels::get)
                    .ifPresent(vessel -> handleShipSign(signType.get(),
                            Objects.requireNonNull(event.getClickedBlock()), vessel));
        }
    }

    private void handleLicenseSign(Vessel vessel, Block block) {
        // Do nothing
    }

    private void handleSteeringSign(Vessel vessel, Block block) {
        vessel.turn(block);
    }

    private void handleEngineSign(Vessel vessel, Block block) {
        vessel.moveForward();
    }

    private void handleShipSign(String signType, Block block, Vessel vessel) {
        Vessel.ShipSignType type = Vessel.ShipSignType.shipSignTypeFromStringFromString(signType);
        switch (type) {
            case LICENSE:
                handleLicenseSign(vessel, block);
                break;
            case STEERING:
                handleSteeringSign(vessel, block);
                break;
            case ENGINE:
                handleEngineSign(vessel, block);
                break;
            case UNKNOWN:
            default:
                owningPlugin.getLogger().info("Ship control sign with unknown type clicked");
        }
    }
}
