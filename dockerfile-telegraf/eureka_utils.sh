#!/bin/bash

### PROVIDE LOCAL URLS ###
# An alternative to https://github.com/docker/swarm/issues/1106

call_eureka() {
    if hash curl 2>/dev/null; then
        curl -s "$@"
    else
        wget -q -O - "$@"
    fi
}

add_dns_entry() {
  target=$2
  host=$1
  # https://stackoverflow.com/questions/24991136/docker-build-could-not-resolve-archive-ubuntu-com-apt-get-fails-to-install-a
  ip=$(nslookup ${target} 2>/dev/null | tail -n1 | awk '{ print $3 }')
  # http://jasani.org/2014/11/19/docker-now-supports-adding-host-mappings/
  sed -i "/${host}\$/d" ~/hosts.new
  echo "$ip $host" >> ~/hosts.new
}

setup_local_containers() {
  # http://blog.jonathanargentiero.com/docker-sed-cannot-rename-etcsedl8ysxl-device-or-resource-busy/
  cp /etc/hosts ~/hosts.new

  if [ -z "$NODE_ID" ]; then
      SERVICES=$(call_eureka http://${EUREKA_URL}:${EUREKA_PORT}/services)
  else
      SERVICES=$(call_eureka http://${EUREKA_URL}:${EUREKA_PORT}/services/node/${NODE_ID})
  fi

  # https://stedolan.github.io/jq/
  while IFS="=" read name value; do
    container="${value/%\ */}"
    export "${name//-/_}=\"${container}\""
    add_dns_entry ${name} ${container}

    export "${name//-/_}0=\"$value\""
    i=1
    for container in $value; do
      ## Stored as an Environment Variable
      entry=${name}$((i++))
      export "${entry//-/_}=\"${container}\""
      ## Added as a DNS entry
      add_dns_entry ${entry} ${container}
    done
  done < <( echo "$SERVICES" | jq '.[] | tostring' | sed -e 's/\"{\\\"//g' -e 's/\\\"\:\[\\\"/_local=/g' -e 's/\\\",\\\"/\\\ /g' -e 's/\\\"]}\"//g')

  # cp -f ~/hosts.new /etc/hosts # cp: can't create '/etc/hosts': File exists
  echo "$(cat ~/hosts.new)" > /etc/hosts

  if [ "$DEBUG" = "true" ]; then
    echo $EUREKA_URL:$EUREKA_PORT
    env | grep _local | sort
    cat /etc/hosts
  fi
}

### CHECK DEPENDENCIES ###
# https://github.com/moby/moby/issues/31333#issuecomment-303250242

# https://stackoverflow.com/questions/26177059/refresh-net-core-somaxcomm-or-any-sysctl-property-for-docker-containers/26197875#26197875
# https://stackoverflow.com/questions/26050899/how-to-mount-host-volumes-into-docker-containers-in-dockerfile-during-build
# docker run ... -v /proc:/writable-proc ...
desable_ping() {
  echo "1" >  /writable-proc/sys/net/ipv4/icmp_echo_ignore_all
}

enable_ping() {
  echo "0" >  /writable-proc/sys/net/ipv4/icmp_echo_ignore_all
}

kill_cmdpid () {
  declare cmdpid=$1
  # http://www.bashcookbook.com/bashinfo/source/bash-4.0/examples/scripts/timeout3
  # Be nice, post SIGTERM first.
  # The 'exit 0' below will be executed if any preceeding command fails.
  kill -s SIGTERM $cmdpid && kill -0 $cmdpid || exit 0
  sleep $delay
  kill -s SIGKILL $cmdpid
}

#### Initial Checks ####

initial_check() {
  declare cmdpid=$1

  #### SETUP timeout

  if [ "${CHECK_TIMEOUT}" ]; then
    # http://www.bashcookbook.com/bashinfo/source/bash-4.0/examples/scripts/timeout3
    # Timeout.
    declare -i timeout=CHECK_TIMEOUT
    # kill -0 pid   Exit code indicates if a signal may be sent to $pid process.
    (
        ((t = timeout))

        while ((t > 0)); do
            echo "$t Second(s) Remaining Before Timeout"
            sleep $interval
            kill -0 $$ || exit 0
            ((t -= interval))
        done

        if ((started == 0)); then
          echo "Timeout. Will EXIT"
          # Be nice, post SIGTERM first.
          # The 'exit 0' below will be executed if any preceeding command fails.
          kill -s SIGTERM $cmdpid && kill -0 $cmdpid || exit 0
          sleep $delay
          kill -s SIGKILL $cmdpid
        fi
    ) 2> /dev/null &
  fi

  # https://docs.docker.com/compose/startup-order/
  if [ "${DEPENDS_ON}" ]; then
    >&2 echo "Checking DEPENDENCIES ${DEPENDS_ON}"
    until [ "$(call_eureka http://${EUREKA_URL}:${EUREKA_PORT}/dependencies/${DEPENDS_ON})" == "OK" ]; do
      >&2 echo "Still WAITING for Dependencies ${DEPENDS_ON}"
      sleep $interval
    done
  fi

  # https://github.com/Eficode/wait-for
  if [ "${WAIT_FOR}" ]; then
    >&2 echo "Checking URLS $WAIT_FOR"
    URLS=$(echo $WAIT_FOR | tr "," "\n")
    for URL in $URLS
    do
      if [[ $URL == *":"* ]]; then # url + port
        HOST=$(printf "%s\n" "$URL"| cut -d : -f 1)
        PORT=$(printf "%s\n" "$URL"| cut -d : -f 2)
        until nc -z "$HOST" "$PORT" > /dev/null 2>&1 ; result=$? ; [ $result -eq 0 ] ; do
          >&2 echo "Still WAITING for URL $HOST:$PORT"
          sleep $interval
          setup_local_containers &
        done
      else # ping url
        until ping -c1 "$URL" &>/dev/null; do
          >&2 echo "Still WAITING for $URL PING"
          sleep $interval
          setup_local_containers &
        done
      fi
    done
  fi

  if [ "${CHECK_TIMEOUT}" ]; then
    # $! expands to the PID of the last process executed in the background.
    kill $!
  fi
}

#### Continuous Checks ####

check_dependencies(){
  declare cmdpid=$1

  if [ "${DEPENDS_ON}" ]; then
    dependencies_checked=$(call_eureka http://${EUREKA_URL}:${EUREKA_PORT}/dependencies/${DEPENDS_ON})
    if [ "$dependencies_checked" != "OK" ]; then
      >&2 echo "Failed Check Dependencies ${DEPENDS_ON}"
      kill_cmdpid $cmdpid
    fi
  fi

  # https://github.com/Eficode/wait-for
  if [ "${WAIT_FOR}" ]; then
    URLS=$(echo $WAIT_FOR | tr "," "\n")
    for URL in $URLS
    do
      if [[ $URL == *":"* ]]; then # url + port
        HOST=$(printf "%s\n" "$URL"| cut -d : -f 1)
        PORT=$(printf "%s\n" "$URL"| cut -d : -f 2)
        nc -z "$HOST" "$PORT" > /dev/null 2>&1 ; result=$? ;
        if [ $result -ne 0 ] ; then
          >&2 echo "Failed Check URL ${URL}"
          kill_cmdpid $cmdpid
        fi
      elif ! ping -c 1 "$URL" &>/dev/null ; then # ping url
        >&2 echo "Failed ${URL} Ping"
        kill_cmdpid $cmdpid
      fi
    done
  fi
}

infinite_setup_check(){
  while true
  do
    setup_local_containers &
    sleep $interval
    if [[ "${DEPENDS_ON}" || "${CHECK_TIMEOUT}" ]]; then
      check_dependencies $1 &
    fi
  done
}

monitor_output() {
  declare cmdpid=$2

  if [ "$ready" = false ] && [[ $1 == *"${READY_WHEN}"* ]]; then
    >&2 echo "READY!"
    ready="$READINESS"
    enable_ping
  fi
  if [ "$ready" = true ] && [[ $1 == *"${FAILED_WHEN}"* ]]; then
    >&2 echo "FAILED!"
    if [ ${KILL_WHEN_FAILED} ]; then
      kill_cmdpid $cmdpid
    else
      ready=false
      desable_ping
    fi
  fi
}
