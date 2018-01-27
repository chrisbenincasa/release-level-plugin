package com.chrisbenincasa.release_level.model;

import java.lang.annotation.Annotation;

public enum  ReleaseStage {
    PREALPHA("Pre-alpha", PreAlpha.class),
    ALPHA("Alpha", Alpha.class),
    BETA("Beta", Beta.class),
    GAMMA("Gamma", Gamma.class);

    private final String name;
    private final Class<? extends Annotation> annoClazz;

    ReleaseStage(String name, Class<? extends Annotation> annoClazz) {
        this.name = name;
        this.annoClazz = annoClazz;
    }

    public String getName() {
        return name;
    }

    public Class<? extends Annotation> getAnnoClazz() {
        return annoClazz;
    }

    public static ReleaseStage fromName(final String n) {
        String normalized = normalizeEnumName(n);
        for (ReleaseStage s : ReleaseStage.values()) {
            if (normalizeEnumName(s.name).equalsIgnoreCase(normalized)) {
                return s;
            }
        }

        throw new IllegalArgumentException("No stage found for name = " + n);
    }

    private static String normalizeEnumName(final String n) {
        return n.replaceAll("-", "").toLowerCase();
    }
}
