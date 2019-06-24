package jrds.configuration;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Filter;
import jrds.FilterXml;
import jrds.Util;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class FilterBuilder extends ConfigObjectBuilder<Filter> {

    static final private Logger logger = LoggerFactory.getLogger(FilterBuilder.class);

    public FilterBuilder() {
        super(ConfigType.FILTER);
    }

    @Override
    Filter build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeFilter(n);
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new InvocationTargetException(e, FilterBuilder.class.getName());
        }
    }

    public Filter makeFilter(JrdsDocument n) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        JrdsElement root = n.getRootElement();
        JrdsElement name = root.getElementbyName("name");
        if(name == null)
            return null;
        FilterXml f = new FilterXml(name.getTextContent());
        setMethod(root.getChildElementsByName("path"), f, "addPath", String.class);
        setMethod(root.getChildElementsByName("tag"), f, "addTag", String.class);
        setMethod(root.getChildElementsByName("qualifiedname"), f, "addGraph", String.class);
        doACL(f, n, root);
        logger.trace("Filter loaded: {}", Util.delayedFormatString(f::getName));
        return f;
    }

}
