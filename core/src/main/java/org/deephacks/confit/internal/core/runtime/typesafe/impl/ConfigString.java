/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package org.deephacks.confit.internal.core.runtime.typesafe.impl;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.deephacks.confit.internal.core.runtime.typesafe.ConfigOrigin;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigRenderOptions;
import org.deephacks.confit.internal.core.runtime.typesafe.ConfigValueType;

final class ConfigString extends AbstractConfigValue implements Serializable {

    private static final long serialVersionUID = 2L;

    final private String value;

    ConfigString(ConfigOrigin origin, String value) {
        super(origin);
        this.value = value;
    }

    @Override
    public ConfigValueType valueType() {
        return ConfigValueType.STRING;
    }

    @Override
    public String unwrapped() {
        return value;
    }

    @Override
    String transformToString() {
        return value;
    }

    @Override
    protected void render(StringBuilder sb, int indent, ConfigRenderOptions options) {
        String rendered;
        if (options.getJson())
            rendered = ConfigImplUtil.renderJsonString(value);
        else
            rendered = ConfigImplUtil.renderStringUnquotedIfPossible(value);
        sb.append(rendered);
    }

    @Override
    protected ConfigString newCopy(ConfigOrigin origin) {
        return new ConfigString(origin, value);
    }

    // serialization list goes through SerializedConfigValue
    private Object writeReplace() throws ObjectStreamException {
        return new SerializedConfigValue(this);
    }
}
