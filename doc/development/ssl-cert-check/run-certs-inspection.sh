#!/bin/bash

###################################################################################################
### Script: run-certs-inspection.sh
### 
### Purpose: Tests whether particular SSL/TLS certificates are expired or are near expiring.
###
###          On local machine, examines HPC server cert contained in a Java keystore that has been
###          deployed to Apache Servicemix: ${Servicemix home}/etc/hpc-server/keystore.jks.
###          Also examines selected CA certs contained in another Java keystore that has been 
###          deployed to Apache Servicemix: ${Servicemix home}/etc/hpc-server/cacerts.jks.  Those 
###          certs are NIH IRODS certificate, NIH LDAP root certificate, and NIH intermediate CA 
###          certificate.
###
###          This script also examines certs for HPC DME web server and HPC DME REST API server in
###          a different environment/tier.  The strategy is:
###            a. DEV machine examines certs for UAT tier.
###            b. UAT machine examines certs for PROD tier.
###            c. PROD machine examines certs for DEV tier.
###
### Assumptions:
###   1.  This script is executed on a server on which Apache Servicemix is installed.
###   2.  The server on which this script is executed has Apache Servicemix installed at the
###        local file system path of /opt/apache-servicemix-<version number>.
###   3.  The HPC DME server-side software (at least hpc-server-rest-services and optionally 
###        hpc-server-scheduler) has been installed in Apache Servicemix.
###
### Original author: William Yu-Wei Liu | william.liu2@nih.gov
### Original revision date: 2018-02-21
###
### Revision history:
### [entry # | date | author | description ]
### 
### 1.   2018-02-23   William Yu-Wei Liu     Removed assumption that a cronjob executes this script
###                   william.liu2@nih.gov    with environment variables of service account user
###                                           available.  Refactored script code to use divide logic  
###                                           into functions.
###################################################################################################


####################################################################################################
### BEGIN: Set environment-agnostic variables that may change commonly
####################################################################################################

### If number of days elapsed since a log file was last modified is greater than or equal
###  to the number stored in DAYS_AGE_FOR_LOG_DELETE, then delete that log file.
DAYS_AGE_FOR_LOG_DELETE=30

### If a certificate shall expire within number of days stored in EXPIRY_THRESHOLD_DAYS, then
###  send notification email about certificate expiring soon.
EXPIRY_THRESHOLD_DAYS=21

### Send notification emails to this address or these addresses; separate multiple addresses using
###  commas
TO_EMAIL_ADDR=HPC_DME_Admin@mail.nih.gov
#TO_EMAIL_ADDR=william.liu2@nih.gov

####################################################################################################
### END: Set environment-agnostic variables that may change commonly
####################################################################################################


####################################################################################################
### BEGIN: Set environment-agnostic variables that probably change rarely
####################################################################################################
ALIAS_HPC_SERVER_KEY=hpc-server-key
DOMAIN_EXTENSION=ncifcrf.gov
ETC_HPC_SERVER=etc/hpc-server
HPC_CACERTS_ALIASES=( nih-irods nih-dpki-root-1a nih-dpki-ca-1a )
HPC_CACERTS_STORE_PASSWORD=changeit
HPC_SERVER_KEYSTORE_PASSWORD=changeit
LOG_FILE_DIR=/tmp
LOG_FILE_EXTENSION=log
LOG_FILE_NAME_PREFIX=cert-check-log-
SCRIPT_NAME=run-certs-inspection.sh
TIMESTAMP=`date +"%Y-%m-%d-%H%M"`
TMP_FILES_DIR=/tmp
TMP_FILE_SUFFIX=-rfc.crt

LOG_FILE=$LOG_FILE_DIR/$LOG_FILE_NAME_PREFIX$TIMESTAMP.$LOG_FILE_EXTENSION
LOG_FILE_PATTERN=$LOG_FILE_DIR/$LOG_FILE_NAME_PREFIX*.$LOG_FILE_EXTENSION
####################################################################################################
### END: Set environment-agnostic variables that probably change rarely
####################################################################################################

####################################################################################################
### BEGIN: Set environment-dependent variables
####################################################################################################
THIS_HOST=`hostname`
if [[ $THIS_HOST = "fr-s-hpcdm-gp-d"* ]]; then
  APACHE_SRVCMIX_HOME=/opt/apache-servicemix-7.0.0.M3
  EMAIL_DOMAIN=fr-s-hpcdm-gp-d.$DOMAIN_EXTENSION
  SRVC_ACCNT_USER_HOME=/home/NCIF-HPCDM-SVC
  ### From DEV, check UAT servers
  SSLDOMAINS_FILE=ssldomains-uat
elif [[ $THIS_HOST = "fr-s-hpcdm-uat-p"* ]]; then
  APACHE_SRVCMIX_HOME=/opt/apache-servicemix-7.0.0
  EMAIL_DOMAIN=fr-s-hpcdm-uat-p.$DOMAIN_EXTENSION
  SRVC_ACCNT_USER_HOME=/home/NCIF-HPCDM-SVC
  ### From UAT, check PROD servers
  SSLDOMAINS_FILE=ssldomains-prod
else
  APACHE_SRVCMIX_HOME=/opt/apache-servicemix-7.0.0.M3
  EMAIL_DOMAIN=fr-s-dmeapi-t-p.$DOMAIN_EXTENSION
  SRVC_ACCNT_USER_HOME=/home/ncifhpcdmsvcp
  ### From PROD, check DEV servers
  SSLDOMAINS_FILE=ssldomains-dev
fi

