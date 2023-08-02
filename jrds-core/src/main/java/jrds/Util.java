package jrds;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UnknownFormatConversionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.starter.HostStarter;
import jrds.starter.StarterNode;

/**
 *
 * @author Fabrice Bacchella
 * @version $Revision$, $Date$
 */
public class Util {
    static final private Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * The SI prefix as an enumeration, with factor provided.
     * <p>
     * More informations can be found at
     * <a target="_blank" href="http://en.wikipedia.org/wiki/SI_prefix">
     * Wikipedia's page</a>
     */
    public enum SiPrefix {
        Y(24),
        Z(21),
        E(18),
        P(15),
        T(12),
        G(9),
        M(6),
        k(3),
        h(2),
        da(1),
        FIXED(0),
        d(-1),
        c(-2),
        m(-3),
        µ(-6),
        n(-9),
        p(-12),
        f(-15),
        a(-18),
        z(-21),
        y(-24);

        private final int exponent;

        SiPrefix(int exponent) {
            this.exponent = exponent;
        }

        /**
         * Evaluate a value in the context of this prefix
         * 
         * @param value the value to evalute
         * @param isSi is the prefix metric or binary (power of 2)
         * @return the raw value
         */
        public double evaluate(double value, boolean isSi) {
            return Math.pow(isSi ? 10 : 1024, isSi ? exponent : exponent / 3.0) * value;
        }

        /**
         * @return the exponent for this prefix
         */
        public int getExponent() {
            return exponent;
        }
    }

    static final private ErrorListener el = new ErrorListener() {
        public void error(TransformerException e) {
            logger.error("Invalid xsl: {}",  e.getMessageAndLocation());
        }

        public void fatalError(TransformerException e) {
            logger.error("Invalid xsl: {}", e.getMessageAndLocation());
        }

        public void warning(TransformerException e) {
            logger.warn("Invalid xsl: {}", e.getMessageAndLocation());
        }
    };
    static final TransformerFactory tFactory = TransformerFactory.newInstance();

    static {
        tFactory.setErrorListener(el);
    }

