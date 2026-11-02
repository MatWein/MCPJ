package de.matthias.puddlejumper.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

public final class ModNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(JumperActionPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JumperActionPacket::encode)
                .decoder(JumperActionPacket::decode)
                .consumerMainThread(JumperActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(JumperImpactPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JumperImpactPacket::encode)
                .decoder(JumperImpactPacket::decode)
                .consumerMainThread(JumperImpactPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
