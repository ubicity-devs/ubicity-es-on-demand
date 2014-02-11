
package at.ac.ait.ubicity.ondemand;


import java.util.Collection;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
/**
 *
 * @author jan van oort
 */
public class JitIndexingPlugin extends AbstractPlugin {

    
    final private String name = "JitIndexingPlugin";
    
    final private String description = "An elasticsearch plugin that fetches social media content on demand, in cooperation with ubicity";
    
    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    
    
    public Collection< Class< ? extends Module > > modules()     {
        Collection< Class< ? extends Module > > modules = Lists.newArrayList();
        modules.add( UbicityJitIndexingModule.class ) ;
        return modules;
    }
    
}
