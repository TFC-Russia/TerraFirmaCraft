/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.misc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.entities.TFCEntities;

public class Seat extends Entity
{
    public static void sit(Level level, BlockPos pos, Entity sitter)
    {
        if (!level.isClientSide)
        {
            Seat seat = TFCEntities.SEAT.get().create(level);
            assert seat != null;
            seat.moveTo(pos, 0f, 0f);
            level.addFreshEntity(seat);
            sitter.startRiding(seat);
        }
    }

    @Nullable
    public static Entity getSittingEntity(Level level, BlockPos pos)
    {
        List<Seat> entities = level.getEntitiesOfClass(Seat.class, new AABB(pos));
        if (!entities.isEmpty())
        {
            List<Entity> passengers = entities.get(0).getPassengers();
            if (!passengers.isEmpty())
            {
                return passengers.get(0);
            }
        }
        return null;
    }

    private static final Vec3 ATTACHMENT_POINT = new Vec3(0, -0.25, 0);

    public Seat(EntityType<?> type, Level level)
    {
        super(type, level);
        noPhysics = true;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!isVehicle())
        {
            setRemoved(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick)
    {
        return ATTACHMENT_POINT;
    }

    @Override
    protected boolean repositionEntityAfterLoad()
    {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
