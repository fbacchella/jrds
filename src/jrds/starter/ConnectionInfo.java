package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import jrds.Util;
import jrds.factories.ArgFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ConnectionInfo {
    static final private Logger logger = Logger.getLogger(ConnectionInfo.class);

    private final List<Object> args;
    private final Map<String, String> beansValue;
    private final String name;
    private final Class<? extends Connection<?>> type;

    public ConnectionInfo(Class<? extends Connection<?>> type, String name, List<Object> args, Map<String, String> beansValue) {
        super();
        this.args = args;
        this.beansValue = beansValue;
        this.name = name;
        this.type = type;
    }

    public void register(StarterNode node) throws InvocationTargetException {
        try {
            Class<?>[] constArgsType = new Class[args.size()];
            Object[] constArgsVal = new Object[args.size()];
            int index = 0;
            for (Object arg: args) {
                constArgsType[index] = arg.getClass();
                constArgsVal[index] = arg;
                index++;
            }
            Connection<?> cnx = type.getConstructor(constArgsType).newInstance(constArgsVal);
            for(Map.Entry<String, String> e: beansValue.entrySet()) {
                String textValue = Util.parseTemplate(e.getValue(), cnx);
                ArgFactory.beanSetter(cnx, e.getKey(), textValue);
                cnx.log(Level.TRACE, "Setting bean '%s' to value '%s' for %s", e.getKey(), textValue, node);
            }
            if(name != null && ! name.trim().isEmpty())
                cnx.setName(name.trim());
            node.registerStarter(cnx);
            logger.debug(Util.delayedFormatString("Connexion registred: %s for %s", cnx, node));
        }
        catch (Exception ex) {
            throw new InvocationTargetException(ex, "Error during connection creation of type " + type.getName() + " for " + node);
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

}
