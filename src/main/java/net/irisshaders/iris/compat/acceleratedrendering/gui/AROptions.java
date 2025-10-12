package net.irisshaders.iris.compat.acceleratedrendering.gui;

import com.github.argon4w.acceleratedrendering.configs.FeatureConfig;
import com.github.argon4w.acceleratedrendering.configs.FeatureStatus;
import com.github.argon4w.acceleratedrendering.configs.PipelineSetting;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import net.irisshaders.iris.compat.embeddium.impl.config.ConfigValueStorage;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.embeddedt.embeddium.api.options.binding.GenericBinding;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionImpact;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;

public class AROptions {
    
    public static final Option<Integer> corePooledBufferSetSize;
    public static final Option<Integer> corePooledElementBufferSize;
    public static final Option<Integer> coreCachedImageSize;
    public static final Option<FeatureStatus> coreForceTranslucentAcceleration;
    public static final Option<FeatureStatus> coreCacheIdenticalPose;
    public static final Option<FeatureStatus> acceleratedEntityRenderingFeatureStatus;
    public static final Option<PipelineSetting> acceleratedEntityRenderingDefaultPipeline;
    public static final Option<MeshType> acceleratedEntityRenderingMeshType;
    public static final Option<FeatureStatus> acceleratedTextRenderingFeatureStatus;
    public static final Option<PipelineSetting> acceleratedTextRenderingDefaultPipeline;
    public static final Option<MeshType> acceleratedTextRenderingMeshType;
    public static final Option<FeatureStatus> acceleratedItemRenderingFeatureStatus;
    public static final Option<FeatureStatus> acceleratedItemRenderingBakeMeshForQuads;
    public static final Option<PipelineSetting> acceleratedItemRenderingDefaultPipeline;
    public static final Option<MeshType> acceleratedItemRenderingMeshType;
    public static final Option<FeatureStatus> acceleratedBlockEntityRenderingFeatureStatus;
    public static final Option<PipelineSetting> acceleratedBlockEntityRenderingDefaultPipeline;
    public static final Option<MeshType> acceleratedBlockEntityRenderingMeshType;
    public static final Option<FeatureStatus> orientationCullingFeatureStatus;
    public static final Option<FeatureStatus> orientationCullingDefaultCulling;
    public static final Option<FeatureStatus> orientationCullingIgnoreCullState;
    public static final Option<FeatureStatus> irisCompatFeatureStatus;
    public static final Option<FeatureStatus> irisCompatOrientationCullingCompat;
    public static final Option<FeatureStatus> irisCompatShadowCulling;
    public static final Option<FeatureStatus> irisCompatEntitiesCompat;
    public static final Option<FeatureStatus> irisCompatPolygonProcessing;
    public static final Option<FeatureStatus> irisCompatFastRenderTypeCheck;

    public static final GenericBinding<ModConfigSpec.ConfigValue<Integer>,Integer> integerBinding = new GenericBinding<>(
            ModConfigSpec.ConfigValue::set,
            ModConfigSpec.ConfigValue::get
    );
    public static final GenericBinding<ModConfigSpec.ConfigValue<FeatureStatus>,FeatureStatus> featureStatusBinding = new GenericBinding<>(
            ModConfigSpec.ConfigValue::set,
            ModConfigSpec.ConfigValue::get
    );
    public static final GenericBinding<ModConfigSpec.ConfigValue<PipelineSetting>,PipelineSetting> pipelineSettingBinding = new GenericBinding<>(
            ModConfigSpec.ConfigValue::set,
            ModConfigSpec.ConfigValue::get
    );
    public static final GenericBinding<ModConfigSpec.ConfigValue<MeshType>,MeshType> meshTypeBinding = new GenericBinding<>(
            ModConfigSpec.ConfigValue::set,
            ModConfigSpec.ConfigValue::get
    );

