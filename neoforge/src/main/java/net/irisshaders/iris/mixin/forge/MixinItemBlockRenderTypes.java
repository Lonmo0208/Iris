package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumMap;
import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public class MixinItemBlockRenderTypes {
    @Unique
    private static final EnumMap<BlockRenderType, ChunkRenderTypeSet> RENDER_TYPE_CACHE = new EnumMap<>(BlockRenderType.class);

    static {
        // 预先生成所有枚举值对应的渲染类型集合
        for (BlockRenderType type : BlockRenderType.values()) {
            RenderType renderType = BlockMaterialMapping.convertBlockToRenderType(type);
            RENDER_TYPE_CACHE.put(type, ChunkRenderTypeSet.of(renderType));
        }
    }

    @Inject(
        method = "getRenderLayers",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void iris$setCustomRenderType(
        BlockState state,
        CallbackInfoReturnable<ChunkRenderTypeSet> cir
    ) {
        final Map<Block, BlockRenderType> blockTypeMap = WorldRenderingSettings.INSTANCE.getBlockTypeIds();
        if (blockTypeMap == null) return;

        final Block block = state.getBlock();
        final BlockRenderType renderType = blockTypeMap.get(block);
        
        if (renderType != null) {
            cir.setReturnValue(RENDER_TYPE_CACHE.get(renderType));
        }
    }
}