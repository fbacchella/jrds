package jrds.factories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import jrds.factories.xml.JrdsNode;
import jrds.starter.Starter;
import jrds.webapp.DiscoverAgent;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProbeMeta {
    public class EmptyDiscoverAgent extends DiscoverAgent {
        @Override
        public void discover(String hostname, Document hostDom,
                Collection<JrdsNode> probdescs, HttpServletRequest request) {
        }
    };
    
    public class EmptyStarter extends Starter {
    };
    
    Class<? extends DiscoverAgent> discoverAgent() default EmptyDiscoverAgent.class;
    Class<? extends Starter> topStarter() default EmptyStarter.class;

}
