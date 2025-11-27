package com.example;

import com.fasterxml.jackson.annotation.*;

@SuppressWarnings("serial")
public abstract class MapperConfigBase<CFG extends ConfigFeature, T> extends MapperConfig<T> {

    public final ConfigOverride getConfigOverride(Class<?> type) {
        throw new java.lang.Error();
    }

    public final JsonInclude.Value getDefaultPropertyInclusion() {
        throw new java.lang.Error();
    }

    public final JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType) {
        JsonInclude.Value v = getConfigOverride(baseType).getInclude();
        JsonInclude.Value def = getDefaultPropertyInclusion();
        if (def == null) {
            return v;
        }
        return def.withOverrides(v);
    }
}
