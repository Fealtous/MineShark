package dev.fealtous.mineshark;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.network.ConnectionStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MineShark.MODID)
public class MineShark {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mineshark";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> exclusions = new HashSet<>();
    public MineShark(FMLJavaModLoadingContext context) {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.addListener(MineShark::onConnect);
        MinecraftForge.EVENT_BUS.addListener(MineShark::registerClientCommands);
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exclude")
                .then(Commands.argument("packettype", StringArgumentType.string())
                        .executes(ctx -> {
                            var pname = ctx.getArgument("packettype", String.class);
                            exclusions.add(pname);
                            return SINGLE_SUCCESS;
                        })));
    }

    @SubscribeEvent
    public static void onConnect(ConnectionStartEvent event) {
        event.getConnection().channel().pipeline().addBefore("packet_handler", InboundListener.class.getName(), new InboundListener());
        event.getConnection().channel().pipeline().addBefore("packet_handler", OutboundListener.class.getName(), new OutboundListener());
    }

    static class InboundListener extends SimpleChannelInboundHandler<Packet> {
        public InboundListener() {
            super(false);
        }
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
            var msgname = msg.getClass().getSimpleName();
            if (!exclusions.contains(msgname)) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal(msgname));
            }
            ctx.fireChannelRead(msg);
        }
    }

    static class OutboundListener extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            var msgname = msg.getClass().getSimpleName();
            if (!exclusions.contains(msgname)) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal(msgname));
            }
            super.write(ctx, msg, promise);
        }
    }
}
