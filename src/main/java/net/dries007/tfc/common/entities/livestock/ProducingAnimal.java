/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.livestock;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.config.animals.ProducingAnimalConfig;
import net.dries007.tfc.util.calendar.Calendars;

public abstract class ProducingAnimal extends TFCAnimal
{
    public static final EntityDataAccessor<Long> DATA_PRODUCED = SynchedEntityData.defineId(ProducingAnimal.class, EntityDataSerializers.LONG);

    protected final Supplier<Integer> produceTicks;
    protected final Supplier<Double> produceFamiliarity;

    public ProducingAnimal(EntityType<? extends TFCAnimal> type, Level level, TFCSounds.EntityId sounds, ProducingAnimalConfig config)
    {
        super(type, level, sounds, config.inner());
        this.produceTicks = config.produceTicks();
        this.produceFamiliarity = config.produceFamiliarity();
    }

    @Override
    public boolean isReadyForAnimalProduct()
    {
        return getFamiliarity() > produceFamiliarity.get() && hasProduct();
    }

    @Override
    public void setProductsCooldown()
    {
        setProducedTick(Calendars.get(level()).getTicks());
    }

    @Override
    public long getProductsCooldown()
    {
        return Math.max(0, produceTicks.get() + getProducedTick() - Calendars.get(level()).getTicks());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt)
    {
        super.addAdditionalSaveData(nbt);
        nbt.putLong("produced", getProducedTick());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt)
    {
        super.readAdditionalSaveData(nbt);
        setProducedTick(nbt.getLong("produced"));
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_PRODUCED, 0L);
    }

    public long getProducedTick()
    {
        return entityData.get(DATA_PRODUCED);
    }

    public void setProducedTick(long producedTick)
    {
        entityData.set(DATA_PRODUCED, producedTick);
    }

}
