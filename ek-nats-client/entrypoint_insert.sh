export NATS_USERNAME=${NATS_USERNAME:=`cat ${NATS_USERNAME_FILE}`}
export NATS_PASSWORD=${NATS_PASSWORD:=`cat ${NATS_PASSWORD_FILE}`}

env
echo "--------------------"
