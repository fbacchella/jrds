package jrds.probe;

import org.apache.http.client.methods.HttpRequestBase;

import jrds.starter.Connection;

public abstract class HttpSession extends Connection<HttpSession> {

    @Override
    public HttpSession getConnection() {
        return this;
    }

    public abstract boolean makeSession(HttpRequestBase request);
}
