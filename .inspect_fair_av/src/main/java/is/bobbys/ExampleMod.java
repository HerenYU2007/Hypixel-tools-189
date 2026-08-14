package is.bobbys;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import static is.bobbys.AutoClicker.atkKey;
import static is.bobbys.Bridge.bridgeKey;
import static is.bobbys.Zoom.zoomKey;

@Mod(modid = ExampleMod.MODID, version = ExampleMod.VERSION)
public class ExampleMod {
    public static final String MODID = "fair_av";
    public static final String VERSION = "1.0";

    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(zoomKey);
        ClientRegistry.registerKeyBinding(bridgeKey);
        ClientRegistry.registerKeyBinding(atkKey);
        System.out.println("Fair Advanced!!!!!");
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }
}
