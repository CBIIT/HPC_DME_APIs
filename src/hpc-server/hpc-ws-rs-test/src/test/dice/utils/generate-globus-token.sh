#!/bin/bash

source $HPC_DM_TEST/utils/functions

LOG_FILE=$HPC_DM_TEST/utils/globus-log
rm $LOG_FILE 2> /dev/null

SERVER="https://nexus.api.globusonline.org/goauth/token?grant_type=client_credentials"

curl -u `get_globus_user` 'https://nexus.api.globusonline.org/goauth/token?grant_type=client_credentials' -s > $LOG_FILE 

TOKEN=$(get_json_value $LOG_FILE access_token)

if [ -z $TOKEN ]
then
        echo "ERROR: Can not get Globus token." 
        exit
fi


GLOBUS_CONFIG="$HPC_DM_TEST/utils/globus-config"

CONFIG=$(cat $GLOBUS_CONFIG-sample | sed "s@Bearer.*@Bearer $TOKEN\"@")

echo "$CONFIG" > $GLOBUS_CONFIG 

