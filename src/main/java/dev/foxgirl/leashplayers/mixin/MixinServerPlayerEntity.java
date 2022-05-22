package dev.foxgirl.leashplayers.mixin;

import dev.foxgirl.leashplayers.LeashImpl;
import dev.foxgirl.leashplayers.LeashPlayers;
import dev.foxgirl.leashplayers.LeashProxyEntity;
import dev.foxgirl.leashplayers.LeashSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity implements LeashImpl {
    private final ServerPlayerEntity leashplayers$self = (ServerPlayerEntity) (Object) this;
    private final LeashSettings leashplayers$settings = LeashPlayers.getSettings(leashplayers$self.world);

    private LeashProxyEntity leashplayers$proxy;
    private Entity leashplayers$holder;

    private int leashplayers$lastage;

    private boolean leashplayers$disabled() {
        return !leashplayers$settings.isEnabled();
    }

    private void leashplayers$update() {
        if (
            leashplayers$holder != null && (
                leashplayers$disabled()
                || !leashplayers$holder.isAlive()
                || !leashplayers$self.isAlive()
                || leashplayers$self.hasVehicle()
            )
        ) {
            leashplayers$detach();
            leashplayers$drop();
        }

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy = null;
            }
            else {
                var holderActual = leashplayers$holder;
                var holderTarget = leashplayers$proxy.getHoldingEntity();

                if (holderTarget == null && holderActual != null) {
                    leashplayers$detach();
                    leashplayers$drop();
                }
                else if (holderTarget != holderActual) {
                    leashplayers$attach(holderTarget);
                }
            }
        }

        leashplayers$apply();
    }

    private void leashplayers$apply() {
        var player = leashplayers$self;
        var holder = leashplayers$holder;
        if (holder == null) return;
        if (holder.world != player.world) return;

        var distance = player.distanceTo(holder);
        if (distance < leashplayers$settings.getDistanceMin()) {
            return;
        }
        if (distance > leashplayers$settings.getDistanceMax()) {
            leashplayers$detach();
            leashplayers$drop();
            return;
        }

        var dx = (holder.getX() - player.getX()) / (double) distance;
        var dy = (holder.getY() - player.getY()) / (double) distance;
        var dz = (holder.getZ() - player.getZ()) / (double) distance;

        player.addVelocity(
            Math.copySign(dx * dx * 0.4D, dx),
            Math.copySign(dy * dy * 0.4D, dy),
            Math.copySign(dz * dz * 0.4D, dz)
        );

        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.velocityDirty = false;
    }

    private void leashplayers$attach(Entity entity) {
        leashplayers$holder = entity;

        if (leashplayers$proxy == null) {
            leashplayers$proxy = new LeashProxyEntity(leashplayers$self);
            leashplayers$self.world.spawnEntity(leashplayers$proxy);
        }
        leashplayers$proxy.attachLeash(leashplayers$holder, true);

        if (leashplayers$self.hasVehicle()) {
            leashplayers$self.stopRiding();
        }

        leashplayers$lastage = leashplayers$self.age;
    }

    private void leashplayers$detach() {
        leashplayers$holder = null;

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.isAlive() || !leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy.proxyRemove();
            }
            leashplayers$proxy = null;
        }
    }

    private void leashplayers$drop() {
        leashplayers$self.dropItem(Items.LEAD);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void leashplayers$tick(CallbackInfo info) {
        leashplayers$update();
    }

    @Override
    public ActionResult leashplayers$interact(PlayerEntity player, Hand hand) {
        if (leashplayers$disabled()) return ActionResult.PASS;

        var stack = player.getStackInHand(hand);
        if (stack.isOf(Items.LEAD) && leashplayers$holder == null) {
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            leashplayers$attach(player);
            return ActionResult.SUCCESS;
        }

        if (leashplayers$holder == player && leashplayers$lastage + 20 < leashplayers$self.age) {
            if (!player.isCreative()) {
                leashplayers$drop();
            }
            leashplayers$detach();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
