/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.component.forge;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
// import net.minecraft.network.codec.StreamCodec;
// import net.minecraft.util.StringRepresentable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.capabilities.forge.ForgingBonus;
import net.dries007.tfc.util.Tooltips;

public record ForgingBonusComponent(
    ForgingBonus type,
    Optional<String> author
)
{
    public static void addTooltipInfo(ItemStack stack, List<Component> tooltips)
    {
        final @Nullable ForgingBonus bonus = get(stack);
        if (bonus != null && bonus != ForgingBonus.NONE)
        {
            final MutableComponent name = bonus.getDisplayName();
            tooltips.add(ForgingBonus.getAuthor(stack)
                .map(author -> Tooltips.author(name, author))
                .orElse(name));
        }
    }

    /**
     * Mimics unbreaking-like effects for items with a forging bonus. This hooks into the method enchantments use to apply durability
    * based effects, in {@link EnchantmentHelper#processDurabilityChange}, and modifies the amount of damage taken. We base the system
    * off of how enchantments work - each durability point of damage has a chance to not apply (binomial distribution)
    */
    public static int applyLikeUnbreaking(ItemStack stack, RandomSource random, int originalDamage)
    {
        final ForgingBonus bonus = get(stack);

        int damage = originalDamage;
        if (bonus != ForgingBonus.NONE)
        {
            for (int i = 0; i < damage; i++)
            {
                if (random.nextFloat() < bonus.durability())
                {
                    damage--;
                }
            }
        }
        return damage;
    }

    public static ItemStack copy(ItemStack from, ItemStack to)
    {
        final ForgingBonus bonus = get(from);
        if (bonus != null)
        {
            set(to, bonus);
        }
        return to;
    }

    /**
     * Get the forging bonus currently attached to an item stack. If the stack has no bonus, {@link ForgingBonus#NONE} will be returned.
    */
    public static ForgingBonus get(ItemStack stack)
    {
        return ForgingBonus.get(stack);
    }

    /**
     * Set the forging bonus on an item stack
    */
    public static void set(ItemStack stack, ForgingBonus bonus)
    {
        set(stack, bonus, null);
    }

    public static void set(ItemStack stack, ForgingBonus bonus, @Nullable Player player)
    {
        if (bonus != ForgingBonus.NONE)
        {
            ForgingBonus.set(stack, bonus, player);
        }
    }
}
