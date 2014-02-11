

package at.ac.ait.ubicity.ondemand;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Binder;
import org.elasticsearch.common.inject.Module;

/**
 *
 * @author jan van oort
 */
public class UbicityJitIndexingModule extends AbstractModule {

    
    
    
    
    
    @Override
    protected void configure() {
        bind(  SocialMediaTermHandler.class ).asEagerSingleton();
    }

    
    
    
}
