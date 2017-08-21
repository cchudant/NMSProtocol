package me.skybeast.nmsprotocol;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener
{
    @Override public void onDisable() {Protocol.clean();}

    @Override public void onEnable()  {Protocol.inject();}
}
