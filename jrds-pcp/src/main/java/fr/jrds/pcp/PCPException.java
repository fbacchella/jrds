package fr.jrds.pcp;

import lombok.Getter;

public class PCPException extends Exception {

    @Getter
    private final ERROR error;

    public PCPException(ERROR error) {
        this.error = error;
    }

    @Override
    public String getMessage() {
        if (error.getMessage() != null) {
            return error.getMessage();
        } else {
            return "PCPException";
        }
    }

}
