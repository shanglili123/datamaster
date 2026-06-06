FROM apache/doris:4.0.3-all-slim

COPY start-doris-local.sh /usr/local/bin/start-doris-local.sh

RUN sed -i 's/-Xmx8192m -Xms8192m/-Xmx1024m -Xms1024m/' /opt/apache-doris/fe/conf/fe.conf \
    && sed -i 's/-Xmx2048m/-Xmx1024m/' /opt/apache-doris/be/conf/be.conf \
    && sed -i 's/if \[\[ "${MAX_MAP_COUNT}" -lt 2000000 \]\]; then/if false; then/' /opt/apache-doris/be/bin/start_be.sh \
    && sed -i 's/if \\[\\[ "$(swapon -s | wc -l)" -gt 1 \\]\\]; then/if false; then/' /opt/apache-doris/be/bin/start_be.sh \
    && printf '#!/bin/bash\nexit 0\n' >/usr/local/bin/stop_broker.sh \
    && chmod +x /usr/local/bin/stop_broker.sh /usr/local/bin/start-doris-local.sh

ENTRYPOINT ["bash", "/usr/local/bin/start-doris-local.sh"]
