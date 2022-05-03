package me.antonvassilev.ships;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Optional;

public class SignClickEventHandler implements Listener {

    private final Plugin owningPlugin;
    private final HashMap<String, Vessel> vessels;

    public SignClickEventHandler(Plugin owningPlugin, HashMap<String, Vessel> vessels) {
        this.owningPlugin = owningPlugin;
        this.vessels = vessels;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        switch (event.getAction())
        {
            case RIGHT_CLICK_BLOCK: {
                // Make sure the block we clicked is a vessel engine.
                Optional<String> controlType = ShipUtils.getMetadataStringFromBlock(
                        Vessel.VESSEL_CONTROL_TYPE_METADATA_KEY, event.getClickedBlock(), owningPlugin);
                if (!controlType.isPresent() ||
                        !controlType.get().equals(Vessel.EngineSign.METADATA_KEY)) {
                    return;
                }
                // Find the vessel by the name extracted from engine sign metadata and
                // move the vessel forward if found.
                ShipUtils.getMetadataStringFromBlock(Vessel.VESSEL_NAME_METADATA_KEY,
                                event.getClickedBlock(), owningPlugin)
                        .map(vessels::get)
                        .ifPresent(Vessel::moveForward);
                break;
            }
            case LEFT_CLICK_BLOCK: {
                Optional<String> controlType = ShipUtils.getMetadataStringFromBlock(
                        Vessel.VESSEL_CONTROL_TYPE_METADATA_KEY, event.getClickedBlock(), owningPlugin);
                if (!controlType.isPresent() ||
                        !controlType.get().equals(Vessel.EngineSign.METADATA_KEY)) {
                    return;
                }

                Optional<Vessel> owningVessel = ShipUtils.getMetadataStringFromBlock(Vessel.VESSEL_NAME_METADATA_KEY,
                                event.getClickedBlock(), owningPlugin)
                        .map(vessels::get);
                if (!owningVessel.isPresent())
                {
                    break;
                }

                if (event.getPlayer().isSneaking())
                    owningVessel.get().decrementVelocity();
                else
                    owningVessel.get().incrementVelocity();

                break;
            }
            default:
                break;
        }
    }
}
