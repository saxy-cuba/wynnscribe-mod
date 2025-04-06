package com.wynnscribe.fabric;

import com.wynnscribe.Wynnscribe;
import net.fabricmc.api.ClientModInitializer;

public class WynnscribeFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Wynnscribe.INSTANCE.init();
    }
}
