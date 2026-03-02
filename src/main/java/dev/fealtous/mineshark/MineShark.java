package dev.fealtous.mineshark;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.network.ConnectionStartEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod(MineShark.MODID)
public class MineShark {
    public static final String MODID = "mineshark";
    private static final List<String> filter = new ArrayList<>();
    private static Mode mode = Mode.EXCLUDE;
    private static final ConcurrentLinkedQueue<Component> msgQueue = new ConcurrentLinkedQueue<>();
    public MineShark() {
        LogUtils.getLogger().warn("MineShark is enabled. I am not and do not claim to be a performance mod. If you are not using me for education, please remove me. Much love, MineShark <3.");
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
        );
    }

    public static void onConnect(ConnectionStartEvent event) {
        event.getConnection().channel().pipeline().addBefore("packet_handler", InboundListener.class.getName(), new InboundListener());
        event.getConnection().channel().pipeline().addBefore("packet_handler", OutboundListener.class.getName(), new OutboundListener());
    }

    protected synchronized static boolean useFilter(String string, Opt opt) {
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

    public static void queue(Component msg) {
        msgQueue.add(msg);
    }

    private static void chat(Component msg) {
        Minecraft.getInstance().gui.getChat().addMessage(msg);
    }

    private enum Mode {
        INCLUDE,
        EXCLUDE
    }

    protected static String extractPacketInfo(Object p) {
        List<String> comps = new ArrayList<>();
        var parent = p.getClass().getSuperclass();
        // Grab superclass fields as well in case of child class just having a codec (e.g. ClientboundMoveEntityPacket$Pos)
        // If there's some class which is >2 deep then whatever, this isn't meant to be holistic.
        for (Field declaredField : parent.getDeclaredFields()) {
            String name = declaredField.getName();
            declaredField.setAccessible(true);
            try {
                comps.add(name + ": " + declaredField.get(p).toString() + "\n");
            } catch (Exception e) {
                comps.add("Unable to access: " + name + "\n");
            }
        }
        for (Field declaredField : p.getClass().getDeclaredFields()) {
            String name = declaredField.getName();
            declaredField.setAccessible(true);
            try {
                comps.add(name + ": " + declaredField.get(p).toString() + "\n");
            } catch (Exception e) {
                comps.add("Unable to access: " + name + "\n");
            }
        }
        return comps.stream().reduce("", String::concat);
    }
}