FROM_EMAIL_ADDR=`whoami`@$EMAIL_DOMAIN
HPC_CACERTS_STORE=$APACHE_SRVCMIX_HOME/$ETC_HPC_SERVER/cacerts.jks
HPC_SERVER_KEYSTORE=$APACHE_SRVCMIX_HOME/$ETC_HPC_SERVER/keystore.jks
SSL_CERT_CHECK_HOME=$SRVC_ACCNT_USER_HOME/ssl-cert-check

COMMON_SSL_CERT_CHECK_OPTIONS="-x $EXPIRY_THRESHOLD_DAYS -i -a -e $TO_EMAIL_ADDR -E $FROM_EMAIL_ADDR"
####################################################################################################
### END: Set environment-dependent variables
####################################################################################################

inspect_ssldomains() {
  RET_STATUS=0
  if [ $# -ne 1 ]; then
    local USAGE_MSG="Usage: inspect_ssldomains <SSL domains file, as expected by ssl-cert-check utility>"
    echo $USAGE_MSG
    local MSG_SUBJ="Error running script for checking expiration of domain certificate(s)"
    echo -e "Script: $SCRIPT_NAME\nDetails ...\n$USAGE_MSG" | mail -s "$MSG_SUBJ" $TO_EMAIL_ADDR -aFrom:$FROM_EMAIL_ADDR
    RET_STATUS=1
  elif [ -f "$1" ]; then 
    local DOMAINS_FILE=$1
    $SSL_CERT_CHECK_HOME/ssl-cert-check $COMMON_SSL_CERT_CHECK_OPTIONS -f $DOMAINS_FILE
  else
    local NO_SUCH_FILE_MSG="The file $1 does not exist."
    echo $NO_SUCH_FILE_MSG
    local MSG_SUBJ="Error running script for checking expiration of domain certificate(s)"
    echo -e "Script: $SCRIPT_NAME\nDetails ...\n$NO_SUCH_FILE_MSG" | mail -s "$MSG_SUBJ" $TO_EMAIL_ADDR -aFrom:$FROM_EMAIL_ADDR
    RET_STATUS=1
  fi
  return $RET_STATUS
}

inspect_keystore_cert() {
  local RET_STATUS=0
  if [ $# -ne 3 ]; then
    local USAGE_MSG="Usage: inspect_keystore_cert <Java keystore file> <Java keystore password> <Java keystore entry alias>"
    echo $USAGE_MSG
    local MSG_SUBJ="Error running script for checking certificate expiration"
    echo -e "Script: $SCRIPT_NAME\nDetails ...\n$USAGE_MSG" | mail -s "$MSG_SUBJ" $TO_EMAIL_ADDR -aFrom:$FROM_EMAIL_ADDR
    RET_STATUS=1
  else 
    local KYSTR=$1
    local KYSTR_PSSWRD=$2
    local KYSTR_ALIAS=$3
    local TMP_CERT_FILE=$TMP_FILES_DIR/$KYSTR_ALIAS$TMP_FILE_SUFFIX

    keytool -keystore $KYSTR -storepass $KYSTR_PSSWRD -list -rfc -alias $KYSTR_ALIAS > $TMP_CERT_FILE 2>&1
    local FIRST_LINE=`sed -n 1p $TMP_CERT_FILE` 

    if [[ "${FIRST_LINE:0:12}" != "Alias name: " ]]; then
      local TMP_FILE_MSG_BODY=$TMP_FILES_DIR/tmp-msg-body.txt

      # First echo into file is to replace existing content, not append to it.
      echo "There was a failure to examine certificate in Java keystore, $KYSTR." > $TMP_FILE_MSG_BODY

      echo -e "\nKeystore password provided was $KYSTR_PSSWRD." >> $TMP_FILE_MSG_BODY
      echo -e "\nKeystore entry alias provided was $KYSTR_ALIAS." >> $TMP_FILE_MSG_BODY
      echo -e "\nMore details appear below.\n\n" >> $TMP_FILE_MSG_BODY
      cat $TMP_CERT_FILE >> $TMP_FILE_MSG_BODY

      local MSG_SUBJ="Failure to examine certificate in Java keystore"
      cat $TMP_FILE_MSG_BODY | mail -s "$MSG_SUBJ" $TO_EMAIL_ADDR -aFrom:$FROM_EMAIL_ADDR

      # Send message content to log file
      cat $TMP_FILE_MSG_BODY >> $LOG_FILE

      # Remove temporary file that was used to compose message content
      rm -f $TMP_FILE_MSG_BODY

      RET_STATUS=1
    else
      $SSL_CERT_CHECK_HOME/ssl-cert-check $COMMON_SSL_CERT_CHECK_OPTIONS -c "$TMP_CERT_FILE" -j $KYSTR -w $KYSTR_ALIAS
    fi

    # Remove temporary file containing output from attempt to extract certificate from keystore file
    rm -f $TMP_CERT_FILE
  fi

  return $RET_STATUS
}

main() {
  inspect_keystore_cert $HPC_SERVER_KEYSTORE $HPC_SERVER_KEYSTORE_PASSWORD $ALIAS_HPC_SERVER_KEY >> $LOG_FILE 2>&1
  for SOME_CACERT_ALIAS in "${HPC_CACERTS_ALIASES[@]}"
  do
    inspect_keystore_cert $HPC_CACERTS_STORE $HPC_CACERTS_STORE_PASSWORD $SOME_CACERT_ALIAS >> $LOG_FILE 2>&1
  done
  
  inspect_ssldomains $SSLDOMAINS_FILE >> $LOG_FILE 2>&1

  # Delete any log files that this script generated 90 or more days ago
  find $LOG_FILE_PATTERN -type f -mtime +$((DAYS_AGE_FOR_LOG_DELETE - 1)) -exec rm {} \;
}

main