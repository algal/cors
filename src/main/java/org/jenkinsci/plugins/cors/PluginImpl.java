package org.jenkinsci.plugins.cors;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import hudson.Plugin;
import hudson.Extension;
import hudson.model.Hudson;
import hudson.util.PluginServletFilter;
import hudson.model.Descriptor.FormException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import jenkins.model.Jenkins;

/**
 * Entry point of the plugin.
 * 
 * <p>
 * There must be one {@link Plugin} class in each plugin. See javadoc of
 * {@link Plugin} for more about what can be done on this class.
 * 
 * @author Alexis Gallagher
 */
public class PluginImpl
    extends Plugin
{
    // instance variables
    private final static Logger LOG = Logger.getLogger("org.jenkinsci.plugins.cors.PluginImpl");

    // defaults
    private static final String  DEFAULT_ALLOWED_ORIGINS   = "*";
    private static final String  DEFAULT_ALLOWED_METHODS   = "GET,POST,HEAD";
    private static final String  DEFAULT_ALLOWED_HEADERS   = "X-Requested-With,Content-Type,Accept,Origin,Authorization";
    private static final String  DEFAULT_PREFLIGHT_MAX_AGE = "1800";
    private static final boolean DEFAULT_ALLOW_CREDENTIALS = true;
    private static final String  DEFAULT_EXPOSED_HEADERS   = "";
    private static final boolean DEFAULT_CHAIN_PREFLIGHT   = false;

    // config fields, which should be serialized
    private String  allowedOrigins;
    private String  allowedMethods;
    private String  allowedHeaders;
    private String  preflightMaxAge;
    private boolean allowCredentials;
    private String  exposedHeaders;
    private boolean chainPreflight;

    // internal state, which should not be serialized
    @XStreamOmitField
    private ServletContext context;
    @XStreamOmitField
    private CrossOriginFilter filter;

    public PluginImpl()
    {
        this(DEFAULT_ALLOWED_ORIGINS,
             DEFAULT_ALLOWED_METHODS,
             DEFAULT_ALLOWED_HEADERS,
             DEFAULT_PREFLIGHT_MAX_AGE,
             DEFAULT_ALLOW_CREDENTIALS,
             DEFAULT_EXPOSED_HEADERS,
             DEFAULT_CHAIN_PREFLIGHT);
        LOG.finer("PluginImpl.PluginImpl() just finished calling PluginImpl.PluginImpl(args)");
    }

    @DataBoundConstructor
    public PluginImpl(final String allowedOrigins,
                      final String allowedMethods,
                      final String allowedHeaders,
                      final String preflightMaxAge,
                      final boolean allowCredentials,
                      final String exposedHeaders,
                      final boolean chainPreflight)
    {
        super();
        LOG.entering("PluginImpl","PluginImpl(:String,:String,:String,:String,:boolean,:String,:boolean)");
        this.allowedOrigins   = allowedOrigins;
        this.allowedMethods   = allowedMethods;
        this.allowedHeaders   = allowedHeaders;
        this.preflightMaxAge  = preflightMaxAge;
        this.allowCredentials = allowCredentials;
        this.exposedHeaders   = exposedHeaders;
        this.chainPreflight   = chainPreflight; 
        LOG.exiting("PluginImpl","PluginImpl(:String,:String,:String,:String,:boolean,:String,:boolean)");
    }

    public boolean isChainPreflight() { return this.chainPreflight; }
    public void setChainPreflight(final boolean v) { this.chainPreflight = chainPreflight; }
    public boolean getAllowCredentials() {return this.allowCredentials;}
    public void isAllowCredentials(final boolean v) { this.allowCredentials = allowCredentials; }
    public String getAllowedOrigins() { 
        LOG.finer("PluginImpl.getAllowedOrigins");
        return this.allowedOrigins;
    }
    public void setAllowedOrigins(final String allowedOrigins) {
        LOG.entering("PluginImpl","setAllowedOrigins");
        this.allowedOrigins = allowedOrigins;
        LOG.exiting("PluginImpl","setAllowedOrigins");
    }
    public String getAllowedMethods() { return this.allowedMethods; }
    public void setAllowedMethods(final String allowedMethods) { this.allowedMethods = allowedMethods; }
    public String getAllowedHeaders() { return this.allowedHeaders; }
    public void setAllowedHeaders(final String allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public String getPreflightMaxAge() { return this.preflightMaxAge; }
    public void setPreflightMaxAge(final String preflightMaxAge) { this.preflightMaxAge = preflightMaxAge; }
    public String getExposedHeaders() { return this.exposedHeaders; }
    public void setExposedHeaders(final String exposedHeaders) { this.exposedHeaders = exposedHeaders; }

    /** {@inheritDoc} */
    @Override
    public void setServletContext(ServletContext context) {
        super.setServletContext(context);
        LOG.entering("PluginImpl","setServletContext");
        this.context = context;
    }

    /** {@inheritDoc} */
    @Override
    public void start() throws Exception {
        super.start();
        LOG.entering("PluginImpl","start");
        try {
            load(); 
        } catch (java.io.IOException e) {
            LOG.severe("error trying to load serialized plugin values");
        }

        LOG.fine("creating CrossOriginFilter");
        // create and install the filter
        CrossOriginFilter myFilter = new CrossOriginFilter();
        PluginServletFilter.addFilter(myFilter);
        this.filter = myFilter;

        LOG.exiting("PluginImpl","start");
    }

    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();
        LOG.entering("PluginImpl","postInitialize");
        // log config field values in the instance variables 
        LOG.config("start() called with following state: " +
                   CrossOriginFilter.ALLOWED_ORIGINS_PARAM   + "=" +  allowedOrigins   + ", " +
                   CrossOriginFilter.ALLOWED_METHODS_PARAM   + "=" +  allowedMethods   + ", " +
                   CrossOriginFilter.ALLOWED_HEADERS_PARAM   + "=" +  allowedHeaders   + ", " +
                   CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM + "=" +  preflightMaxAge  + ", " +
                   CrossOriginFilter.ALLOW_CREDENTIALS_PARAM + "=" +  allowCredentials + ", " +
                   CrossOriginFilter.EXPOSED_HEADERS_PARAM   + "=" +  exposedHeaders   + ", " +
                   CrossOriginFilter.CHAIN_PREFLIGHT_PARAM   + "=" +  chainPreflight)   ;

        // generate a FilterConfigWrapper from this's state
        FilterConfigWrapper configWrapper = createFilterConfigWrapper();
        filter.init(configWrapper);
        LOG.exiting("PluginImpl","postInitialize");
    }

    private FilterConfigWrapper createFilterConfigWrapper() {
        // put plugin's config properties in a Map
        final Map<String,String> paramMap = new HashMap<String,String>() {{
                put(CrossOriginFilter.ALLOWED_ORIGINS_PARAM   , allowedOrigins );
                put(CrossOriginFilter.ALLOWED_METHODS_PARAM   , allowedMethods );
                put(CrossOriginFilter.ALLOWED_HEADERS_PARAM   , allowedHeaders );
                put(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM , preflightMaxAge );
                put(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM , (allowCredentials ? "true" : "false") );
                put(CrossOriginFilter.EXPOSED_HEADERS_PARAM   , exposedHeaders );
                put(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM   , (chainPreflight ? "true" : "false") );
            }};
        // wrap them in a FilterConfigWrapper, with the servlet context
        final FilterConfigWrapper configWrapper = new FilterConfigWrapper("filterName",this.context,paramMap);
        return configWrapper;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() throws Exception {
        super.stop();
        LOG.entering("PluginImpl","stop");
        filter.destroy();
        LOG.exiting("PluginImpl","stop");
    }

    @Override
    public void configure(StaplerRequest req,
                          JSONObject formData)
        throws java.io.IOException,
               javax.servlet.ServletException,
               hudson.model.Descriptor.FormException
    {
        LOG.entering("PluginImpl","configure");
        LOG.config("configure() called with form data: " + 
                   CrossOriginFilter.ALLOWED_ORIGINS_PARAM   + "=" + formData.getString(  CrossOriginFilter.ALLOWED_ORIGINS_PARAM) + ", " +
                   CrossOriginFilter.ALLOWED_METHODS_PARAM   + "=" + formData.getString(  CrossOriginFilter.ALLOWED_METHODS_PARAM) + ", " +
                   CrossOriginFilter.ALLOWED_HEADERS_PARAM   + "=" + formData.getString(  CrossOriginFilter.ALLOWED_HEADERS_PARAM) + ", " +
                   CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM + "=" + formData.getString(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM) + ", " +
                   CrossOriginFilter.ALLOW_CREDENTIALS_PARAM + "=" + formData.getString(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM) + ", " +
                   CrossOriginFilter.EXPOSED_HEADERS_PARAM   + "=" + formData.getString(  CrossOriginFilter.EXPOSED_HEADERS_PARAM) + ", " +
                   CrossOriginFilter.CHAIN_PREFLIGHT_PARAM   + "=" + formData.getString(  CrossOriginFilter.CHAIN_PREFLIGHT_PARAM) );
        
        LOG.config("configure called with formData.getString(\"name\") = " + formData.getString("name"));

        allowedOrigins   =   formData.getString(  CrossOriginFilter.ALLOWED_ORIGINS_PARAM);
        allowedMethods   =   formData.getString(  CrossOriginFilter.ALLOWED_METHODS_PARAM);
        allowedHeaders   =   formData.getString(  CrossOriginFilter.ALLOWED_HEADERS_PARAM);
        preflightMaxAge  =   formData.getString(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM);
        allowCredentials =   formData.getString(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM).equals("true");
        exposedHeaders   =   formData.getString(  CrossOriginFilter.EXPOSED_HEADERS_PARAM);
        chainPreflight   =   formData.getString(  CrossOriginFilter.CHAIN_PREFLIGHT_PARAM).equals("true");

        LOG.finer("saving() new config values");
        save();

        LOG.finer("re-initializaing filter with new values");
        FilterConfigWrapper configWrapper = createFilterConfigWrapper();
        this.filter.init(configWrapper);

        LOG.exiting("PluginImpl","configure");
        return ;
    }
}
