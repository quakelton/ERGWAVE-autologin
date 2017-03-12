package hk.idv.elton.cuwaveautologin;

// Source: http://www.yeory.com/126

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;

public class CustomRedirectHandler extends DefaultRedirectHandler{

    private static final String REDIRECT_LOCATIONS = "http.protocol.redirect-locations";

    public CustomRedirectHandler() {
        super();
    }

    public boolean isRedirectRequested(
            final HttpResponse response,
            final HttpContext context) {
        if (response == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
                return true;
            default:
                return false;
        } //end of switch
    }

    public URI getLocationURI(final HttpResponse response, final HttpContext context) {
        URI uri = null;
        try{
            if (response == null) {
                throw new IllegalArgumentException("HTTP response may not be null");
            }
            //get the location header to find out where to redirect to
            Header locationHeader = response.getFirstHeader("location");
            if (locationHeader == null) {
                // got a redirect response, but no location header
                throw new ProtocolException("Received redirect response " + response.getStatusLine()+ " but no location header");
            }
            //HERE IS THE MODIFIED LINE OF CODE
            String location = locationHeader.getValue().replaceAll (" ", "%20");

            try {
                uri = new URI(location);
            } catch (URISyntaxException ex) {
                throw new ProtocolException("Invalid redirect URI: " + location);
            }

            HttpParams params = response.getParams();
            // rfc2616 demands the location value be a complete URI
            // Location       = "Location" ":" absoluteURI
            if (!uri.isAbsolute()) {
                if (params.isParameterTrue(ClientPNames.REJECT_RELATIVE_REDIRECT)) {
                    throw new ProtocolException("Relative redirect location '"+ uri + "' not allowed");
                }
                // Adjust location URI
                HttpHost target = (HttpHost) context.getAttribute(
                        ExecutionContext.HTTP_TARGET_HOST);
                if (target == null) {
                    throw new IllegalStateException("Target host not available " + "in the HTTP context");
                }

                HttpRequest request = (HttpRequest) context.getAttribute(
                        ExecutionContext.HTTP_REQUEST);

                try {
                    URI requestURI = new URI(request.getRequestLine().getUri());
                    URI absoluteRequestURI = URIUtils.rewriteURI(requestURI, target, true);
                    uri = URIUtils.resolve(absoluteRequestURI, uri);
                } catch (URISyntaxException ex) {
                    throw new ProtocolException();
                }
            }

            if (params.isParameterFalse(ClientPNames.ALLOW_CIRCULAR_REDIRECTS)) {

                RedirectLocations redirectLocations = (RedirectLocations) context.getAttribute(
                        REDIRECT_LOCATIONS);

                if (redirectLocations == null) {
                    redirectLocations = new RedirectLocations();
                    context.setAttribute(REDIRECT_LOCATIONS, redirectLocations);
                }

                URI redirectURI;
                if (uri.getFragment() != null) {
                    try {
                        HttpHost target = new HttpHost(
                                uri.getHost(),
                                uri.getPort(),
                                uri.getScheme());
                        redirectURI = URIUtils.rewriteURI(uri, target, true);
                    } catch (URISyntaxException ex) {
                        throw new ProtocolException(ex.getMessage());
                    }
                } else {
                    redirectURI = uri;
                }

                if (redirectLocations.contains(redirectURI)) {
                    throw new CircularRedirectException("Circular redirect to '" +redirectURI + "'");
                } else {
                    redirectLocations.add(redirectURI);
                }
            }
        }catch(ProtocolException ex){
            ex.printStackTrace();
        } catch (CircularRedirectException e) {
            e.printStackTrace();
        }
        return uri;
    }
}