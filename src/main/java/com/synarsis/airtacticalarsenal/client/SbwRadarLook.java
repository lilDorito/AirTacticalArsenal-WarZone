package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.block.ModBlocks;
import com.synarsis.airtacticalarsenal.block.ShahedRadarBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SbwRadarLook {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-RadarLook");
    private static final ResourceLocation FUMO_25 = new ResourceLocation("superbwarfare", "fumo_25");

    private SbwRadarLook() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!ShahedRadarBlock.SBW_LOOK) return;
        Map<ResourceLocation, BakedModel> models = event.getModels();

        BakedModel item = models.get(new ModelResourceLocation(FUMO_25, "inventory"));
        if (item != null) {
            models.put(new ModelResourceLocation(ModBlocks.SHAHED_RADAR.getId(), "inventory"), item);
        } else {
            LOGGER.warn("[ATA] superbwarfare:fumo_25 item model not found, radar keeps the ATA icon");
        }

        Block fumo = ForgeRegistries.BLOCKS.getValue(FUMO_25);
        BakedModel block = fumo == null ? null : models.get(BlockModelShaper.stateToModelLocation(fumo.defaultBlockState()));
        if (block != null) {
            for (BlockState state : ModBlocks.SHAHED_RADAR.get().getStateDefinition().getPossibleStates()) {
                models.put(BlockModelShaper.stateToModelLocation(state), block);
            }
        }
    }
}
