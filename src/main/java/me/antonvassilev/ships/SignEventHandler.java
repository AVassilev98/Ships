package me.antonvassilev.ships;

import java.util.EventListener;
import java.util.logging.Level;

import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;


public class SignEventHandler implements Listener {
    final private Plugin m_plugin;
    private Vessel m_vessel = null;

    SignEventHandler(Plugin plugin)
    {
        m_plugin = plugin;
    }


    static final String SHIP_STRING = "[Ship]";
    enum ShipSignType
    {
        LICENSE,
        WHEEL,
        ENGINE,
        UNKNOWN,
    }

    private ShipSignType strToSignType(String str)
    {
        switch (str.toLowerCase())
        {
            case "ship":
                return ShipSignType.LICENSE;
            case "[wheel]":
                return ShipSignType.WHEEL;
            case "[move]":
                return ShipSignType.ENGINE;
            default:
                return ShipSignType.UNKNOWN;
        }
    }

    private void handleLicenseSign(SignChangeEvent event)
    {
        m_vessel = new Vessel(event.getBlock());
    }

    private void handleWheelSign(SignChangeEvent event)
    {
        // TODO: Implement
    }

    private void handleEngineSign(SignChangeEvent event)
    {
        // TODO: Handle clicking on engine sign to move
        m_vessel.moveForward();
    }


    @EventHandler
    public void handleIfShipSign(SignChangeEvent event) {
        // Some code here
        if (!event.getLine(0).equals(SHIP_STRING))
        {
            m_plugin.getLogger().info("Sign created not ship sign: " + event.getLine(0));
            return;
        }
        ShipSignType type = strToSignType(event.getLine(1));

        switch (type)
        {
            case LICENSE:
                m_plugin.getLogger().info("New License Sign");
                handleLicenseSign(event);
                break;
            case WHEEL:
                m_plugin.getLogger().info("New Wheel Sign");
                handleWheelSign(event);
                break;
            case ENGINE:
                m_plugin.getLogger().info("New Engine Sign");
                handleEngineSign(event);
                break;
            case UNKNOWN:
                m_plugin.getLogger().info("Unknown Ship Sign");
                return;
        }
    }
}
