package is.bobbys;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class DmgU {
    public static float handDamage(EntityPlayer player){
        float dmg=getSwordDamage(player.getHeldItem());
        if (player.getHeldItem().isItemEnchanted()) dmg++;
        return dmg;
    }
    public static float getSwordDamage(ItemStack stack) {

        if (stack == null)
            return 1;

        Item item = stack.getItem();

        if (item == Items.wooden_sword) {
            return 4.0F;
        }

        if (item == Items.stone_sword) {
            return 5.0F;
        }

        if (item == Items.iron_sword) {
            return 6.0F;
        }

        if (item == Items.diamond_sword) {
            return 7.0F;
        }

        return 0;
    }
}
