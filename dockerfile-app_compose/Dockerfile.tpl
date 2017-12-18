FROM ((docker-dz_compose-repository)):((docker-dz_compose-tag))((docker-additional-tag))

COPY *.sh ./
COPY compose/*.yml ./
COPY properties/*.properties properties/*.yml ./properties/