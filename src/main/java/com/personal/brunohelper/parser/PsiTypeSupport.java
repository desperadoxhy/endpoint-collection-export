package com.personal.brunohelper.parser;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class PsiTypeSupport {

    private static final Set<String> SIMPLE_TYPES = Set.of(
            CommonClassNames.JAVA_LANG_STRING,
            CommonClassNames.JAVA_LANG_BOOLEAN,
            CommonClassNames.JAVA_LANG_BYTE,
            CommonClassNames.JAVA_LANG_SHORT,
            CommonClassNames.JAVA_LANG_INTEGER,
            CommonClassNames.JAVA_LANG_LONG,
            CommonClassNames.JAVA_LANG_FLOAT,
            CommonClassNames.JAVA_LANG_DOUBLE,
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.OffsetDateTime",
            "java.time.ZonedDateTime",
            "java.util.Date",
            "java.util.UUID",
            "java.net.URI",
            "java.net.URL"
    );

    private static final Set<String> IGNORED_PARAMETER_TYPES = Set.of(
            "org.springframework.validation.BindingResult",
            "org.springframework.validation.Errors",
            "javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpServletRequest",
            "jakarta.servlet.http.HttpServletResponse",
            "javax.servlet.ServletRequest",
            "javax.servlet.ServletResponse",
            "jakarta.servlet.ServletRequest",
            "jakarta.servlet.ServletResponse",
            "org.springframework.ui.Model",
            "org.springframework.ui.ModelMap",
            "org.springframework.web.servlet.mvc.support.RedirectAttributes",
            "java.security.Principal",
            "java.util.Locale"
    );

    private PsiTypeSupport() {
    }

    public static boolean isSimpleType(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            return true;
        }
        if (type instanceof PsiArrayType arrayType) {
            return isByteArray(arrayType);
        }
        String canonicalText = type.getCanonicalText();
        return SIMPLE_TYPES.contains(canonicalText);
    }

    public static boolean isCollectionType(PsiType type) {
        return type instanceof PsiClassType classType
                && InheritanceUtil.isInheritor(classType.resolve(), CommonClassNames.JAVA_UTIL_COLLECTION);
    }

    public static boolean isMapType(PsiType type) {
        return type instanceof PsiClassType classType
                && InheritanceUtil.isInheritor(classType.resolve(), CommonClassNames.JAVA_UTIL_MAP);
    }

    public static boolean isMultipartType(PsiType type) {
        if (type instanceof PsiArrayType arrayType) {
            return isMultipartType(arrayType.getComponentType());
        }
        return "org.springframework.web.multipart.MultipartFile".equals(type.getCanonicalText());
    }

    public static boolean isBinaryResponseType(PsiType type) {
        if (type instanceof PsiArrayType arrayType) {
            return isByteArray(arrayType);
        }
        String canonicalText = type.getCanonicalText();
        return "org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody".equals(canonicalText)
                || "java.io.InputStream".equals(canonicalText)
                || "java.io.File".equals(canonicalText);
    }

    public static boolean shouldIgnoreParameter(PsiType type) {
        String canonicalText = type.getCanonicalText();
        return IGNORED_PARAMETER_TYPES.contains(canonicalText);
    }

    public static @Nullable String getQualifiedName(PsiType type) {
        if (type instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            if (psiClass != null) {
                return psiClass.getQualifiedName();
            }
        }
        return null;
    }

    private static boolean isByteArray(PsiArrayType type) {
        return PsiType.BYTE.equals(type.getComponentType());
    }
}
