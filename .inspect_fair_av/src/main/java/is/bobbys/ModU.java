package is.bobbys;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ModU {
    public static int myColor=-1;
    public static int getPlayerColor(EntityPlayer player){
        int color = -1;
        ItemStack helmet = player.getCurrentArmor(3);
        if (helmet != null && helmet.hasTagCompound()) {
            NBTTagCompound display = helmet.getSubCompound("display", false);
            if (display != null && display.hasKey("color")) {
                color = display.getInteger("color");
            }
        }
        return color;
    }
    public static int getProtection(EntityPlayer player) {
        ItemStack h = player.getCurrentArmor(1);
        if (h == null) {
            return 0;
        }
        return EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId,h);
    }
    public static String colorToString(int color){
        if (color != -1) {
            return "0x"+Integer.toHexString((color >> 16) & 0xFF)+Integer.toHexString((color >> 8) & 0xFF)+Integer.toHexString(color & 0xFF);
        }
        return null;
    }
}
