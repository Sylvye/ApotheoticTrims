package com.apotheotictrims;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;

final class SilenceEquipmentListener extends PacketListenerAbstract {
    // Vanilla can send a hand swap without sending either item stack.
    private static final int SWAP_HANDS = 55;
    private final SilenceManager silence;

    SilenceEquipmentListener(SilenceManager silence) {
        super(PacketListenerPriority.HIGHEST);
        this.silence = silence;
    }

    @Override public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            var packet = new WrapperPlayServerEntityEquipment(event);
            if (silence.masksEquipment(packet.getEntityId(), event.getUser().getUUID())) {
                packet.setEquipment(packet.getEquipment().stream()
                        .map(entry -> new Equipment(entry.getSlot(), ItemStack.EMPTY))
                        .toList());
                event.markForReEncode(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
            var packet = new WrapperPlayServerEntityStatus(event);
            if (packet.getStatus() == SWAP_HANDS && silence.masksEquipment(packet.getEntityId(), event.getUser().getUUID())) {
                event.setCancelled(true);
            }
        }
    }
}
