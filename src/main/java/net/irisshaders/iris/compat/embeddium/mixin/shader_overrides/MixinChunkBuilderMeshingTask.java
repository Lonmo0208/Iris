package net.irisshaders.iris.compat.embeddium.mixin.shader_overrides;

import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(ChunkBuilderMeshingTask.class)
public class MixinChunkBuilderMeshingTask {
    @Unique
    private static final ChunkRenderTypeSet[] LAYER_SET;

    static {
        LAYER_SET = new ChunkRenderTypeSet[BlockRenderType.values().length];
        for (int i = 0; i < BlockRenderType.values().length; i++) {
            LAYER_SET[i] = ChunkRenderTypeSet.of(BlockMaterialMapping.convertBlockToRenderType(BlockRenderType.values()[i]));
        }
    }
    /**
     * @author embeddedt
     * @reason On Forge, render types are not intended to be driven by a central registry like in vanilla; instead, they
     * get queried from the block model during meshing. Only the default baked models defer to the vanilla registry;
     * specifying a render type in the model JSON or using a custom model will return its own value. Thus, we need
     * to redirect the access at a higher level.
     */
    @Redirect(method = "execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;)Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;"),
            remap = false)
    private ChunkRenderTypeSet oculus$overrideRenderTypes(BakedModel instance, BlockState blockState, RandomSource randomSource, ModelData modelData) {
        Map<Block, BlockRenderType> idMap = WorldRenderingSettings.INSTANCE.getBlockTypeIds();
        if (idMap != null) {
            BlockRenderType type = idMap.get(blockState.getBlock());
            if (type != null) {
                return LAYER_SET[type.ordinal()];
            }
        }

        return instance.getRenderTypes(blockState, randomSource, modelData);
    }
}

