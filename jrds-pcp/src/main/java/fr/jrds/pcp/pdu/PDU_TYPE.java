package fr.jrds.pcp.pdu;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum PDU_TYPE {
    START(0x7000),
    ERROR(0x7000),
    RESULT(0x7001),
    PROFILE(0x7002),
    FETCH(0x7003),
    DESC_REQ(0x7004),
    DESC(0x7005),
    INSTANCE_REQ(0x7006),
    INSTANCE(0x7007),
    TEXT_REQ(0x7008),
    TEXT(0x7009),
    CONTROL_REQ(0x700a),
    CREDS(0x700c),
    PNMS_IDS(0x700d),
    PMNS_NAMES(0x700e),
    PMNS_CHILD(0x700f),
    PMNS_TRAVERSE(0x7010),
    ATTR(0x7011),
    AUTH(0x7011),
    LABEL_REQ(0x7012),
    LABEL(0x7013),
    FINISH(0x7013),
    ;

    public final int code;
    PDU_TYPE(int code) {
        this.code = code;
    }

    static public final Map<Integer, PDU_TYPE> types;
    static {
        Map<Integer, PDU_TYPE> _types = new HashMap<>(PDU_TYPE.values().length);
        Arrays.stream(PDU_TYPE.values()).filter(t -> t != PDU_TYPE.ERROR).forEach(t -> _types.put(t.code, t));
        types = Collections.unmodifiableMap(_types);
    }

    @SuppressWarnings("unchecked")
    public static <P extends Pdu> P Resolve(ByteBuffer buffer) throws IOException {
        buffer.mark();
        int type = buffer.getInt(4);
        if (! types.containsKey(type)) {
            throw new IOException("Corrupted PDU, type not identified");
        }
        P newPdu = null;
        switch(types.get(type)) {
        case START: {
            int status = buffer.getInt(12);
            if (status == 0) {
                newPdu = (P) new Start();
            } else {
                newPdu = (P) new PdpError();
            }
            break;
        }
        case RESULT:
            newPdu = (P) new Result();
            break;
        case PROFILE:
            newPdu = (P) new Profile();
            break;
        case FETCH:
            newPdu = (P) new Fetch();
            break;
        case INSTANCE:
            newPdu = (P) new Instance();
            break;
        case INSTANCE_REQ:
            newPdu = (P) new InstanceReq();
            break;
        case PNMS_IDS:
            newPdu = (P) new PnmsIds();
            break;
        case PMNS_TRAVERSE:
            newPdu = (P) new PnmsTraverse();
            break;
        case PMNS_NAMES:
            newPdu = (P) new PnmsNames();
            break;
        case DESC:
            newPdu = (P) new Desc();
            break;
        default:
            throw new UnsupportedOperationException("Unmannaged PDU");
        }
        buffer.reset();
        return newPdu;
    }

}
