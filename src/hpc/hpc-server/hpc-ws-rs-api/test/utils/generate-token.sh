#!/bin/bash

source $HPC_DM_TEST/utils/functions

LOG_FILE=$HPC_DM_TEST/utils/log
rm $LOG_FILE 2>/dev/null

SERVER=$(cat $HPC_DM_TEST/utils/server)


curl -k -u `get_username` ${SERVER}/authenticate >  $LOG_FILE
TOKEN=$(get_json_value $LOG_FILE token)

if [ -z $TOKEN ]
then
    echo "Error: no token found in $LOG_FILE "
    exit
fi

CONFIG=$(cat "$HPC_DM_TEST/utils/config-sample" | sed "s/Bearer.*/Bearer $TOKEN\"/")
echo "$CONFIG" > $HPC_DM_TEST/utils/config

