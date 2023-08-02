package fr.jrds.pcp.pdu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.jrds.pcp.MessageBuffer;
import lombok.Getter;
import lombok.Setter;

public class PnmsNames extends Pdu {

    private final List<String> names = new ArrayList<>();

    @Getter @Setter
    private int status;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.PMNS_NAMES;
    }

    @Override
    public int bufferSize() {
        return 24 + names.stream()
        .mapToInt( i -> i.length() + 5 +3)
        .sum();
    }
    @Override
    protected void fill(MessageBuffer buffer) {
        int size = names.stream()
                        .mapToInt( i -> i.length() + 1)
                        .sum();
        buffer.putInt(size);
        buffer.putInt(status);
        buffer.putInt(names.size());
        names.forEach(buffer::putString);
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        names.clear();
        // The size is useless given the way it's evaluated
        buffer.getInt();
        status = buffer.getInt();
        int count = buffer.getInt();
        for (int i = 0; i < count; i++) {
            names.add(buffer.getString());
        }
    }

    public void add(String name) {
        names.add(name);
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

}
