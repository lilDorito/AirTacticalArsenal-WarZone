package com.synarsis.airtacticalarsenal.client.renderer.item;

import com.synarsis.airtacticalarsenal.client.model.item.LightUavLauncherItemModel;
import com.synarsis.airtacticalarsenal.item.LightUavLauncherItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class LightUavLauncherItemRenderer extends GeoItemRenderer<LightUavLauncherItem> {

    public LightUavLauncherItemRenderer() {
        super(new LightUavLauncherItemModel());
    }
}
