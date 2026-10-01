package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.block.ModBlocks;
import com.synarsis.airtacticalarsenal.block.ShahedRadarBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SbwRadarLook {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-RadarLook");
    private static final String SBW = "superbwarfare";
    private static final ResourceLocation FUMO_25 = new ResourceLocation(SBW, "fumo_25");

    private static final ResourceLocation[][] LAYOUTS = {
            {new ResourceLocation(SBW, "geo/fumo_25.geo.json"), new ResourceLocation(SBW, "textures/block/fumo_25.png")},
            {new ResourceLocation(SBW, "models/bedrock/block/fumo_25.geo.json"), new ResourceLocation(SBW, "textures/bedrock/block/fumo_25.png")}
    };
    private static final String[] SPIN_BONES = {"mian", "rolling"};

    private static volatile BakedGeoModel model;
    private static volatile ResourceLocation texture;
    private static volatile String spinBone;

    private SbwRadarLook() {}

    public static BakedGeoModel model() {
        return model;
    }

    public static ResourceLocation texture() {
        return texture;
    }

    public static String spinBone() {
        return spinBone;
    }

    public static void disable(String reason, Throwable error) {
        if (model == null) return;
        model = null;
        ShahedRadarBlock.fumoModel = false;
        LOGGER.error("[ATA] SuperbWarfare FuMO 25 look disabled ({}), the radar uses the ATA model now", reason, error);
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        });
    }

    private static void probe(ResourceManager resources) {
        model = null;
        texture = null;
        spinBone = null;
        for (ResourceLocation[] layout : LAYOUTS) {
            if (resources.getResource(layout[0]).isEmpty() || resources.getResource(layout[1]).isEmpty()) continue;
            try {
                Model raw = FileLoader.loadModelFile(layout[0], resources);
                BakedGeoModel baked = BakedModelFactory.getForNamespace(SBW).constructGeoModel(GeometryTree.fromModel(raw));
                String bone = null;
                for (String name : SPIN_BONES) {
                    if (baked.getBone(name).isPresent()) {
                        bone = name;
                        break;
                    }
                }
                texture = layout[1];
                spinBone = bone;
                model = baked;
                LOGGER.info("[ATA] Radar uses the SuperbWarfare FuMO 25 model from {} (spinning part: {})", layout[0], bone == null ? "none" : bone);
                return;
            } catch (Throwable t) {
                LOGGER.warn("[ATA] Could not load the FuMO 25 model from {}", layout[0], t);
            }
        }
        LOGGER.warn("[ATA] SuperbWarfare FuMO 25 model not found in this SuperbWarfare version, the radar uses the ATA model");
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!ShahedRadarBlock.SBW_LOOK) return;
        try {
            probe(Minecraft.getInstance().getResourceManager());
        } catch (Throwable t) {
            model = null;
            LOGGER.warn("[ATA] FuMO 25 model check failed, the radar uses the ATA model", t);
        }
        boolean fumo = model != null;
        ShahedRadarBlock.fumoModel = fumo;

        Map<ResourceLocation, BakedModel> models = event.getModels();
        BakedModel item = models.get(new ModelResourceLocation(FUMO_25, "inventory"));
        if (item != null) {
            models.put(new ModelResourceLocation(ModBlocks.SHAHED_RADAR.getId(), "inventory"), item);
        } else {
            LOGGER.warn("[ATA] superbwarfare:fumo_25 item model not found, radar keeps the ATA icon");
        }

        if (!fumo) return;
        Block fumoBlock = ForgeRegistries.BLOCKS.containsKey(FUMO_25) ? ForgeRegistries.BLOCKS.getValue(FUMO_25) : null;
        BakedModel block = fumoBlock == null ? null : models.get(BlockModelShaper.stateToModelLocation(fumoBlock.defaultBlockState()));
        if (block != null) {
            for (BlockState state : ModBlocks.SHAHED_RADAR.get().getStateDefinition().getPossibleStates()) {
                models.put(BlockModelShaper.stateToModelLocation(state), block);
            }
        }
    }
}
