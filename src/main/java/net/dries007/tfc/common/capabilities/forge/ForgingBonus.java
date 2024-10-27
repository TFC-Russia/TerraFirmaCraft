/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.capabilities.forge;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;

public enum ForgingBonus
{
    NONE(() -> Double.POSITIVE_INFINITY),
    MODESTLY_FORGED(TFCConfig.SERVER.anvilModestlyForgedThreshold::get),
    WELL_FORGED(TFCConfig.SERVER.anvilWellForgedThreshold::get),
    EXPERTLY_FORGED(TFCConfig.SERVER.anvilExpertForgedThreshold::get),
    PERFECTLY_FORGED(TFCConfig.SERVER.anvilPerfectlyForgedThreshold::get);

    private static final String KEY = "tfc:forging_bonus";
    private static final String KEY_AUTHOR = "tfc:forging_author";
    private static final ForgingBonus[] VALUES = values();

    public static ForgingBonus valueOf(int i)
    {
        return i < 0 ? VALUES[0] : (i >= VALUES.length ? VALUES[VALUES.length - 1] : VALUES[i]);
    }

    public static ForgingBonus byRatio(float ratio)
    {
        for (int i = VALUES.length - 1; i > 0; i--)
        {
            if (VALUES[i].minRatio.getAsDouble() > ratio)
            {
                return VALUES[i];
            }
        }
        return NONE;
    }

    public static void addTooltipInfo(ItemStack stack, List<Component> tooltips)
    {
        final ForgingBonus bonus = get(stack);
        if (bonus != NONE)
        {
            tooltips.add(Helpers.translateEnum(bonus).withStyle(ChatFormatting.GREEN));
        }

        final Optional<String> author = getAuthor(stack);
        if (author.isPresent() && !author.get().isEmpty())
        {
            tooltips.add(Component.translatable("tfc.tooltip.author", author.get()).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Mimics unbreaking behavior for higher forging bonuses.
     *
     * @return {@code true} if the damage was consumed.
     * @see ItemStack#hurt(int, RandomSource, ServerPlayer)
     */
    public static boolean applyLikeUnbreaking(ItemStack stack, RandomSource random)
    {
        if (stack.isDamageableItem())
        {
            final ForgingBonus bonus = get(stack);
            if (bonus != NONE)
            {
                return random.nextFloat() < bonus.durability();
            }
        }
        return false;
    }

    /**
     * Get the forging bonus currently attached to an item stack.
     */
    public static ForgingBonus get(ItemStack stack)
    {
        final CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY, Tag.TAG_INT))
        {
            return valueOf(tag.getInt(KEY));
        }
        return NONE;
    }

    public static Optional<String> getAuthor(ItemStack stack)
    {
        final CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY_AUTHOR, Tag.TAG_STRING))
        {
            return Optional.of(tag.getString(KEY_AUTHOR));
        }
        return Optional.empty();
    }

    /**
     * Set the forging bonus on an item stack
     */
    public static void set(ItemStack stack, ForgingBonus bonus)
    {
        if (bonus != NONE)
        {
            stack.getOrCreateTag().putInt(KEY, bonus.ordinal());
        }
        else
        {
            stack.removeTagKey(KEY);
        }
    }

    /**
     * Set the forging bonus and author on an item stack
     */
    public static void set(ItemStack stack, ForgingBonus bonus, Player player)
    {
        if (bonus != NONE)
        {
            stack.getOrCreateTag().putInt(KEY, bonus.ordinal());

            if (player != null) {
                stack.getOrCreateTag().putString(KEY_AUTHOR, player.getName().getString());
            }
        }
        else
        {
            stack.removeTagKey(KEY);
        }
    }

    private final DoubleSupplier minRatio;

    ForgingBonus(DoubleSupplier minRatio)
    {
        this.minRatio = minRatio;
    }

    public float efficiency()
    {
        return Helpers.lerp(ordinal() * 0.25f, 1.0f, TFCConfig.SERVER.anvilMaxEfficiencyMultiplier.get().floatValue());
    }

    public float durability()
    {
        return Helpers.lerp(ordinal() * 0.25f, 0f, TFCConfig.SERVER.anvilMaxDurabilityMultiplier.get().floatValue());
    }

    public float damage()
    {
        return Helpers.lerp(ordinal() * 0.25f, 1.0f, TFCConfig.SERVER.anvilMaxDamageMultiplier.get().floatValue());
    }
}
