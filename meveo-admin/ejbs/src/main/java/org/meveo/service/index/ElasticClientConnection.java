package org.meveo.service.index;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.meveo.commons.utils.ParamBean;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Establish a connection to Elastic Search cluster
 * 
 * @author Andrius Karpavicius
 * @author Wassim Drira
 * @lastModifiedVersion 5.0
 *
 */
@Startup
@Singleton
@Lock(LockType.READ)
public class ElasticClientConnection {

    @Inject
    private Logger log;

    @Inject
    private ElasticSearchConfiguration esConfiguration;

    private ParamBean paramBean = ParamBean.getInstance();

    /**
     * The actual ES client
     */
    private RestHighLevelClient client = null;

    /**
     * Is Elastic Search enabled/connected
     */
    private boolean esEnabled = false;

    /**
     * Initialize Elastic Search client connection
     */
    @PostConstruct
    private void initES() {

        String clusterName = null;
        String[] hosts = null;
        String portStr = null;
        
        if(paramBean.getProperty("elasticsearch.disabled", "false").equals("true")) {
        	return;
        }

        try {

            String restUri = paramBean.getProperty("elasticsearch.restUri", "http://localhost:9200");
            hosts = restUri.split(";");

            if (StringUtils.isBlank(restUri) || hosts.length == 0) {
                log.warn("Elastic search is not enabled. Current settings: hosts={}", hosts.toString());

            } else {
                HttpHost[] httpHosts = new HttpHost[hosts.length];

                for (int i = 0; i < hosts.length; i++) {
                    httpHosts[i] = HttpHost.create(hosts[i]);
                }

                client = new RestHighLevelClient(RestClient.builder(httpHosts));

                @SuppressWarnings("unused")
                MainResponse response = client.info(RequestOptions.DEFAULT);
            }

        } catch (Exception e) {
            log.error("Error while initializing elastic search. Current settings:  hosts={}", hosts, e);
            shutdownES();
        }

        try {
            if (client != null) {
                esConfiguration.loadConfiguration();
            }
        } catch (Exception e) {
            log.error("Error while loading elastic search mapping configuration", e);
            shutdownES();
        }

        esEnabled = client != null;

    }

    /**
     * Shutdown Elastic Search client
     */
    @PreDestroy
    private void shutdownES() {
        if (client != null) {
            try {
                client.close();
                client = null;
            } catch (Exception e) {
                log.error("Failed to close ES client", e);
            }
        }
    }

    /**
     * Reinitialize ES connection
     */
    public void reinitES() {
        shutdownES();
        initES();
    }

    /**
     * Is Elastic Search integration turned on.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return esEnabled;
    }

    /**
     * Get Elastic Search client instance
     * 
     * @return Elastic Search client instance
     */
    public RestHighLevelClient getClient() {
        return client;
    }
}