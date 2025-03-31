package net.irisshaders.iris.compat.acceleratedrendering.gui;


import com.github.argon4w.acceleratedrendering.AcceleratedRenderingModEntry;
import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.Iris;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.LoadingModList;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;


@EventBusSubscriber
public class EmbeddiumEvent {

    public static OptionPage page;

    @SubscribeEvent
    public static void onGui(OptionGUIConstructionEvent event){
        if(LoadingModList.get().getModFileById("acceleratedrendering") == null)return;

        OptionGroup core = OptionGroup.createBuilder()
                .add(AROptions.corePooledBufferSetSize)
                .add(AROptions.corePooledElementBufferSize)
                .add(AROptions.coreCachedImageSize)
                .add(AROptions.coreForceTranslucentAcceleration)
                .add(AROptions.coreCacheSamePose)
                .setId(AcceleratedRenderingModEntry.location("configuration.core_settings"))
                .build();
        OptionGroup entity = OptionGroup.createBuilder()
                .add(AROptions.acceleratedEntityRenderingFeatureStatus)
                .add(AROptions.acceleratedEntityRenderingDefaultPipeline)
                .add(AROptions.acceleratedEntityRenderingMeshType)
                .setId(AcceleratedRenderingModEntry.location("configuration.accelerated_entity_rendering"))
                .build();
        OptionGroup blockEntity = OptionGroup.createBuilder()
                .add(AROptions.acceleratedBlockEntityRenderingFeatureStatus)
                .add(AROptions.acceleratedBlockEntityRenderingDefaultPipeline)
                .add(AROptions.acceleratedBlockEntityRenderingMeshType)
                .setId(AcceleratedRenderingModEntry.location("configuration.accelerated_block_entity_rendering"))
                .build();
        OptionGroup text = OptionGroup.createBuilder()
                .add(AROptions.acceleratedTextRenderingFeatureStatus)
                .add(AROptions.acceleratedTextRenderingDefaultPipeline)
                .add(AROptions.acceleratedTextRenderingMeshType)
                .setId(AcceleratedRenderingModEntry.location("configuration.accelerated_text_rendering"))
                .build();
        OptionGroup item = OptionGroup.createBuilder()
                .add(AROptions.acceleratedItemRenderingFeatureStatus)
                .add(AROptions.acceleratedItemRenderingDefaultPipeline)
                .add(AROptions.acceleratedItemRenderingMeshType)
                .add(AROptions.acceleratedItemRenderingBakeMeshForQuads)
                .setId(AcceleratedRenderingModEntry.location("configuration.accelerated_item_rendering"))
                .build();
        OptionGroup culling = OptionGroup.createBuilder()
                .add(AROptions.normalCullingFeatureStatus)
                .add(AROptions.normalCullingDefaultCulling)
                .add(AROptions.normalCullingIgnoreCullState)
                .setId(AcceleratedRenderingModEntry.location("configuration.normal_culling"))
                .build();
        OptionGroup compatibility = OptionGroup.createBuilder()
                .add(AROptions.irisCompatFeatureStatus)
                .add(AROptions.irisCompatNormalCullingCompat)
                .add(AROptions.irisCompatShadowCulling)
                .add(AROptions.irisCompatEntitiesCompat)
                .add(AROptions.irisCompatPolygonProcessing)
                .add(AROptions.irisCompatFastRenderTypeCheck)
                .setId(AcceleratedRenderingModEntry.location("configuration.iris_compatibility"))
                .build();

        page = new OptionPage(OptionIdentifier.create(ResourceLocation.fromNamespaceAndPath(Iris.MODID, "accelerated_rendering")),
                Component.translatable("acceleratedrendering.configuration.title"),
                ImmutableList.of(core, entity, blockEntity, item, text, culling, compatibility));
        event.addPage(page);
    }

}