    static {
        corePooledBufferSetSize = OptionImpl.createBuilder(Integer.TYPE,new ConfigValueStorage<>(FeatureConfig.CONFIG.corePooledBufferSetSize))
                .setId(ARModInfo.location("core_settings.pooled_buffer_set_size"))
                .setName(Component.translatable("acceleratedrendering.configuration.core_settings.pooled_buffer_set_size"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.core_settings.pooled_buffer_set_size.tooltip"))
                .setControl(e->new SliderControl(e,1,16,1, ControlValueFormatter.number()))
                .setBinding(integerBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        corePooledElementBufferSize = OptionImpl.createBuilder(Integer.TYPE,new ConfigValueStorage<>(FeatureConfig.CONFIG.corePooledElementBufferSize))
                .setId(ARModInfo.location("core_settings.pooled_element_buffer_size"))
                .setName(Component.translatable("acceleratedrendering.configuration.core_settings.pooled_element_buffer_size"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.core_settings.pooled_element_buffer_size.tooltip"))
                .setControl(e->new SliderControl(e,1,256,1, ControlValueFormatter.number()))
                .setBinding(integerBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        coreCachedImageSize = OptionImpl.createBuilder(Integer.TYPE,new ConfigValueStorage<>(FeatureConfig.CONFIG.coreCachedImageSize))
                .setId(ARModInfo.location("core_settings.cached_image_size"))
                .setName(Component.translatable("acceleratedrendering.configuration.core_settings.cached_image_size"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.core_settings.cached_image_size.tooltip"))
                .setControl(e->new SliderControl(e,1,128,1, ControlValueFormatter.number()))
                .setBinding(integerBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        coreForceTranslucentAcceleration = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.coreForceTranslucentAcceleration))
                .setId(ARModInfo.location("core_settings.force_translucent_acceleration"))
                .setName(Component.translatable("acceleratedrendering.configuration.core_settings.force_translucent_acceleration"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.core_settings.force_translucent_acceleration.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.HIGH)
                .build();
        coreCacheIdenticalPose = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.coreCacheIdenticalPose))
                .setId(ARModInfo.location("core_settings.cache_identical_pose"))
                .setName(Component.translatable("acceleratedrendering.configuration.core_settings.cache_identical_pose"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.core_settings.cache_identical_pose.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.MEDIUM)
                .build();
        acceleratedEntityRenderingFeatureStatus = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedEntityRenderingFeatureStatus))
                .setId(ARModInfo.location("accelerated_entity_rendering.feature_status"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_entity_rendering.feature_status"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_entity_rendering.feature_status.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.HIGH)
                .build();
        acceleratedEntityRenderingDefaultPipeline = OptionImpl.createBuilder(PipelineSetting.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedEntityRenderingDefaultPipeline))
                .setId(ARModInfo.location("accelerated_entity_rendering.default_pipeline"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_entity_rendering.default_pipeline"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_entity_rendering.default_pipeline.tooltip"))
                .setControl(e->new CyclingControl<>(e,PipelineSetting.class))
                .setBinding(pipelineSettingBinding)
                .build();
        acceleratedEntityRenderingMeshType = OptionImpl.createBuilder(MeshType.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedEntityRenderingMeshType))
                .setId(ARModInfo.location("accelerated_entity_rendering.mesh_type"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_entity_rendering.mesh_type"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_entity_rendering.mesh_type.tooltip"))
                .setControl(e->new CyclingControl<>(e,MeshType.class))
                .setBinding(meshTypeBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        acceleratedTextRenderingFeatureStatus = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedTextRenderingFeatureStatus))
                .setId(ARModInfo.location("accelerated_text_rendering.feature_status"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_text_rendering.feature_status"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_text_rendering.feature_status.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.LOW)
                .build();
        acceleratedTextRenderingDefaultPipeline = OptionImpl.createBuilder(PipelineSetting.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedTextRenderingDefaultPipeline))
                .setId(ARModInfo.location("accelerated_text_rendering.default_pipeline"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_text_rendering.default_pipeline"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_text_rendering.default_pipeline.tooltip"))
                .setControl(e->new CyclingControl<>(e,PipelineSetting.class))
                .setBinding(pipelineSettingBinding)
                .build();
        acceleratedTextRenderingMeshType = OptionImpl.createBuilder(MeshType.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedTextRenderingMeshType))
                .setId(ARModInfo.location("accelerated_text_rendering.mesh_type"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_text_rendering.mesh_type"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_text_rendering.mesh_type.tooltip"))
                .setControl(e->new CyclingControl<>(e,MeshType.class))
                .setBinding(meshTypeBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        acceleratedItemRenderingFeatureStatus = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedItemRenderingFeatureStatus))
                .setId(ARModInfo.location("accelerated_item_rendering.feature_status"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.feature_status"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.feature_status.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.LOW)
                .build();
        acceleratedItemRenderingDefaultPipeline = OptionImpl.createBuilder(PipelineSetting.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedItemRenderingDefaultPipeline))
                .setId(ARModInfo.location("accelerated_item_rendering.default_pipeline"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.default_pipeline"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.default_pipeline.tooltip"))
                .setControl(e->new CyclingControl<>(e,PipelineSetting.class))
                .setBinding(pipelineSettingBinding)
                .build();
        acceleratedItemRenderingMeshType = OptionImpl.createBuilder(MeshType.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedItemRenderingMeshType))
                .setId(ARModInfo.location("accelerated_item_rendering.mesh_type"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.mesh_type"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.mesh_type.tooltip"))
                .setControl(e->new CyclingControl<>(e,MeshType.class))
                .setBinding(meshTypeBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        acceleratedItemRenderingBakeMeshForQuads = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedItemRenderingBakeMeshForQuads))
                .setId(ARModInfo.location("accelerated_item_rendering.bake_mesh_for_quads"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.bake_mesh_for_quads"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_item_rendering.bake_mesh_for_quads.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        acceleratedBlockEntityRenderingFeatureStatus = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedBlockEntityRenderingFeatureStatus))
                .setId(ARModInfo.location("accelerated_block_entity_rendering.feature_status"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_block_entity_rendering.feature_status"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_block_entity_rendering.feature_status.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .setImpact(OptionImpact.LOW)
                .build();
        acceleratedBlockEntityRenderingDefaultPipeline = OptionImpl.createBuilder(PipelineSetting.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedBlockEntityRenderingDefaultPipeline))
                .setId(ARModInfo.location("accelerated_block_entity_rendering.default_pipeline"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_block_entity_rendering.default_pipeline"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_block_entity_rendering.default_pipeline.tooltip"))
                .setControl(e->new CyclingControl<>(e,PipelineSetting.class))
                .setBinding(pipelineSettingBinding)
                .build();
        acceleratedBlockEntityRenderingMeshType = OptionImpl.createBuilder(MeshType.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.acceleratedBlockEntityRenderingMeshType))
                .setId(ARModInfo.location("accelerated_block_entity_rendering.mesh_type"))
                .setName(Component.translatable("acceleratedrendering.configuration.accelerated_block_entity_rendering.mesh_type"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.accelerated_block_entity_rendering.mesh_type.tooltip"))
                .setControl(e->new CyclingControl<>(e,MeshType.class))
                .setBinding(meshTypeBinding)
                .setImpact(OptionImpact.VARIES)
                .build();
        orientationCullingFeatureStatus = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.orientationCullingFeatureStatus))
                .setId(ARModInfo.location("orientation_culling.feature_status"))
                .setName(Component.translatable("acceleratedrendering.configuration.orientation_culling.feature_status"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.orientation_culling.feature_status.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        orientationCullingDefaultCulling = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.orientationCullingDefaultCulling))
                .setId(ARModInfo.location("orientation_culling.default_culling"))
                .setName(Component.translatable("acceleratedrendering.configuration.orientation_culling.default_culling"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.orientation_culling.default_culling.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        orientationCullingIgnoreCullState = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.orientationCullingIgnoreCullState))
                .setId(ARModInfo.location("orientation_culling.ignore_cull_state"))
                .setName(Component.translatable("acceleratedrendering.configuration.orientation_culling.ignore_cull_state"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.orientation_culling.ignore_cull_state.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        irisCompatFeatureStatus = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.irisCompatFeatureStatus))
                .setId(ARModInfo.location("iris_compatibility.feature_status"))
                .setName(Component.translatable("acceleratedrendering.configuration.iris_compatibility.feature_status"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.iris_compatibility.feature_status.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        irisCompatOrientationCullingCompat = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.irisCompatOrientationCullingCompat))
                .setId(ARModInfo.location("iris_compatibility.orientation_culling_compatibility"))
                .setName(Component.translatable("acceleratedrendering.configuration.iris_compatibility.orientation_culling_compatibility"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.iris_compatibility.orientation_culling_compatibility.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        irisCompatShadowCulling = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.irisCompatShadowCulling))
                .setId(ARModInfo.location("iris_compatibility.shadow_culling"))
                .setName(Component.translatable("acceleratedrendering.configuration.iris_compatibility.shadow_culling"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.iris_compatibility.shadow_culling.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        irisCompatEntitiesCompat = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.irisCompatEntitiesCompat))
                .setId(ARModInfo.location("iris_compatibility.entities_compatibility"))
                .setName(Component.translatable("acceleratedrendering.configuration.iris_compatibility.entities_compatibility"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.iris_compatibility.entities_compatibility.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        irisCompatPolygonProcessing = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.irisCompatPolygonProcessing))
                .setId(ARModInfo.location("iris_compatibility.polygon_processing"))
                .setName(Component.translatable("acceleratedrendering.configuration.iris_compatibility.polygon_processing"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.iris_compatibility.polygon_processing.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
        irisCompatFastRenderTypeCheck = OptionImpl.createBuilder(FeatureStatus.class,new ConfigValueStorage<>(FeatureConfig.CONFIG.irisCompatFastRenderTypeCheck))
                .setId(ARModInfo.location("iris_compatability.fast_render_type_check"))
                .setName(Component.translatable("acceleratedrendering.configuration.iris_compatability.fast_render_type_check"))
                .setTooltip(Component.translatable("acceleratedrendering.configuration.iris_compatability.fast_render_type_check.tooltip"))
                .setControl(FeatureStatusTickBoxControl::new)
                .setBinding(featureStatusBinding)
                .build();
    }
    
}
