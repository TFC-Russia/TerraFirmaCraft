/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities;

import java.util.Collections;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fmllegacy.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import net.dries007.tfc.common.items.TFCFishingRodItem;

public class TFCFishingHook extends FishingHook implements IEntityAdditionalSpawnData
{
    public TFCFishingHook(EntityType<? extends TFCFishingHook> type, Level level)
    {
        super(type, level);
    }

    public TFCFishingHook(Player player, Level level)
    {
        this(TFCEntities.FISHING_BOBBER.get(), level);
        this.setOwner(player);
        float f = player.getXRot();
        float f1 = player.getYRot();
        float f2 = Mth.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = Mth.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -Mth.cos(-f * ((float) Math.PI / 180F));
        float f5 = Mth.sin(-f * ((float) Math.PI / 180F));
        double d0 = player.getX() - (double) f3 * 0.3D;
        double d1 = player.getEyeY();
        double d2 = player.getZ() - (double) f2 * 0.3D;
        this.moveTo(d0, d1, d2, f1, f);
        Vec3 vec3 = new Vec3(-f3, Mth.clamp(-(f5 / f4), -5.0F, 5.0F), -f2);
        double d3 = vec3.length();
        vec3 = vec3.multiply(0.6D / d3 + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / d3 + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / d3 + 0.5D + this.random.nextGaussian() * 0.0045D);
        this.setDeltaMovement(vec3);
        this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(vec3.y, vec3.horizontalDistance()) * (double) (180F / (float) Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void tick()
    {
        super.tick();
        if (hookedIn != null && !hookedIn.isRemoved()) // due to some path-dependent client/server stuff in the big tick loop, we do this to be 100% sure the state is set correctly
        {
            currentState = FishHookState.HOOKED_IN_ENTITY;
            setPos(hookedIn.getX(), hookedIn.getY(0.8D), hookedIn.getZ());
        }
    }

    @Override
    public boolean shouldStopFishing(Player player)//todo: remove after forge PR #8168
    {
        boolean main = player.getMainHandItem().getItem() instanceof TFCFishingRodItem;
        boolean off = player.getOffhandItem().getItem() instanceof TFCFishingRodItem;
        if (!player.isRemoved() && player.isAlive() && (main || off) && !(distanceToSqr(player) > 1024.0D))
        {
            return false;
        }
        else
        {
            discard();
            return true;
        }
    }

    @Override
    public void catchingFish(BlockPos pos) { } // no-op fishing

    @Override
    public int retrieve(ItemStack stack)
    {
        Player player = getPlayerOwner();
        if (!level.isClientSide && player != null && !shouldStopFishing(player))
        {
            if (hookedIn != null)
            {
                pullEntity(hookedIn);
                if (hookedIn instanceof AbstractFish)
                {
                    player.awardStat(Stats.FISH_CAUGHT, 1);
                }
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, stack, this, Collections.emptyList());
                level.broadcastEntityEvent(this, (byte) 31);
            }
            if (hookedIn == null || hookedIn.isRemoved())
            {
                discard(); // change from vanilla -- lets you keep tugging on the thing while it's alive.
            }
            return onGround ? 2 : 1;
        }
        return 0;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer)
    {
        Entity owner = getOwner();
        buffer.writeVarInt(owner == null ? this.getId() : owner.getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData)
    {
        int id = additionalData.readVarInt();
        Entity entity = this.level.getEntity(id);
        if (entity != null)
        {
            this.setOwner(entity);
        }
        if (getPlayerOwner() == null)
        {
            LOGGER.error("Failed to recreate fishing hook on client. {} (id: {}) is not a valid owner.", this.level.getEntity(id), id);
            kill();
        }
    }

    @Override
    protected void pullEntity(Entity entity)
    {
        Entity owner = this.getOwner();
        if (owner != null)
        {
            double dx = owner.getX() - this.getX();
            double dy = owner.getY() - this.getY();
            double dz = owner.getZ() - this.getZ();
            double mod = 0.04D;
            entity.setDeltaMovement(new Vec3(dx * mod, dy * mod, dz * mod));
        }
    }

    @Override
    public Packet<?> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
