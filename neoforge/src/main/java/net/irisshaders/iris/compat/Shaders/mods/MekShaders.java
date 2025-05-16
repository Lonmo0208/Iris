package net.irisshaders.iris.compat.Shaders.mods;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.irisshaders.iris.compat.Shaders.ModdedShaderPipeline;
import net.irisshaders.iris.compat.Shaders.impl.ProgramSetExtension;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.stream.Collectors;

public class MekShaders {

    public static final ResourceLocation SPS = ResourceLocation.fromNamespaceAndPath("mekanism", "rendertype_sps");
    public static final ResourceLocation MEKASUIT = ResourceLocation.fromNamespaceAndPath("mekanism", "rendertype_mekasuit");
    public static final ResourceLocation FLAME = ResourceLocation.fromNamespaceAndPath("mekanism", "rendertype_flame");

    static {
        init();
    }

    public static void init() {
        ModdedShaderPipeline.addShaderFromJson(MEKASUIT, AlphaTests.ONE_TENTH_ALPHA, DefaultVertexFormat.NEW_ENTITY, false, ProgramId.EntitiesTrans);
        ModdedShaderPipeline.addShaderFromJson(SPS, AlphaTests.ONE_TENTH_ALPHA, DefaultVertexFormat.POSITION_TEX_COLOR, false, ProgramId.EntitiesTrans);
        ModdedShaderPipeline.addShaderFromJson(FLAME, AlphaTests.ONE_TENTH_ALPHA, DefaultVertexFormat.POSITION_TEX_COLOR, false, ProgramId.SpiderEyes);
    }

    private static ProgramSource getSource(String program, ProgramSet programSet) {
        try {
            var vertexOpt = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("iris", "shaders/" + program + ".vsh"));
            String vertexSource = null;
            if (vertexOpt.isPresent()) {
                vertexSource = vertexOpt.get().openAsReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }

            var geometryOpt = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("iris", "shaders/" + program + ".gsh"));
            String geometrySource = null;
            if (geometryOpt.isPresent()) {
                geometrySource = geometryOpt.get().openAsReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }

            var tessControlOpt = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("iris", "shaders/" + program + ".tcs"));
            String tessControlSource = null;
            if (tessControlOpt.isPresent()) {
                tessControlSource = tessControlOpt.get().openAsReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }

            var tessEvalOpt = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("iris", "shaders/" + program + ".tes"));
            String tessEvalSource = null;
            if (tessEvalOpt.isPresent()) {
                tessEvalSource = tessEvalOpt.get().openAsReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }
            var fragmentOpt = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("iris", "shaders/" + program + ".fsh"));
            String fragmentSource = null;
            if (fragmentOpt.isPresent()) {
                fragmentSource = fragmentOpt.get().openAsReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }
            ProgramSource source = new ProgramSource(program, vertexSource, geometrySource, tessControlSource, tessEvalSource, fragmentSource, programSet, ((ProgramSetExtension)programSet).getShaderProperties(), null);
            return source;
        } catch (Exception ignored) {

        }
        return null;
    }
}
