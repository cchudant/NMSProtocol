package me.skybeast.nmsprotocol;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.net.SocketAddress;
import java.util.Objects;

public abstract class PacketEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS = new HandlerList();
    private final Object        packet;
    private final SocketAddress address;
    private       boolean       cancel;

    PacketEvent(Object packet, SocketAddress address)
    {
        super(true);
        this.packet = packet;
        this.address = address;
    }

    @Override public HandlerList getHandlers()         {return HANDLERS;}

    public static HandlerList getHandlerList()         {return HANDLERS;}

    public abstract boolean isIncoming();

    public boolean isOutgoing()                        {return !isIncoming();}

    @Override public boolean isCancelled()             {return cancel;}

    @Override public void setCancelled(boolean cancel) {this.cancel = cancel;}

    public Object getPacket()                          {return packet;}

    public SocketAddress getAddress()                  {return address;}

    public Player getPlayer()                          {return Protocol.getPlayer(address);}

    @Override public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketEvent other = (PacketEvent) o;
        return cancel == other.cancel &&
               Objects.equals(packet, other.packet) &&
               Objects.equals(address, other.address);
    }

    @Override public int hashCode()
    {
        return Objects.hash(isIncoming(), packet, address, cancel);
    }

    @Override public String toString()
    {
        return getClass().getSimpleName() +
               '{' +
               "packet=" + packet +
               ", address=" + address +
               ", player=" + getPlayer() +
               ", cancel=" + cancel +
               '}';
    }
}
