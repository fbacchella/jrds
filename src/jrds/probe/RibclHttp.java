package jrds.probe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import jrds.factories.ProbeMeta;
import jrds.starter.XmlProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@ProbeMeta(
        timerStarter = jrds.probe.HttpClientStarter.class
        )
public class RibclHttp extends Ribcl {

    @Override
    public Map<String, Number> getNewSampleValues() {

        if(! isCollectRunning())
            return Collections.emptyMap();

        XmlProvider xmlstarter = find(XmlProvider.class);
        if(xmlstarter == null) {
            log(Level.ERROR, "XML Provider not found");
            return Collections.emptyMap();
        }

        HttpClientStarter httpstarter = find(HttpClientStarter.class);
        HttpClient cnx = httpstarter.getHttpClient();

        //Prepare the POST request
        HttpPost query = new HttpPost(String.format("https://%s:%d/ribcl", getIloHost(), getPort()));
        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        buildQuery(bufferStream, xmlstarter);            
        log(Level.DEBUG, "sending to '%s':\n %s", query.getURI(), bufferStream);
        ByteArrayEntity xmlcmd = new ByteArrayEntity(bufferStream.toByteArray());
        query.setEntity(xmlcmd);

        try {
            HttpResponse response = cnx.execute(query);
            HttpEntity entity = response.getEntity();
            bufferStream.reset();
            entity.writeTo(bufferStream);
            log(Level.DEBUG, "http response was\n%s\n%s", response.getStatusLine(), bufferStream);
            if(response.getStatusLine().getStatusCode() != 200) {
                log(Level.ERROR, "Request error: %d %s", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()); 
            }
        } catch (ClientProtocolException e1) {
            log(Level.ERROR, e1, "HTTP protocol failed: %s", e1.getMessage()); 
            return Collections.emptyMap();
        } catch (IOException e1) {
            log(Level.ERROR, e1, "Socket communication failed: %s", e1.getMessage()); 
            return Collections.emptyMap();
        }

        return parseRibcl(bufferStream.toString(), xmlstarter);
    }

    @Override
    protected Document makeDocument(XmlProvider xmlstarter) {
        Document locfgQ = super.makeDocument(xmlstarter);
        Document ribclQ = xmlstarter.getDocument();
        Node ribclElem = locfgQ.getDocumentElement().getFirstChild();
        ribclQ.adoptNode(ribclElem);
        ribclQ.appendChild(ribclElem);        
        return ribclQ;
    }

}
