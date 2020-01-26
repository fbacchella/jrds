package fr.jrds.pcp;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Explained in https://pcp.io/man/man5/LOGARCHIVE.5.html
 * @author Fabrice Bacchella
 *
 */
@Data
public class ResultInstance {

    @Getter
    private final int instance;

    @Getter
    private final Object value;

    @Builder
    private ResultInstance(int instance, Object value) {
        this.instance = instance;
        this.value = value;
    }

    @Builder
    private ResultInstance(ERROR error) {
        value = error;
        this.instance = -1;
    }

    @SuppressWarnings("unchecked")
    public <P> P getCheckedValue() throws PCPException {
        if (value instanceof ERROR) {
            throw new PCPException((ERROR)value);
        }
        return (P) value;
    }

}
