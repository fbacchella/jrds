package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
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
import fr.jrds.pcp.ResultInstance;
import fr.jrds.pcp.VALFMT;
import lombok.Getter;

public class Result extends Pdu {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    private Instant date;

    @Getter
    private Map<PmId, List<ResultInstance>> ids = Collections.emptyMap();

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.RESULT;
    }

    @Override
    protected void parse(MessageBuffer buffer) throws PCPException {
        int seconds = buffer.getInt();
        int microsecends = buffer.getInt();
        date = Instant.ofEpochSecond(seconds, microsecends * 1000);
        int count = buffer.getInt();
        ids = new HashMap<>(count);
        for (int i=0 ; i < count ; i++) {
            PmId pmid = new PmId(buffer.getInt());
            logger.debug("Values domain id={}", pmid.getId());
           int resultCount = buffer.getInt();
            buffer.mark();
            List<ResultInstance> results;
            if (resultCount < 0 && ERROR.errors.containsKey(resultCount)) {
                results = Collections.singletonList(new ResultInstance(ERROR.errors.get(resultCount)));
            } else if (resultCount > 0) {
                results = new ArrayList<>(resultCount);
                VALFMT valfmt = VALFMT.values()[buffer.getInt()];
                for (int j = 0; j < resultCount; j++) {
                    ResultInstance r = new ResultInstance();
                    r.parse(buffer, valfmt);
                    results.add(r);
                    logger.debug("  Value instance={}, value=\"{}\"",
                                 r.getInstance(), r.getCheckedValue());
                } 
            } else {
                results = Collections.emptyList();
            }
            ids.put(pmid, Collections.unmodifiableList(results));
        }
        ids = Collections.unmodifiableMap(ids);
    }

}
