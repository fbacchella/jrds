package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.ERROR;
import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PCPException;
import fr.jrds.pcp.PmId;
import fr.jrds.pcp.ResultData;
import fr.jrds.pcp.ResultInstance;
import fr.jrds.pcp.VALFMT;
import fr.jrds.pcp.VALUE_TYPE;
import lombok.Getter;

public class Result extends Pdu {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    ResultData rd;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.RESULT;
    }

    @Override
    protected void parse(MessageBuffer buffer) throws PCPException {
        int seconds = buffer.getInt();
        int microsecends = buffer.getInt();
        Instant date = Instant.ofEpochSecond(seconds, microsecends * 1000);
        int count = buffer.getInt();
        Map<PmId, List<ResultInstance>> ids = new HashMap<>(count);
        for (int i=0 ; i < count ; i++) {
            PmId pmid = new PmId(buffer.getInt());
            logger.debug("Values domain id={}", pmid.getId());
           int resultCount = buffer.getInt();
            buffer.mark();
            List<ResultInstance> results;
            if (resultCount < 0 && ERROR.errors.containsKey(resultCount)) {
                results = Collections.singletonList(ResultInstance.builder().error(ERROR.errors.get(resultCount)).build());
            } else if (resultCount > 0) {
                results = new ArrayList<>(resultCount);
                VALFMT valfmt = VALFMT.values()[buffer.getInt()];
                for (int j = 0; j < resultCount; j++) {
                    ResultInstance r = parseValue(buffer, valfmt);
                    results.add(r);
                    logger.debug("  Value instance={}, value=\"{}\"",
                                 r.getInstance(), r.getCheckedValue());
                } 
            } else {
                results = Collections.emptyList();
            }
            ids.put(pmid, Collections.unmodifiableList(results));
        }
        rd = ResultData.builder().ids(Collections.unmodifiableMap(ids)).date(date).build();
    }

    public ResultInstance parseValue(MessageBuffer buffer, VALFMT valfmt) {
        ResultInstance.ResultInstanceBuilder builder = ResultInstance.builder();
        builder.instance(buffer.getInt());
        switch(valfmt) {
        case INSITU:
            builder.value(Integer.toUnsignedLong(buffer.getInt()));
            break;
        case DPTR:
            builder.value(dptr(buffer));
            break;
        case SPTR:
            throw new UnsupportedOperationException();
        }
        return builder.build();
    }

    private Object dptr(MessageBuffer buffer) {
        int instance_offset = buffer.getInt();
        buffer.mark();
        buffer.position(instance_offset * 4);
        byte type_value = buffer.getByte();
        VALUE_TYPE type = VALUE_TYPE.values()[type_value];
        byte[] valueLengthBytes = new byte[4];
        Arrays.fill(valueLengthBytes, (byte)0);
        buffer.getByte(valueLengthBytes, 1, 3);
        int valueLength = ByteBuffer.wrap(valueLengthBytes).getInt();
        byte[] valueBytes = new byte[valueLength - 4];
        buffer.getByte(valueBytes);
        Object value;
        switch (type) {
        case STRING:
            // A zero-terminated string
            value = new String(valueBytes, 0, valueBytes.length -1);
            break;
        case DOUBLE:
            value = ByteBuffer.wrap(valueBytes).getDouble();
            break;
        case FLOAT:
            value = ByteBuffer.wrap(valueBytes).getFloat();
            break;
        case U64:
        case I64:
            value = ByteBuffer.wrap(valueBytes).getLong();
            break;
        case I32:
        case U32:
            value = ByteBuffer.wrap(valueBytes).getInt();
            break;
        default:
            throw new UnsupportedOperationException(type.toString());
        }
        buffer.reset();
        return value;
    }


}
