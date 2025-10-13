package se.file14.procosmetics.v1_21;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import se.file14.procosmetics.nms.AbstractNMSEquipment;
import se.file14.procosmetics.util.ReflectionUtil;

import java.util.Collections;

public class NMSEquipment extends AbstractNMSEquipment<Packet> {

    public NMSEquipment(Player player, boolean tracker) {
        super(player, tracker);
    }

    @Override
    public void sendUpdateToPlayer(Player player) {
        if (player != null) {
            ServerPlayer serverPlayer = (ServerPlayer) ReflectionUtil.getHandle(player);

            if (serverPlayer == null) {
                return;
            }

            ServerGamePacketListenerImpl playerConnection = serverPlayer.connection;

            if (player.getUniqueId() != uuid) {
                playerConnection.send(helmetPacket);
            } else {
                playerConnection.send(slotSetPacket);
            }
        }
    }

    @Override
    public void sendRemoveUpdate(Player player, ItemStack itemStack) {
        ServerPlayer serverPlayer = (ServerPlayer) ReflectionUtil.getHandle(player);

        if (serverPlayer == null) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSetEquipmentPacket(
                id, Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)))
        ));
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        net.minecraft.world.item.ItemStack craftItemStack = CraftItemStack.asNMSCopy(itemStack);

        helmetPacket = new ClientboundSetEquipmentPacket(id, Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, craftItemStack)));
        slotSetPacket = new ClientboundContainerSetSlotPacket(0, 0, 5, craftItemStack);
    }
}
