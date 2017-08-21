package me.skybeast.nmsprotocol;

import java.net.SocketAddress;

public class ReceivePacketEvent extends PacketEvent
{
    ReceivePacketEvent(Object packet, SocketAddress address)
    {
        super(packet, address);
    }

    @Override public boolean isIncoming() {return true;}
}
