#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
if [[ $(basename ${BASE}) != "wordland-server" && -d "${BASE}/wordland-server" ]] ; then
  BASE="${BASE}/wordland-server"
fi

if [ -f ~/.wl.env ] ; then
  . ~/.wl.env
fi

debug="${1}"
if [ "x${debug}" = "xdebug" ] ; then
  shift
  ARG_LEN=$(echo -n "${1}" | wc -c)
  ARG_NUMERIC_LEN=$(echo -n "${1}" | tr -dc [:digit:] | wc -c)  # strip all non-digits
  if [ ${ARG_LEN} -eq ${ARG_NUMERIC_LEN} ] ; then
    # Second arg is the debug port
    DEBUG_PORT="${1}"
    shift
  fi
  if [ -z "${DEBUG_PORT}" ] ; then
    DEBUG_PORT=6005
  fi
  debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=${DEBUG_PORT}"
else
  debug=""
fi

command="${1}"
if [ -z "${command}" ] ; then
  CLASS=wordland.server.WordlandServer
else
  CLASS=wordland.main.WordlandMain
  shift
fi

JAR="${BASE}/target/wordland-server-1.0.0-SNAPSHOT.jar"

java ${debug} -Xmx1900m -Xms1900m -Djava.net.preferIPv4Stack=true -server -cp ${JAR} ${CLASS} ${command} "${@}"
