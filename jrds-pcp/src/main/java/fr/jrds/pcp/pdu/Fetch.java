package fr.jrds.pcp.pdu;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PmId;
import lombok.Setter;

public class Fetch extends Pdu {

    @Setter
    private int contextNumber;

    @Setter
    private Instant timeValue = Instant.EPOCH;

    private final List<PmId> ids = new ArrayList<>();

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.FETCH;
    }

    @Override
    public int bufferSize() {
        return 7*4 + ids.size() * 4;
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(contextNumber);
        buffer.putInt((int)timeValue.getEpochSecond());
        buffer.putInt(timeValue.get(ChronoField.MICRO_OF_SECOND));
        buffer.putInt(ids.size());
        ids.forEach(i -> buffer.putInt(i.getId()));
    }

    public void addPmId(PmId id) {
        assert id != null;
        ids.add(id);
    }

}
