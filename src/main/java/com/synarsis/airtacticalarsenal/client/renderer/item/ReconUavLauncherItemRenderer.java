package com.synarsis.airtacticalarsenal.client.renderer.item;

import com.synarsis.airtacticalarsenal.client.model.item.ReconUavLauncherItemModel;
import com.synarsis.airtacticalarsenal.item.ReconUavLauncherItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class ReconUavLauncherItemRenderer extends GeoItemRenderer<ReconUavLauncherItem> {

    public ReconUavLauncherItemRenderer() {
        super(new ReconUavLauncherItemModel());
    }
}
