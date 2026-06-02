package io.crest.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crest.observability")
public class ObservabilityProperties {

    private final Prometheus prometheus = new Prometheus();

    private final Grafana grafana = new Grafana();

    private Api api = new Api();

    public Prometheus getPrometheus() {
        return prometheus;
    }

    public Grafana getGrafana() {
        return grafana;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public static class Prometheus {
        private boolean enabled;
        private String endpoint = "/api/v1/actuator/prometheus";
        private String token;
        private int maxUriTags = 200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public int getMaxUriTags() {
            return maxUriTags;
        }

        public void setMaxUriTags(int maxUriTags) {
            this.maxUriTags = maxUriTags;
        }
    }

    public static class Grafana {
        private boolean enabled;
        private String publicUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }
    }

    public static class Api {
        private boolean routeInventoryEnabled = true;
        private String routePrefix = "/api/v1";

        public boolean isRouteInventoryEnabled() {
            return routeInventoryEnabled;
        }

        public void setRouteInventoryEnabled(boolean routeInventoryEnabled) {
            this.routeInventoryEnabled = routeInventoryEnabled;
        }

        public String getRoutePrefix() {
            return routePrefix;
        }

        public void setRoutePrefix(String routePrefix) {
            this.routePrefix = routePrefix;
        }
    }
}
