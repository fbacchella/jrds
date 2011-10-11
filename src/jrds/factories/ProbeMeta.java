package jrds.factories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.starter.Starter;
import jrds.webapp.DiscoverAgent;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProbeMeta {
    public class EmptyDiscoverAgent extends DiscoverAgent {
        public EmptyDiscoverAgent() {
            super("Empty");
        }

        @Override
        public void discover(String hostname, JrdsElement hostElement,
                Map<String,JrdsDocument> probdescs, HttpServletRequest request) {
        }

        @Override
        public List<FieldInfo> getFields() {
            return Collections.emptyList();
        }

        /**
         * This discover agent does nothing
         *
         */
        @Override
        public void doHtmlDiscoverFields(JrdsDocument document) {
        }
    };

    /**
     * @author bacchell
     *
     */
    public class EmptyStarter extends Starter {
    };

    Class<? extends DiscoverAgent> discoverAgent() default EmptyDiscoverAgent.class;
    Class<? extends Starter> topStarter() default EmptyStarter.class;

}
