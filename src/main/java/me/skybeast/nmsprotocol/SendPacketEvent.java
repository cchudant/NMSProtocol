package me.skybeast.nmsprotocol;

import org.bukkit.entity.Player;

import java.net.SocketAddress;

public class SendPacketEvent extends PacketEvent
{
    SendPacketEvent(Object packet, SocketAddress address)
    {
        super(packet, address);
    }

    @Override public boolean isIncoming() {return false;}
}
