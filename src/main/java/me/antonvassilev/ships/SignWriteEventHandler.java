package me.antonvassilev.ships;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class SignWriteEventHandler implements Listener {

    private static final String SHIP_STRING = "[Ship]";

    private final Plugin owningPlugin;
    private final HashMap<String, Vessel> vessels;

    SignWriteEventHandler(Plugin owningPlugin, HashMap<String, Vessel> vessels) {
        this.owningPlugin = owningPlugin;
        this.vessels = vessels;
    }

    /**
     * Handles placement of signs. If the sign is intended for Ships, dispatch to
     * event handler based on sign content.
     *
     * @param event SignChangeEvent
     */
    @EventHandler
    public void handleIfShipSign(SignChangeEvent event) {
        event.lines().stream().findFirst().ifPresent(component -> {
            if (component instanceof TextComponent) {
                if (((TextComponent) component).content().equals(SHIP_STRING)) {
                    handleShipSign(event.getBlock(),
                            event.lines().stream().skip(1).collect(Collectors.toList()));
                }
            }
        });
    }

    // Handlers for the different types of signs.

    private void handleLicenseSign(Block eventBlock, List<Component> signComponents) {
        owningPlugin.getLogger().info("Registering new ship...");
        Optional<String> name = signComponents.stream().findFirst().map(component -> {
            if (component instanceof TextComponent)
                return ((TextComponent) component).content();
            return null;
        });
        name.ifPresent(s -> {
            vessels.put(s, new Vessel(owningPlugin, s, eventBlock));
            owningPlugin.getLogger().info("Created new vessel: " + s);
        });
    }

    private void handleEngineSign(Block eventBlock, List<Component> signComponents) {
        // TODO: This just gets the metadata from the block the sign was placed on.
        // This will break horribly if the engine sign isn't placed on an existing vessel.
        Optional<String> name = ShipUtils.getMetadataStringFromBlock(
                Vessel.VESSEL_NAME_METADATA_KEY, eventBlock.getRelative(BlockFace.DOWN), owningPlugin);
        name.ifPresent(s -> {
            Vessel vessel = vessels.get(s);
            owningPlugin.getLogger().info("Adding engine sign to vessel: " + s);
            vessel.addEngineSign(eventBlock);
        });
    }

    private void handleSteeringSign(Block eventBlock, List<Component> signComponents) {
        Optional<String> name = ShipUtils.getMetadataStringFromBlock(
                Vessel.VESSEL_NAME_METADATA_KEY, eventBlock.getRelative(BlockFace.DOWN), owningPlugin);
        Optional<String> direction = signComponents.stream().findFirst().map(component -> {
            if (component instanceof TextComponent)
                return ((TextComponent) component).content();
            return null;
        });
        name.ifPresent(s -> direction.ifPresent(d -> {
            Vessel vessel = vessels.get(s);
            owningPlugin.getLogger().info("Adding steering sign to vessel: " + s);
            vessel.addSteeringSign(eventBlock, d);
        }));
    }

    // If a ship sign is created, dispatch the event to the right handler for the type
    // of sign.
    private void handleShipSign(Block eventBlock, List<Component> signComponents) {
        signComponents.stream().findFirst().ifPresent(component -> {
            if (!(component instanceof TextComponent)) return;
            Vessel.ShipSignType type = Vessel.ShipSignType.
                    shipSignTypeFromString(((TextComponent) component).content());
            // Grab the remaining components and pass them down to the handlers.
            List<Component> components = signComponents.stream().skip(1).collect(Collectors.toList());
            switch (type) {
                case LICENSE:
                    owningPlugin.getLogger().info("New License Sign");
                    handleLicenseSign(eventBlock, components);
                    break;
                case STEERING:
                    owningPlugin.getLogger().info("New Steering Sign");
                    handleSteeringSign(eventBlock, components);
                    break;
                case ENGINE:
                    owningPlugin.getLogger().info("New Engine Sign");
                    handleEngineSign(eventBlock, components);
                    break;
                case UNKNOWN:
                    owningPlugin.getLogger().info("Unknown Ship Sign");
            }
        });
    }
}
