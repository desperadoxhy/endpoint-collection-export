package com.personal.brunohelper.model;

import com.intellij.psi.PsiType;

public final class EndpointParameterModel {

    private final String name;
    private final String description;
    private final boolean required;
    private final String defaultValue;
    private final ParameterSource source;
    private final PsiType type;

    public EndpointParameterModel(
            String name,
            String description,
            boolean required,
            String defaultValue,
            ParameterSource source,
            PsiType type
    ) {
        this.name = name;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
        this.source = source;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public ParameterSource getSource() {
        return source;
    }

    public PsiType getType() {
        return type;
    }
}
