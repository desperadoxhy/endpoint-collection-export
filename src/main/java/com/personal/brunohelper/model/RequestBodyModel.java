package com.personal.brunohelper.model;

import com.intellij.psi.PsiType;

public final class RequestBodyModel {

    private final PsiType type;
    private final boolean required;
    private final String description;
    private final String contentType;

    public RequestBodyModel(PsiType type, boolean required, String description, String contentType) {
        this.type = type;
        this.required = required;
        this.description = description;
        this.contentType = contentType;
    }

    public PsiType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getContentType() {
        return contentType;
    }
}