    private static final ThreadLocal<MessageDigest> md5Source = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("You should not see this message, MD5 not available");
            throw new RuntimeException(e);
        }
    });

    public static MessageDigest getmd5() {
        return md5Source.get();
    }

    /**
     * Return the md5 digest value of a string, encoded in base64
     * 
     * @param s The string to use
     * @return the printable md5 digest value for s
     */
    public static String stringSignature(String s) {
        getmd5().reset();
        byte[] digestval = getmd5().digest(s.getBytes());
        getmd5().reset();
        return Base64.getEncoder().encodeToString(digestval);
    }

    public static String cleanPath(String s) {
        String retval = s.replace('\\', '_');
        retval = retval.replace(':', '_');
        retval = retval.replace('/', '_');
        return retval;
    }

    /**
     * Used to normalize the end date to the last update time but only if it's
     * close to it
     * 
     * @param p the probe to check against
     * @param endDate the desired end date
     * @return the normalized end date
     */
    public static Date endDate(Probe<?, ?> p, Date endDate) {
        // Date normalized = endDate;
        // We normalize the last update time, it can't be used directly
        long step = p.getStep();
        Date lastUpdate = p.getLastUpdate();

        // We dont want to graph past the last normalized update time
        // but only if we are within a step interval
        if(Math.abs(endDate.getTime() - lastUpdate.getTime()) <= (step * 1000L))
            return normalize(lastUpdate, step);

        // Else rrd4j will manage the normalization itself
        return endDate;
    }

    /**
     * Normalize to a probe step, as org.rrd4j.core.Util.normalize But use a
     * Date argument and return a Date
     * 
     * @param date A Date to normalize
     * @param step Step in seconds
     * @return "Rounded" Date
     */
    public static Date normalize(Date date, long step) {
        long timestamp = org.rrd4j.core.Util.getTimestamp(date);
        return org.rrd4j.core.Util.getDate(org.rrd4j.core.Util.normalize(timestamp, step));
    }

    private static final Pattern varregexp = Pattern.compile("(.*?)(\\$\\{([\\w\\.-]+)\\}|%)(.*)");
    private static final Pattern oldvarregexp = Pattern.compile("(.*?[^\\$])??\\{(\\d+)\\}(.*)");

    private static final Pattern digit = Pattern.compile("\\d+");
    private static final Pattern attrSignature = Pattern.compile("attr\\.(.*)\\.signature");
    private static final Pattern attr = Pattern.compile("attr\\.(.*)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("%");

    private static String findVariables(String in, int index, Map<String, Integer> indexes, Object... arguments) {
        Matcher m = varregexp.matcher(in);
        if(m.find()) {
            StringBuilder out = new StringBuilder();
            String before = m.group(1);
            String percent = m.group(2);
            String var = m.group(3);
            String after = m.group(4);
            out.append(before);
            String toAppend = "";
            Matcher varMatcher;
            // We just found a lonely %, replace it with %% for latter
            // String.format
            if ("%".equals(percent)) {
                toAppend = "%%";
            }
            // The variable referring to a system variable are directly resolved
            else if (var.startsWith("system.")) {
                toAppend = System.getProperty(var.replace("system.", ""));
                // Will be used as a format string, protect %
                toAppend = PERCENT_PATTERN.matcher(toAppend).replaceAll("%%");
            }
            // Looking for a secret
            else if (var.startsWith("secret.")) {
                SecretStore secrets = null;
                for (Object a : arguments) {
                    if (a instanceof StarterNode) {
                        secrets = ((StarterNode) a).getHostList().getSecrets();
                        break;
                    } else if (a instanceof SecretStore) {
                        secrets = (SecretStore) a;
                    } else if (a instanceof GraphNode) {
                        GraphNode gn = (GraphNode) a;
                        secrets = gn.getProbe().getHostList().getSecrets();
                    }
                }
                if (secrets != null) {
                    byte[] secret = secrets.get(var.replace("secret.", ""));
                    toAppend = new String(secret, StandardCharsets.UTF_8);
                } else {
                    toAppend = var;
                }
            }
            // We found a ${\d+}, directly resolve with the first list argument
            else if(digit.matcher(var).matches()) {
                for(Object o: arguments) {
                    if (o instanceof List) {
                        List<?> l = (List<?>) o;
                        toAppend = l.get(Integer.parseInt(var) - 1).toString();
                        break;
                    }
                }
                // Will be used as a format string, protect %
                toAppend = PERCENT_PATTERN.matcher(toAppend).replaceAll("%%");
            }
            // bean signatures are directly resolved
            else if ((varMatcher = attrSignature.matcher(var)).matches()) {
                String beanName = varMatcher.group(1);
                for(Object o: arguments) {
                    if(o == null) {
                        continue;
                    }
                    // probe manage it's beans
                    if (o instanceof Probe) {
                        GenericBean bean = ((Probe<?, ?>) o).getPd().getBean(beanName);
                        if(bean != null) {
                            Object beanValue = bean.get(o);
                            if (beanValue != null) {
                                toAppend = stringSignature(beanValue.toString());
                            } else {
                                toAppend = m.group(2);
                            }
                            break;
                        }
                    } else {
                        try {
                            PropertyDescriptor bean = new PropertyDescriptor(beanName, o.getClass());
                            Method read = bean.getReadMethod();
                            if(read != null) {
                                Object beanValue = read.invoke(o);
                                if(beanValue != null) {
                                    toAppend = stringSignature(beanValue.toString());
                                } else {
                                    toAppend = m.group(2);
                                }
                                break;
                            }
                        } catch (IntrospectionException e) {
                            // not a bean, skip it
                        } catch (Exception e) {
                            logger.warn("can't output bean {} for {}", beanName, o);
                        }
                    }
                }
                // Will be used as a format string, protect %
                toAppend = PERCENT_PATTERN.matcher(toAppend).replaceAll("%%");
            }
            // beans are directly resolved
            else if((varMatcher = attr.matcher(var)).matches()) {
                String beanName = varMatcher.group(1);
                for (Object o: arguments) {
                    if (o == null) {
                        continue;
                    }
                    // probe manage it's beans
                    if (o instanceof Probe) {
                        GenericBean bean = ((Probe<?, ?>) o).getPd().getBean(beanName);
                        if(bean != null) {
                            Object beanValue = bean.get(o);
                            if(beanValue != null) {
                                toAppend = beanValue.toString();
                            } else {
                                toAppend = m.group(2);
                            }
                            break;
                        }
                    } else {
                        try {
                            PropertyDescriptor bean = new PropertyDescriptor(beanName, o.getClass());
                            Method read = bean.getReadMethod();
                            if(read != null) {
                                Object beanValue = read.invoke(o);
                                if(beanValue != null) {
                                    toAppend = beanValue.toString();
                                } else {
                                    toAppend = m.group(2);
                                }
                                break;
                            }
                        } catch (IntrospectionException e) {
                            // not a bean, skip it
                        } catch (Exception e) {
                            logger.warn("can't output bean {} for {}", beanName, o);
                            logger.warn("Cause", e);
                        }
                    }
                }
                // Will be used as a format string, protect %
                toAppend = PERCENT_PATTERN.matcher(toAppend).replaceAll("%%");
            }
            // Common case, replace the variable with it's index, for
            // MessageFormat
            else {
                if(!indexes.containsKey(var)) {
                    indexes.put(var, index++);
                }
                int slot = indexes.get(var) + 1;
                toAppend = "%" + slot + "$s";
            }
            out.append(toAppend);
            if(after.length() > 0) {
                out.append(findVariables(after, index, indexes, arguments));
            }
            return out.toString();
        }
        return in;
    }

    /**
     * A method to parse a template mixing old elements {x} with new variable
     * ${variable} Should be not be used any more
     * 
     * @param template The template to parse
     * @param keys a array to match indexes elements
     * @param arguments some object to extract value. from
     * @return
     */
    public static String parseOldTemplate(String template, Object[] keys, Object... arguments) {
        // Don't lose time with an empty template
        if(template == null || "".equals(template.trim())) {
            return template;
        }

        Matcher m = oldvarregexp.matcher(template);
        String last = template;
        StringBuilder buffer = new StringBuilder();
        while (m.find()) {
            if(m.group(1) != null)
                buffer.append(m.group(1));
            buffer.append(keys[Integer.parseInt(m.group(2))]);
            last = m.group(3);
            m = oldvarregexp.matcher(last);
        }
        buffer.append(last);
        return jrds.Util.parseTemplate(buffer.toString(), arguments);
    }

    private enum Evaluate {
        index {
            @Override
            String toString(Object o) {
                return convert(o, IndexedProbe.class, IndexedProbe::getIndexName);
            }
        },
        index_signature {
            @Override
            String toString(Object o) {
                return convert(o, IndexedProbe.class, p -> stringSignature(p.getIndexName()));
            }
        },
        index_cleanpath {
            @Override
            String toString(Object o) {
                return convert(o, IndexedProbe.class, p -> cleanPath(p.getIndexName()));
            }
        },
        url {
            @Override
            String toString(Object o) {
                return convert(o, UrlProbe.class, UrlProbe::getUrlAsString);
            }
        },
        url_signature {
            @Override
            String toString(Object o) {
                return convert(o, UrlProbe.class, p -> stringSignature(p.getUrlAsString()));
            }
        },
        port {
            @Override
            String toString(Object o) {
                return convert(o, UrlProbe.class, u -> Integer.toString(u.getPort()));
            }
        },
        host {
            @Override
            String toString(Object o) {
                return convert(o, HostInfo.class, HostInfo::getName);
            }
        },
        dnsname {
            @Override
            String toString(Object o) {
                return convert(o, HostInfo.class, HostInfo::getDnsName);
            }
        },
        probename {
            @Override
            String toString(Object o) {
                return convert(o, Probe.class, Probe::getName);
            }
        },
        label {
            @Override
            String toString(Object o) {
                return convert(o, Probe.class, Probe::getLabel);
            }
        },
        connection_name {
            @Override
            String toString(Object o) {
                return convert(o, ConnectedProbe.class, ConnectedProbe::getConnectionName);
            }
        },
        connection_name_signature {
            @Override
            String toString(Object o) {
                return convert(o, ConnectedProbe.class, p -> stringSignature(p.getConnectionName()));
            }
        },
        probedesc_name {
            @Override
            String toString(Object o) {
                return convert(o, ProbeDesc.class,ProbeDesc::getName);
            }
        },
        graphdesc_title {
            @Override
            String toString(Object o) {
                return convert(o, GraphDesc.class, GraphDesc::getGraphTitle);
            }
        },
        graphdesc_name {
            @Override
            String toString(Object o) {
                return convert(o, GraphDesc.class, GraphDesc::getGraphName);
            }
        };
        abstract String toString(Object o);
        <T> String convert(Object o, Class<T> clazz, Function<T, String> apply) {
            try {
                return Optional.ofNullable(o)
                               .filter(Objects::nonNull)
                               .filter(v -> clazz.isAssignableFrom(v.getClass()))
                               .map(clazz::cast)
                               .map(apply::apply)
                               .orElse(null);
            } catch (RuntimeException e) {
                logger.error("Failed to evalute template for {} on {}, because of {}", clazz.getCanonicalName(), o, resolveThrowableException(e));
                logger.debug("Cause: ", e);
                return null;
            }
        }
    }

    private static void check(Object o, Map<String, Integer> indexes, Object[] values, Evaluate e) {
        String name = e.name().replace('_', '.');
        if (indexes.containsKey(name)) {
            Optional.ofNullable(e.toString(o)).ifPresent(s -> values[indexes.get(name)] = s);
        }
    }

    public static String parseTemplate(String template, Object... arguments) {
        // Don't lose time with an empty template
        if(template == null || "".equals(template.trim())) {
            return template;
        }

        Map<String, Integer> indexes = new HashMap<>();
        String message = findVariables(template, 0, indexes, arguments);
        Object[] values = new Object[indexes.size()];

        for(Object o: arguments) {
            if(o == null)
                continue;
            if(logger.isTraceEnabled())
                logger.trace("Argument for template \"{}\": {}", template, o.getClass());
            if(o instanceof IndexedProbe) {
                check(o, indexes, values, Evaluate.index);
                check(o, indexes, values, Evaluate.index_signature);
                check(o, indexes, values, Evaluate.index_cleanpath);
            }
            if(o instanceof UrlProbe) {
                check(o, indexes, values, Evaluate.url);
                check(o, indexes, values, Evaluate.port);
                check(o, indexes, values, Evaluate.url_signature);
            }
            if(o instanceof ConnectedProbe) {
                check(o, indexes, values, Evaluate.connection_name);
                check(o, indexes, values, Evaluate.connection_name_signature);
            }
            if(o instanceof Probe) {
                Probe<?, ?> p = ((Probe<?, ?>) o);
                HostInfo host = p.getHost();
                check(host, indexes, values, Evaluate.host);
                check(p, indexes, values, Evaluate.probename);
                check(p, indexes, values, Evaluate.label);
            }
            if(o instanceof HostStarter) {
                check(((HostStarter) o).getHost(), indexes, values, Evaluate.host);
            }
            if(o instanceof HostInfo) {
                check(o, indexes, values, Evaluate.host);
                check(o, indexes, values, Evaluate.dnsname);
            }
            if(o instanceof GraphDesc) {
                check(o, indexes, values, Evaluate.graphdesc_name);
                check(o, indexes, values, Evaluate.graphdesc_title);
            }
            if(o instanceof ProbeDesc) {
                check(o, indexes, values, Evaluate.probedesc_name);
            }
            if(o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<? extends String, ?> tempMap = (Map<? extends String, ?>) o;
                for(Map.Entry<String, Integer> e: indexes.entrySet()) {
                    // Check if the given map contains a key to an empty slot in
                    // the values
                    if(tempMap.containsKey(e.getKey()) && values[e.getValue()] == null) {
                        values[e.getValue()] = tempMap.get(e.getKey());
                    }
                }
            }
        }
        try {
            return String.format(message, values);
        } catch (UnknownFormatConversionException e) {
            logger.error("Unable for format {}", message);
            throw e;
        }
    }

    /**
     * <p>
     * A compact and exception free number parser.
     * <p>
     * <p>
     * If the string can be parsed as the specified type, it return the default
     * value
     * <p>
     * 
     * @param toParse The string to parse
     * @param defaultVal A default value to use it the string can't be parsed
     * @return An Number object using the same type than the default value.
     */
    @SuppressWarnings("unchecked")
    public static <NumberClass extends Number> NumberClass parseStringNumber(String toParse, NumberClass defaultVal) {
        if(toParse == null || "".equals(toParse))
            return defaultVal;

        try {
            Class<NumberClass> clazz = (Class<NumberClass>) defaultVal.getClass();
            Constructor<NumberClass> c = clazz.getConstructor(String.class);
            return c.newInstance(toParse);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
        }
        return defaultVal;
    }

    @Deprecated
    public static <NumberClass extends Number> NumberClass parseStringNumber(String toParse, Class<NumberClass> nc, NumberClass defaultVal) {
        if(toParse == null || "".equals(toParse))
            return defaultVal;
        if(!(Number.class.isAssignableFrom(nc))) {
            return defaultVal;
        }

        try {
            Constructor<NumberClass> c = nc.getConstructor(String.class);
            return c.newInstance(toParse);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException| InstantiationException | IllegalAccessException | InvocationTargetException e) {
        }
        return defaultVal;
    }

    public static void serialize(Document d, OutputStream out, URL transformerLocation, Map<String, String> properties) throws TransformerException, IOException {
        Source source = new DOMSource(d);

        Transformer transformer;
        if(transformerLocation != null) {
            Source stylesource = new StreamSource(transformerLocation.toString());
            transformer = tFactory.newTransformer(stylesource);
        } else
            transformer = tFactory.newTransformer();

        String documentEncoding = d.getXmlEncoding();
        if(documentEncoding == null)
            documentEncoding = "UTF-8";
        transformer.setOutputProperty(OutputKeys.ENCODING, documentEncoding);
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");

        DocumentType dt = d.getDoctype();
        // If no transformation, we try to keep the Document type
        if(dt != null && transformerLocation == null) {
            String publicId = dt.getPublicId();
            if(publicId != null)
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
            String systemId = dt.getSystemId();
            if(systemId != null)
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
        }

        if(properties != null) {
            for(Map.Entry<String, String> e: properties.entrySet()) {
                transformer.setOutputProperty(e.getKey(), e.getValue());
            }
        }

        Writer w = new OutputStreamWriter(out, documentEncoding);
        StreamResult result = new StreamResult(w);
        transformer.transform(source, result);
        out.flush();
    }

    public static <T> Iterable<T> iterate(Enumeration<T> en) {
        return () -> new Iterator<>() {
            public boolean hasNext() {
                return en.hasMoreElements();
            }

            public T next() {
                if (hasNext()) {
                    return en.nextElement();
                } else {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Cannot remove in Enumeration wrapper");
            }
        };
    }

    private static final Collator defaultCollator = Collator.getInstance();

    /**
     * Contains an alpha numeric sorter where host2 is before host10
     */
    public static final Comparator<String> nodeComparator = (firstString, secondString) -> {
        if (firstString == null || secondString == null) {
            throw new NullPointerException();
        }

        int result = 0;
 
        int lengthFirstStr = firstString.length();
        int lengthSecondStr = secondString.length();

        int index1 = 0;
        int index2 = 0;

        CharBuffer space1 = CharBuffer.allocate(lengthFirstStr);
        CharBuffer space2 = CharBuffer.allocate(lengthSecondStr);

        while (index1 < lengthFirstStr && index2 < lengthSecondStr) {
            space1.clear();
            space2.clear();

            char ch1 = firstString.charAt(index1);
            boolean isDigit1 = Character.isDigit(ch1);
            char ch2 = secondString.charAt(index2);
            boolean isDigit2 = Character.isDigit(ch2);

            do {
                space1.append(ch1);
                index1++;

                if(index1 < lengthFirstStr) {
                    ch1 = firstString.charAt(index1);
                } else {
                    break;
                }
            } while (Character.isDigit(ch1) == isDigit1);

            do {
                space2.append(ch2);
                index2++;

                if(index2 < lengthSecondStr) {
                    ch2 = secondString.charAt(index2);
                } else {
                    break;
                }
            } while (Character.isDigit(ch2) == isDigit2);

            String str1 = space1.flip().toString();
            String str2 = space2.flip().toString();

            if (isDigit1 && isDigit2) {
                try {
                    long firstNumberToCompare = Long.parseLong(str1);
                    long secondNumberToCompare = Long.parseLong(str2);
                    result = Long.compare(firstNumberToCompare, secondNumberToCompare);
                    if (result == 0) {
                        // 1 == 01 is true with a number, but not with a string, check for a string equality
                        result = defaultCollator.compare(str1, str2);
                    }
                } catch (NumberFormatException e) {
                    // Something prevent the number parsing, do a string
                    // comparison
                    result = defaultCollator.compare(str1, str2);
                }
            } else {
                result = defaultCollator.compare(str1, str2);
            }
            // A difference was found, exit the loop
            if (result != 0) {
                break;
            }
        }
        // one string might be a substring of the other, check that
        if (result == 0) {
            result = lengthFirstStr - lengthSecondStr;
        }
        return result;
    };

    private static class LambdaString {
        private final Supplier<String> source;
        private LambdaString(Supplier<String> source) {
            this.source = source;
        }
        @Override
        public String toString() {
            return source.get();
        }
    }

    /**
     * It tries to extract a meaningful message from any exception
     * @param t
     * @return
     */
    public static String resolveThrowableException(Throwable t) {
        StringBuilder builder = new StringBuilder();
        if (t instanceof UndeclaredThrowableException) {
            t = t.getCause();
        }
        while (t.getCause() != null) {
            String message = t.getMessage();
            if (message == null) {
                message = t.getClass().getSimpleName();
            }
            builder.append(message).append(": ");
            t = t.getCause();
        }
        String message = t.getMessage();
        // Helping resolve bad exception's message
        if (t instanceof NoSuchMethodException) {
            message = "No such method: " + t.getMessage();
        } else if (t instanceof java.lang.NegativeArraySizeException) {
            message = "Negative array size: " + message;
        } else if (t instanceof ArrayIndexOutOfBoundsException) {
            message = "Array out of bounds: " + message;
        } else if (t instanceof ClassNotFoundException) {
            message = "Class not found: " + message;
        } else if (t instanceof IllegalCharsetNameException) {
            message = "Illegal charset name: " + t.getMessage();
        } else if (t instanceof UnsupportedCharsetException) {
            message = "Unsupported charset name: " + t.getMessage();
        } else if (t instanceof ClosedChannelException) {
            message = "Closed channel";
        } else if (t instanceof SSLHandshakeException) {
            // SSLHandshakeException is a chain of the same message, keep the last one
            builder.setLength(0);
        } else if (t instanceof InterruptedException) {
            builder.setLength(0);
            message = "Interrupted";
        } else if (message == null) {
            message = t.getClass().getSimpleName();
        }
        builder.append(message);
        return builder.toString();
    }


    static public void log(Object source, Logger namedLogger, Level l, Throwable e, String format, Object... args) {
        LambdaString ls = new LambdaString(() -> {
            for (int i = 0 ; i < args.length ; i++) {
                if (args[i] instanceof Throwable) {
                    args[i] = resolveThrowableException((Throwable)args[i]);
                }
            }
            StringBuilder line = new StringBuilder();
            if(source != null) {
                line.append("[").append(source).append("] ");
            }
            line.append(String.format(format, args));
            return line.toString();
        });
        Consumer<Object> lg;
        switch(l) {
        case TRACE:
            lg = (i) -> namedLogger.trace("{}", i);
            break;
        case DEBUG:
            lg = (i) -> namedLogger.debug("{}", i);
            break;
        case INFO:
            lg = (i) -> namedLogger.info("{}", i);
            break;
        case WARN:
            lg = (i) -> namedLogger.warn("{}", i);
            break;
        case ERROR:
            lg = (i) -> namedLogger.error("{}", i);
            break;
        default:
            lg = (i) -> {};
        }
        lg.accept(ls);
        // NPE or ArrayIndexOutOfBoundsException should never happen, so it's always logged
        if(e != null && (namedLogger.isDebugEnabled() || e instanceof NullPointerException || e instanceof ArrayIndexOutOfBoundsException)) {
            StackTraceElement[] stack = e.getStackTrace();
            Writer w = new CharArrayWriter(stack.length*20);
            e.printStackTrace(new PrintWriter(w));
            lg.accept("Error stack: ");
            lg.accept(w);
        }
    }

    static public boolean rolesAllowed(Set<String> allowedRoles, Set<String> userRoles) {
        return !Collections.disjoint(allowedRoles, userRoles);
    }

    private static final class Formater {
        private final Supplier<Object> source;

        private Formater(Supplier<Object> source) {
            this.source = source;
        }

        @Override
        public final String toString() {
            return source.get().toString();
        }
    }

    public static Object delayedFormatString(Supplier<Object> source) {
        return new Formater(source);
    }

}
