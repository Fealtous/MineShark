package dev.fealtous.mineshark;

import com.mojang.brigadier.context.CommandContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;

@SuppressWarnings("rawtypes")
public class InboundListener extends SimpleChannelInboundHandler<Packet> {
    private static boolean enabled = false;
    public InboundListener() {
        super(false);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
        if (enabled) {
            var msgname = msg.getClass().getSimpleName();
            if (MineShark.useFilter(msgname, Opt.CHECK)) {
                var info = Component.literal("[IN] " + msgname);

                var k = MineShark.extractPacketInfo(msg);
                info.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(k))));
                MineShark.queue(info);
            }
        }
        ctx.fireChannelRead(msg);
    }
    protected static int enable(CommandContext<CommandSourceStack> ctx) {
        enabled = true;
        return 1;
    }
    protected static int disable(CommandContext<CommandSourceStack> ctx) {
        enabled = false;
        return 1;
    }
}
