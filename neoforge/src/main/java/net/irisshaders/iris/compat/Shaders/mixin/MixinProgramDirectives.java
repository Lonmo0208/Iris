package net.irisshaders.iris.compat.Shaders.mixin;

import net.irisshaders.iris.compat.Shaders.impl.ProgramDirectivesAccessor;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ProgramDirectives.class)
public class MixinProgramDirectives implements ProgramDirectivesAccessor {

    @Final
    @Mutable
    @Shadow
    private int[] drawBuffers;

    @Unique
    public void setDrawBuffers(int[] drawBuffers) {
        this.drawBuffers = drawBuffers;
    }
}
