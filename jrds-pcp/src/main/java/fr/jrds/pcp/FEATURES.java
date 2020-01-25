package fr.jrds.pcp;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum FEATURES {
    SECURE ,
    COMPRESS,
    AUTH,
    CREDS_REQD,
    SECURE_ACK,
    NO_NSS_INIT,
    CONTAINER,
    CERT_REQD,
    BAD_LABEL,
    LABELS;

    public static Set<FEATURES> resolveMask(short mask) {
        Set<FEATURES> found = new HashSet<>(FEATURES.values().length);
        FEATURES[] fa = FEATURES.values();
        for (int i = 0 ; i < fa.length ; i++) {
            if ((mask & 1 << i) != 0) {
                found.add(fa[i]);
            }
        }
        return EnumSet.copyOf(found);
    }

    public static short buildMask(Set<FEATURES> fs) {
        short mask = 0;
        for (FEATURES f: fs) {
            mask += 1 << f.ordinal();
        }
        return mask;
    }
}

