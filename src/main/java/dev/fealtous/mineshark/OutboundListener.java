package dev.fealtous.mineshark;

import com.mojang.brigadier.context.CommandContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

class OutboundListener extends ChannelOutboundHandlerAdapter {
    private static boolean enabled = false;
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (enabled) {
            var msgname = msg.getClass().getSimpleName();
            if (MineShark.useFilter(msgname, Opt.CHECK)) {
                var info = Component.literal("[OUT] " + msgname);
                var k = MineShark.extractPacketInfo(msg);

                info.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(k))));
                MineShark.queue(info);
            }
        }
        super.write(ctx, msg, promise);
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
