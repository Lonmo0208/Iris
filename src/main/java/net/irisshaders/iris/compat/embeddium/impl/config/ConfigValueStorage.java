package net.irisshaders.iris.compat.embeddium.impl.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;

public class ConfigValueStorage<T> implements OptionStorage<ModConfigSpec.ConfigValue<T>> {

    private final ModConfigSpec.ConfigValue<T> value;

    public ConfigValueStorage(ModConfigSpec.ConfigValue<T> value) {
        this.value = value;
    }

    @Override
    public ModConfigSpec.ConfigValue<T> getData() {
        return value;
    }

    @Override
    public void save() {
        value.save();
    }
}
