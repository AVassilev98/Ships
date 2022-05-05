package me.antonvassilev.ships;

import org.bukkit.block.Block;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

public class ShipUtils {

    /**
     * Extracts a String metadata value from a (block, plugin) tuple. Assumes that the value object
     * is of String type.
     *
     * @param key          String metadata key
     * @param block        Block block
     * @param owningPlugin Plugin the plugin that we are fetching metadata for
     * @return Optional containing the value, if found.
     */
    public static Optional<String> getMetadataStringFromBlock(String key, Block block, Plugin owningPlugin) {
        List<MetadataValue> metadataValues =
                block.getMetadata(key);
        Optional<MetadataValue> obj = metadataValues
                .stream()
                .filter(metadataValue -> metadataValue.getOwningPlugin().equals(owningPlugin))
                .findFirst();
        return obj.map(metadataValue -> (String) metadataValue.value());
    }
}
