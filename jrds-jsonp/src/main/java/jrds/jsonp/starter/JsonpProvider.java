package jrds.jsonp.starter;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;

import jrds.starter.Starter;

public class JsonpProvider extends Starter {

    private final Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).mappingProvider(new JsonOrgMappingProvider()).jsonProvider(new JsonOrgJsonProvider()).build();

    /**
     * @return the conf
     */
    public Configuration getConfiguration() {
        return conf;
    }

}
