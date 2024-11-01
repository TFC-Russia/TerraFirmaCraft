/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.capabilities.forge;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Tooltips;

public enum ForgingBonus implements StringRepresentable
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

    /**
     * Add the item tooltip information for the forging bonus and author.
     * Same logic as in 1.21.x but different implementation
     * Will be moved to a ForgingBonusComponent class in 1.21.x
     */
    public static void addTooltipInfo(ItemStack stack, List<Component> tooltips)
    {
        final ForgingBonus bonus = ForgingBonus.get(stack);
        if (bonus != ForgingBonus.NONE)
        {
            final MutableComponent name = bonus.getDisplayName();
            tooltips.add(ForgingBonus.getAuthor(stack)
                .map(author -> Tooltips.author(name, author))
                .orElse(name));
            tooltips.add(Helpers.translateEnum(bonus).withStyle(ChatFormatting.GREEN));
        }
    }

    /**
     * Mimics unbreaking behavior for higher forging bonuses.
     * Will be moved to a ForgingBonusComponent class in 1.21.x
     *
     * @return {@code true} if the damage was consumed.
     * @see ItemStack#hurt(int, RandomSource, ServerPlayer)
     */
    public static boolean applyLikeUnbreaking(ItemStack stack, RandomSource random)
    {
        if (stack.isDamageableItem())
        {
            final ForgingBonus bonus = ForgingBonus.get(stack);
            if (bonus != ForgingBonus.NONE)
            {
                return random.nextFloat() < bonus.durability();
            }
        }
        return false;
    }

    /**
     * Get the forging bonus currently attached to an item stack.
     * Will be moved to a ForgingBonusComponent class in 1.21.x
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

    /**
     * Get the author of the forging bonus on an item stack.
     * Will be moved to a ForgingBonusComponent class in 1.21.x
     */
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
     * Will be moved to a ForgingBonusComponent class in 1.21.x
     */
    public static void set(ItemStack stack, ForgingBonus bonus)
    {
        set(stack, bonus, null);
    }

    /**
     * Set the forging bonus and author on an item stack
     * Will be moved to a ForgingBonusComponent in 1.21.x
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

    /**
     * Copy the forging bonus from one item stack to another. Keeps the author if present.
     * Will be moved to a ForgingBonusComponent in 1.21.x
     */
    public static ItemStack copy(ItemStack from, ItemStack to)
    {
        final ForgingBonus bonus = get(from);
        if (bonus != NONE)
        {
            set(to, bonus);

            final Optional<String> author = getAuthor(from);
            if (author.isPresent())
            {
                to.getOrCreateTag().putString(KEY_AUTHOR, author.get());
            }
        }
        return to;
    }

    private final String serializedName;
    private final DoubleSupplier minRatio;

    ForgingBonus(DoubleSupplier minRatio)
    {
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.minRatio = minRatio;
    }

    public MutableComponent getDisplayName()
    {
        return Helpers.translateEnum(this);
    }

    @Override
    public String getSerializedName()
    {
        return serializedName;
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
