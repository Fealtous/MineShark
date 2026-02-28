package dev.fealtous.mineshark;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.network.ConnectionStartEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod(MineShark.MODID)
public class MineShark {
    public static final String MODID = "mineshark";
    private static final List<String> filter = new ArrayList<>();
    private static Mode mode = Mode.EXCLUDE;
//    private static boolean details = false;
    private static final ConcurrentLinkedQueue<Component> msgQueue = new ConcurrentLinkedQueue<>();
    public MineShark(FMLJavaModLoadingContext context) {
        ConnectionStartEvent.BUS.addListener(MineShark::onConnect);
        RegisterClientCommandsEvent.BUS.addListener(MineShark::registerClientCommands);
        TickEvent.ClientTickEvent.Post.BUS.addListener(MineShark::registerMsgConsumer);
    }

    public static void registerMsgConsumer(TickEvent.ClientTickEvent.Post event) {
        while (!msgQueue.isEmpty()) {
            chat(msgQueue.poll());
        }
    }

    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        var enableCommand = Commands.literal("enable")
            .then(Commands.literal("in").executes(InboundListener::enable))
            .then(Commands.literal("out").executes(OutboundListener::enable))
            .then(Commands.literal("all").executes(ctx -> {
                InboundListener.enable(ctx);
                OutboundListener.enable(ctx);
                return 1;
            }));

        var disableCommand = Commands.literal("disable")
            .then(Commands.literal("in").executes(InboundListener::disable))
            .then(Commands.literal("out").executes(OutboundListener::disable))
            .then(Commands.literal("all").executes(ctx -> {
                InboundListener.disable(ctx);
                OutboundListener.disable(ctx);
                return 1;
            }));

        var resetCommand = Commands.literal("reset").executes(ctx -> {
            InboundListener.disable(ctx);
            OutboundListener.disable(ctx);
            useFilter("", Opt.CLEAR);
            return 1;
        });

        var modeCommand = Commands.literal("mode")
            .then(Commands.literal("exclude").executes(ctx -> {
                mode = Mode.EXCLUDE;
                return 1;
            }))
            .then(Commands.literal("include").executes(ctx -> {
                mode = Mode.INCLUDE;
                return 1;
            }));

        var filterCommand = Commands.literal("filter")
            .then(Commands.argument("target", StringArgumentType.string())
            .executes(ctx -> {
                useFilter(StringArgumentType.getString(ctx, "target").replaceAll("/", "*"), Opt.ADD);
                return 1;
            }));
        // todo make detail command.
//        var detailCommand = Commands.literal("detail").executes((ctx) -> {
//            details = !details;
//            return 1;
//        });



        event.getDispatcher().register(Commands.literal("mineshark").executes((ctx -> {
            chat(Component.literal("/mineshark enable in/out/all    | Turns mineshark on for a given packet direction"));
            chat(Component.literal("/mineshark disable in/out/all   | Turns mineshark off for a given packet direction"));
            chat(Component.literal("/mineshark reset                | Turns off and resets the filter and its state to EXCLUDE"));
            chat(Component.literal("/mineshark filter <pattern>     | Will filter out all packets matching that name. Accepts regex e.g. \".*expr.*\" and will need english names."));
            chat(Component.literal("/mineshark mode EXCLUDE/INCLUDE | EXCLUDE will set the filter to not display packets that match. INCLUDE will only display packets that match"));
            //chat(Component.literal("/mineshark detail               | Toggles whether packet contents will be included as a clickable element in chat."));
            return 1;
        }))
            .then(enableCommand)
            .then(disableCommand)
            .then(resetCommand)
            .then(modeCommand)
            .then(filterCommand)
            //.then(detailCommand)
        );
    }

    public static void onConnect(ConnectionStartEvent event) {
        event.getConnection().channel().pipeline().addBefore("packet_handler", InboundListener.class.getName(), new InboundListener());
        event.getConnection().channel().pipeline().addBefore("packet_handler", OutboundListener.class.getName(), new OutboundListener());
    }

    @SuppressWarnings("rawtypes")
    static class InboundListener extends SimpleChannelInboundHandler<Packet> {
        private static boolean enabled = false;
        public InboundListener() {
            super(false);
        }
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
            if (enabled) {
                var msgname = msg.getClass().getSimpleName();
                if (useFilter(msgname, Opt.CHECK)) {
                    msgQueue.add(Component.literal("[IN] " + msgname));
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

    static class OutboundListener extends ChannelOutboundHandlerAdapter {
        private static boolean enabled = false;
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (enabled) {
                var msgname = msg.getClass().getSimpleName();
                if (useFilter(msgname, Opt.CHECK)) {
                    msgQueue.add(Component.literal("[OUT] " + msgname));
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

    private synchronized static boolean useFilter(String string, Opt opt) {
        switch (opt) {
            case ADD -> {
                filter.add(string.toLowerCase());
            }
            case CHECK -> {
                for (String pattern : filter) {
                    if (string.toLowerCase().matches(pattern)) return mode == Mode.INCLUDE;
                }
                return mode == Mode.EXCLUDE;
            }
            case CLEAR -> {
                filter.clear();
            }
        }
        return false;
    }

    private static void chat(Component msg) {
        Minecraft.getInstance().gui.getChat().addMessage(msg);
    }

    private enum Mode {
        INCLUDE,
        EXCLUDE
    }
    private enum Opt {
        CLEAR,
        CHECK,
        ADD
    }
}
