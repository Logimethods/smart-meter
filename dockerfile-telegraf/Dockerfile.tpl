FROM telegraf:${telegraf_version}

COPY conf/ /etc/telegraf/
