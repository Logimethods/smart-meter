FROM prom/prometheus:${prometheus_version}
ADD prometheus.yml /etc/prometheus/
