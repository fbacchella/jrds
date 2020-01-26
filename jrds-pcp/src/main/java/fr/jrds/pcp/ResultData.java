package fr.jrds.pcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import fr.jrds.pcp.pdu.Result;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder @Data
public class ResultData {

    @Getter
    private final Instant date;

    @Getter
    private final Map<PmId, List<ResultInstance>> ids;
    
    public static ResultData of(Result r) {
        return ResultData.builder().date(r.getDate()).ids(r.getIds()).build();
    }

}
