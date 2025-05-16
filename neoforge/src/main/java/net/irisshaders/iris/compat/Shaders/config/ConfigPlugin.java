package net.irisshaders.iris.compat.Shaders.config;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ConfigPlugin implements IMixinConfigPlugin {

    private static final String MIXINCONFIG = Type.getDescriptor(Config.class);

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try(InputStream stream = this.getClass().getClassLoader().getResourceAsStream(mixinClassName.replace('.', '/') + ".class")) {
            ClassReader reader = new ClassReader(stream);
            ClassNode node = new ClassNode();
            reader.accept(node,  ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            if (node.invisibleAnnotations == null) {
                return true;
            }

            Properties config = net.irisshaders.iris.platform.IrisForgeMod.loadConfig();
            for(AnnotationNode annotation : node.invisibleAnnotations) {
                if (annotation.desc.equals(MIXINCONFIG)) {
                    for(int i = 0; i < annotation.values.size(); i += 2) {
                        if(annotation.values.get(i).equals("value")) {
                            String modId = (String) annotation.values.get(i + 1);
                            if(modId != null) {
                                if (net.irisshaders.iris.platform.IrisForgeMod.FALSE.equals(config.getOrDefault(modId, net.irisshaders.iris.platform.IrisForgeMod.FALSE))) {
                                    return false;
                                }
                            }
                            break;
                        }
                    }
                }
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
