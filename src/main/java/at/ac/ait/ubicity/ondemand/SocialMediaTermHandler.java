
package at.ac.ait.ubicity.ondemand;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;
/**
 *
 * @author jan van oort
 */
public class SocialMediaTermHandler implements RestHandler {

    
    
    @Inject
    public SocialMediaTermHandler( RestController restController )  {
        restController.registerHandler( GET, "/_jitindex", this);
    }
    
    
    public void handleRequest(RestRequest rr, RestChannel rc) {
        System.out.println( rr.param( "q" ) );
    }
    
}
