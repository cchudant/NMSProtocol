package me.skybeast.nmsprotocol;

import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import me.skybeast.nmsprotocol.NMSReflection.FieldAccessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressWarnings("ProhibitedExceptionDeclared")
public final class Protocol
{
    /*
     * Accessors
     */

    // Player injection
    private static final FieldAccessor<Object>  PLAYERCONNECTION_NETWORKMANAGER =
            NMSReflection.getFieldAccessor(NMSReflection.getClass("{nms}.PlayerConnection"), "networkManager");
    private static final FieldAccessor<Channel> NETWORKMANAGER_CHANNEL          =
            NMSReflection.getFirstFieldOfTypeAccessor("{nms}.NetworkManager", Channel.class);
    private static final FieldAccessor<Object>  ENTITYPLAYER_PLAYERCONNECTION   =
            NMSReflection.getFieldAccessor("{nms}.EntityPlayer", "playerConnection");
    private static final FieldAccessor<Object>  CRAFTENTITY_ENTITY              =
            NMSReflection.getFieldAccessor("{cb}.entity.CraftEntity", "entity");

    private static final boolean                           SNIFFER  = false;
    static final         Map<SocketAddress, PacketHandler> HANDLERS = new ConcurrentHashMap<>();
    private static List<ChannelFuture> channelFutures;

    /*
     * Injection
     */

    public static void inject()
    {
        Object mcServer      = NMSReflection.getValue(Bukkit.getServer(), "console");
        Object srvConnection = NMSReflection.getFirstValueOfType(mcServer, "{nms}.ServerConnection");
        channelFutures = NMSReflection.getFirstValueOfType(srvConnection, List.class); //Steal channelFutures list

        for (ChannelFuture o : channelFutures)
            o.channel().pipeline().addFirst(ChannelFutureHandler.ID, ChannelFutureHandler.INSTANCE);

        for (Player player : Bukkit.getOnlinePlayers()) // /reload support
            injectPlayer(player);                       // (inject to already connected players)

    }

    private static void injectPlayer(Player player)
    {
        Channel ch = getChannel(player);

        if (ch.pipeline().get(PacketHandler.ID) == null)
            ch.pipeline().addBefore("packet_handler", PacketHandler.ID, new PacketHandler(ch));
    }

    private static Channel getChannel(Player player)
    {
        Object nmsPlayer        = CRAFTENTITY_ENTITY.get(player);
        Object playerConnection = ENTITYPLAYER_PLAYERCONNECTION.get(nmsPlayer);
        Object networkManager   = PLAYERCONNECTION_NETWORKMANAGER.get(playerConnection);
        return NETWORKMANAGER_CHANNEL.get(networkManager);
    }

    public static void clean()
    {
        if (channelFutures == null)
            return;

        for (ChannelFuture o : channelFutures)
        {
            ChannelPipeline pp = o.channel().pipeline();
            if (pp.get(ChannelFutureHandler.ID) != null)
                pp.remove(ChannelFutureHandler.ID);
        }
    }

    public static boolean sendPacket(Player to, Object packet)
    {
        return sendPacket(to.getAddress(), packet);
    }

    public static boolean sendPacket(SocketAddress to, Object packet)
    {
        PacketHandler handler = HANDLERS.get(to);
        if (handler == null) return false;

        handler.channel.writeAndFlush(packet);
        return true;
    }

    public static boolean receivePacket(Player from, Object packet)
    {
        return receivePacket(from.getAddress(), packet);
    }

    public static boolean receivePacket(SocketAddress from, Object packet)
    {
        PacketHandler handler = HANDLERS.get(from);
        if (handler == null) return false;

        handler.channel.pipeline().context("encoder").fireChannelRead(packet);
        return true;
    }

    private static <T extends PacketEvent> boolean callEvent(T event)
    {
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    static Player getPlayer(SocketAddress address)
    {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getAddress().equals(address))
                return player;

        return null;
    }

    public static final class ChannelFutureHandler extends ChannelDuplexHandler
    {
        private static final ChannelFutureHandler INSTANCE = new ChannelFutureHandler();
        private static final String               ID       = "NMSProtocol-ChannelFuture";

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            Channel channel = ((Channel) msg);
            channel.pipeline().addFirst(ChannelInitHandler.ID, new ChannelInitHandler(channel));

            super.channelRead(ctx, msg);
        }

        private ChannelFutureHandler() {}
    }

    @Sharable
    public static final class ChannelInitHandler extends ChannelDuplexHandler
    {
        private static final String ID = "NMSProtocol-Init";
        private final Channel channel;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            PacketHandler handler = new PacketHandler(channel);
            HANDLERS.put(channel.remoteAddress(), handler);

            ChannelPipeline pipeline = ctx.channel().pipeline();
            pipeline.addBefore("packet_handler", PacketHandler.ID, handler);
            pipeline.remove(this); //Auto-remove

            super.channelRead(ctx, msg);
        }

        private ChannelInitHandler(Channel channel) {this.channel = channel;}
    }

    public static final class PacketHandler extends ChannelDuplexHandler
    {
        private static final String ID = "NMSProtocol-PacketHandler";
        private final Channel channel;

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
        {
            if (SNIFFER)
                debugPacket(msg, false);

            SocketAddress remote = channel.remoteAddress();
            if (!callEvent(new SendPacketEvent(msg, remote))) //if event not cancelled
                super.write(ctx, msg, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            if (SNIFFER)
                debugPacket(msg, true);

            SocketAddress remote = channel.remoteAddress();
            if (!callEvent(new ReceivePacketEvent(msg, remote))) //if event not cancelled
                super.channelRead(ctx, msg);
        }

        private PacketHandler(Channel channel) {this.channel = channel;}

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
        {
            HANDLERS.remove(channel.remoteAddress());
            ctx.fireChannelUnregistered();
        }
    }

    private static void debugPacket(Object o, boolean in)
    {
        Class<?> clazz = o.getClass();
        StringBuilder builder = new StringBuilder(in ? ">  IN  --- " : "< OUT  --- ")
                .append(clazz.getSimpleName())
                .append('\n');

        try
        {
            for (Field field : clazz.getDeclaredFields())
            {
                field.setAccessible(true);
                builder.append("- ")
                       .append(field.getType().getName())
                       .append(' ')
                       .append(field.getName())
                       .append(" = ")
                       .append(field.get(o))
                       .append('\n');
            }
        }
        catch (IllegalAccessException e)
        {
            Bukkit.getLogger().log(Level.SEVERE, builder.toString(), e);
        }

        Bukkit.getLogger().info(builder.toString());
    }

    private Protocol() {}
}
