package ez.initial.core.main.hacks.exploit;

import com.mojang.realmsclient.gui.ChatFormatting;
import ez.initial.api.event.events.WurstplusEventPacket;
import ez.initial.api.guiscreen.settings.WurstplusSetting;
import ez.initial.core.Wurstplus;
import ez.initial.core.main.hacks.WurstplusCategory;
import ez.initial.core.main.hacks.WurstplusHack;
import ez.initial.core.main.util.InventoryUtil;
import ez.initial.core.main.util.WurstplusMessageUtil;
import ez.initial.core.main.util.WurstplusTimer;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.util.EnumHand;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FrameDupe extends WurstplusHack {
    public FrameDupe() {
        super("Auto Frame Dupe", "AutoFrameDupe", "frame dupe but automated.", WurstplusCategory.WURSTPLUS_EXPLOIT);
    }
    WurstplusSetting shulkerSwap = create("Shulker Swap", true);
    WurstplusSetting placeDelay = create("Place Delay", 100, 0, 500);
    WurstplusSetting delay = create("Delay", 100, 0, 500);
    WurstplusSetting swing = create("Swing", false);
    WurstplusSetting place = create("Place", true);
    WurstplusSetting randomizeHotbar = create("Randomize Hotbar", true);

    WurstplusTimer timer = new WurstplusTimer();
    WurstplusTimer placeTimer = new WurstplusTimer();
    ArrayList<Integer> list = new ArrayList<>();
    boolean isSendingPacket;

    @Override
    public void enable() {
        timer.reset();
        list.clear();
        placeTimer.reset();
        isSendingPacket = false;
    }

    @SuppressWarnings("unused")
    @EventHandler
    Listener<WurstplusEventPacket.SendPacket> event = new Listener<>(event -> {
        Packet<?> packet = event.get_packet();
        if (packet instanceof CPacketUseEntity) {
            Entity entity = ((CPacketUseEntity) packet).getEntityFromWorld(mc.world);
            if (entity != null && list.contains(entity.getEntityId()) && ((CPacketUseEntity) packet).action == CPacketUseEntity.Action.ATTACK) {
                EntityItemFrame frame = (EntityItemFrame) entity;
                if (frame.getDisplayedItem().item == null) {
                    event.cancel();
                }
            } else if (entity != null && !list.contains(entity.getEntityId()) && mc.gameSettings.keyBindSneak.isKeyDown() && entity instanceof EntityItemFrame) {
                list.add(entity.getEntityId());
                WurstplusMessageUtil.send_client_message(ChatFormatting.GREEN + "added :D");
                event.cancel();
            }
        }
    });

    @EventHandler
    Listener<WurstplusEventPacket.ReceivePacket> receive = new Listener<>(event -> {
        if (event.get_packet() instanceof SPacketDestroyEntities) {
            List<Integer> ids = Arrays.stream(((SPacketDestroyEntities) event.get_packet()).getEntityIDs()).boxed().collect(Collectors.toList());
            for (int id : list) {
                if (ids.contains(id)) {
                    Wurstplus.notificationManager.notify("Item frame is broken!", new Color(255, 0, 0));
                }
            }
        }
    });

    @Override
    public void preTick() {
        if (mc.player == null && mc.world == null) return;
        for (Integer entities : list) {
            Entity entity = mc.world.getEntityByID(entities);
            if (entity != null) {
                EntityItemFrame item = (EntityItemFrame) entity;

                if (randomizeHotbar.get_value(true)) {
                    InventoryUtil.switchToSlot((int) Math.floor(Math.random() * 9), false);
                }
                if (InventoryUtil.checkShulker() != -1 && shulkerSwap.get_value(true)) {
                    InventoryUtil.switchToShulker();
                }
                if (place.get_value(true) && placeTimer.passed(placeDelay.get_value(1))) {
                    if (item.getDisplayedItem().item == null) {
                        mc.player.connection.sendPacket(new CPacketUseEntity(entity, EnumHand.MAIN_HAND));
                        if (swing.get_value(true)) mc.player.swingArm(EnumHand.MAIN_HAND);
                    }
                    placeTimer.reset();
                }
                if (timer.passed(delay.get_value(1))) {
                    if (item.getDisplayedItem().item != null && !isSendingPacket) {
                        // prevent multiple packets being sent.
                        isSendingPacket = true;
                        mc.player.connection.sendPacket(new CPacketUseEntity(entity));
                        if (swing.get_value(true)) mc.player.swingArm(EnumHand.MAIN_HAND);
                        isSendingPacket = false;
                    } else {
                        WurstplusMessageUtil.send_client_message("prevented hitting empty itemframe");
                    }
                    timer.reset();
                }
            }
        }
    }
}
