package fr.jrds.pcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;

@Builder @Data
public class ResultData {

    @Getter
    private Instant date;

    @Getter @Singular
    private Map<PmId, List<ResultInstance>> ids;

}
