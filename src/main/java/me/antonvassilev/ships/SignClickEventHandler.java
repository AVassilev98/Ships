package me.antonvassilev.ships;

import com.google.common.collect.ImmutableList;
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
            // Make sure the block we clicked is a ship sign.
            Optional<String> signType = ShipUtils.getMetadataStringFromBlock(
                    Vessel.VESSEL_CONTROL_TYPE_METADATA_KEY,
                    Objects.requireNonNull(event.getClickedBlock()), owningPlugin);
            if(!signType.isPresent() ||
                    !CLICKABLE_SIGNS_BY_METADATA_VALUES.contains(signType.get())) {
                return;
            }
            owningPlugin.getLogger().info(signType.get());
            // Find the vessel by the name extracted from engine sign metadata and
            // then handle the type of sign.
            ShipUtils.getMetadataStringFromBlock(Vessel.VESSEL_NAME_METADATA_KEY,
                    event.getClickedBlock(), owningPlugin)
                    .map(vessels::get)
                    .ifPresent(vessel -> handleShipSign(event, signType.get(), vessel));
        }
    }

    private void handleLicenseSign(Vessel vessel, PlayerInteractEvent event) {
        // Do nothing
    }

    private void handleSteeringSign(Vessel vessel, PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK: {
                vessel.rotateRight();
                break;
            }
            case RIGHT_CLICK_BLOCK: {
                vessel.rotateLeft();
            }
            default:
                break;
        }
    }

    private void handleEngineSign(Vessel vessel, PlayerInteractEvent event) {
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK: {
                vessel.moveForward();
                break;
            }
            case LEFT_CLICK_BLOCK: {
                if (event.getPlayer().isSneaking())
                    vessel.decrementVelocity();
                else
                    vessel.incrementVelocity();
                break;
            }
            default:
                break;
        }
    }

    private void handleShipSign(PlayerInteractEvent event, String signType, Vessel vessel) {
        Optional<Vessel.ShipSignType> type = Vessel.ShipSignType.metadataStringToShipSignType(signType);
        type.ifPresent(shipSignType -> {
            switch (shipSignType) {
                case LICENSE:
                    handleLicenseSign(vessel, event);
                    break;
                case STEERING:
                    handleSteeringSign(vessel, event);
                    break;
                case ENGINE:
                    handleEngineSign(vessel, event);
                    break;
                case UNKNOWN:
                default:
                    owningPlugin.getLogger().info("Ship control sign with unknown type clicked");
            }
        });
    }
}
