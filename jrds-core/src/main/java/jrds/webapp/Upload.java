package jrds.webapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jrds.factories.xml.EntityResolver;

@ServletSecurity
public class Upload extends JrdsServlet {
    static final private Logger logger = LoggerFactory.getLogger(Upload.class);

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        FileItemFactory<DiskFileItem> factory = DiskFileItemFactory.builder().get();
        JakartaServletFileUpload<DiskFileItem, FileItemFactory<DiskFileItem>> upload = new JakartaServletFileUpload<>(factory);

        try {
            DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
            instance.setIgnoringComments(true);
            instance.setValidating(true);
            instance.setFeature("http://xml.org/sax/features/external-general-entities", false);
            instance.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            instance.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder dbuilder = instance.newDocumentBuilder();
            dbuilder.setEntityResolver(new EntityResolver());
            dbuilder.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void warning(SAXParseException exception) {
                    logger.warn(exception.getMessage());
                }
            });

            List<DiskFileItem> items = upload.parseRequest(request);

            response.setContentType("text/html");

            PrintWriter outputWriter = response.getWriter();
            outputWriter.println("<textarea>");

            JSONWriter w = new JSONWriter(outputWriter);
            w.array();

            for (FileItem<DiskFileItem> item: items) {
                logger.debug("Item send: {}", item);

                // Process a file upload
                if (!item.isFormField()) {
                    w.object();
                    String fileName = item.getName();
                    w.key("name").value(fileName);
                    try (InputStream uploadedStream = item.getInputStream()) {
                        dbuilder.parse(uploadedStream);
                        File destination = new File(getPropertiesManager().configdir, fileName);
                        if(!destination.exists()) {
                            item.write(destination.toPath());
                            w.key("parsed").value(true);
                        } else {
                            w.key("error").value("file existe");
                            w.key("parsed").value(false);
                        }
                    } catch (Exception e) {
                        w.key("error").value(e.getMessage());
                        w.key("parsed").value(false);
                        logger.error("upload file failed: " + e, e);
                    }
                    w.endObject();
                }
            }
            w.endArray();
            outputWriter.println("</textarea>");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("upload file failed: " + e, e);
        }
        response.flushBuffer();
    }

}
