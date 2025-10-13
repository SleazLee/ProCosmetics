package se.file14.procosmetics.v1_21;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import se.file14.procosmetics.nms.AbstractNMSEquipment;
import se.file14.procosmetics.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import javax.annotation.Nullable;

public class NMSEquipment extends AbstractNMSEquipment<Packet> {

    private static final Method CRAFT_ITEM_STACK_AS_NMS_COPY = resolveCraftItemStackConverter();

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
                id, Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, toNmsItemStackOrEmpty(itemStack)))
        ));
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        net.minecraft.world.item.ItemStack craftItemStack = toNmsItemStackOrEmpty(itemStack);

        helmetPacket = new ClientboundSetEquipmentPacket(id, Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, craftItemStack)));
        slotSetPacket = new ClientboundContainerSetSlotPacket(0, 0, 5, craftItemStack);
    }

    @Nullable
    private static Method resolveCraftItemStackConverter() {
        Class<?> craftItemStackClass = ReflectionUtil.getBukkitClass("inventory.CraftItemStack");

        if (craftItemStackClass != null) {
            return ReflectionUtil.getMethod(craftItemStackClass, "asNMSCopy", ItemStack.class);
        }
        return null;
    }

    private static net.minecraft.world.item.ItemStack toNmsItemStackOrEmpty(ItemStack itemStack) {
        if (itemStack == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        if (CRAFT_ITEM_STACK_AS_NMS_COPY == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        try {
            Object result = CRAFT_ITEM_STACK_AS_NMS_COPY.invoke(null, itemStack);

            if (result instanceof net.minecraft.world.item.ItemStack nmsStack) {
                return nmsStack;
            }
        } catch (IllegalAccessException exception) {
            exception.printStackTrace();
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause != null) {
                cause.printStackTrace();
            } else {
                exception.printStackTrace();
            }
        }

        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
