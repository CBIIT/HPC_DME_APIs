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
###   1.  This script is executed by or as service account user, either ncihpcdmsvcp in PROD or
###        ncif-hpcdm-svc in UAT or DEV.
###   2.  This script is executed on a server on which Apache Servicemix is installed.
###   3.  There is an environment variable named SERVICEMIX_HOME having value which is absolute path 
###        in file system where Apache Servicemix is installed.  For example, 
###        export SERVICEMIX_HOME=/opt/apache-servicemix-7.0.0.M3
###   4.  The HPC DME server-side software (at least hpc-server-rest-services and optionally 
###        hpc-server-scheduler) has been installed in Apache Servicemix.
###
### Original author: William Yu-Wei Liu | william.liu2@nih.gov
### Original revision date: February 21, 2018
###
### Revision history:
###   (date | author | description )
###
###################################################################################################

### If a certificate shall expire within number of days stored in EXPIRY_THRESHOLD_DAYS, then
###  send notification email about certificate expiring soon.
EXPIRY_THRESHOLD_DAYS=21

### Send notification emails to this address
TARGET_EMAIL_ADDR=HPC_DME_Admin@mail.nih.gov

### If number of days elapsed since a log file was last modified is greater than or equal
###  to the number stored in DAYS_AGE_FOR_LOG_DELETE, then delete that log file.
DAYS_AGE_FOR_LOG_DELETE=30


### Assume service account user is running this script
SCRIPT_INVOKING_USER=`whoami`

THIS_HOST=`hostname`
### Depending on which environment, choose proper email domain & file listing servers whose 
###  certificates you want to check
if [[ $THIS_HOST = "fr-s-hpcdm-gp-d"* ]]; then
  EMAIL_DOMAIN=fr-s-hpcdm-gp-d
  ### From DEV, check UAT servers
  SSLDOMAINS_FILE=ssldomains-uat
elif [[ $THIS_HOST = "fr-s-hpcdm-uat-p"* ]]; then
  EMAIL_DOMAIN=fr-s-hpcdm-uat-p
  ### From UAT, check PROD servers
  SSLDOMAINS_FILE=ssldomains-prod
else
  EMAIL_DOMAIN=fr-s-dmeapi-t-p
  ### From PROD, check DEV servers
  SSLDOMAINS_FILE=ssldomains-dev
fi
DOMAIN_EXTENSION=ncifcrg.gov
EMAIL_DOMAIN=$EMAIL_DOMAIN.$DOMAIN_EXTENSION
SENDER_EMAIL_ADDR=$SCRIPT_INVOKING_USER@$EMAIL_DOMAIN

ETC_HPC_SERVER=etc/hpc-server

HPC_SERVER_KEYSTORE=$SERVICEMIX_HOME/$ETC_HPC_SERVER/keystore.jks
HPC_SERVER_KEYSTORE_PASSWORD=changeit
ALIAS_HPC_SERVER_KEY=hpc-server-key

HPC_CACERTS_STORE=$SERVICEMIX_HOME/$ETC_HPC_SERVER/cacerts.jks
HPC_CACERTS_STORE_PASSWORD=changeit
ALIAS_NIH_IRODS=nih-irods
ALIAS_NIH_ROOT=nih-dpki-root-1a
ALIAS_NIH_INTERMED=nih-dpki-ca-1a

SSL_CERT_CHECK_HOME=~/ssl-cert-check

TIMESTAMP=`date +"%Y-%m-%d-%H%M"`

TMP_FILES_DIR=/tmp/ssl-cert-check-$TIMESTAMP
TMP_FILE_SUFFIX=-rfc.crt

TMP_HPC_SERVER_KEY_CERT_FILE=$TMP_FILES_DIR/hpc-server-key$TMP_FILE_SUFFIX
TMP_NIH_IRODS_CERT_FILE=$TMP_FILES_DIR/nih-irods$TMP_FILE_SUFFIX
TMP_NIH_ROOT_CERT_FILE=$TMP_FILES_DIR/nih-root$TMP_FILE_SUFFIX
TMP_NIH_INTERMED_CERT_FILE=$TMP_FILES_DIR/nih-intermed$TMP_FILE_SUFFIX

TMP_CERT_FILES_PATTERN=$TMP_FILES_DIR/*.crt

LOG_FILE_DIR=/tmp
LOG_FILE_NAME_PREFIX=cert-check-log-
LOG_FILE_EXTENSION=log
LOG_FILE=$LOG_FILE_DIR/$LOG_FILE_NAME_PREFIX$TIMESTAMP.$LOG_FILE_EXTENSION
LOG_FILE_PATTERN=$LOG_FILE_DIR/$LOG_FILE_NAME_PREFIX*.$LOG_FILE_EXTENSION

mkdir $TMP_FILES_DIR

keytool -list -rfc -alias $ALIAS_HPC_SERVER_KEY \
        -keystore $HPC_SERVER_KEYSTORE -storepass $HPC_SERVER_KEYSTORE_PASSWORD \
         > $TMP_HPC_SERVER_KEY_CERT_FILE 2>&1

keytool -list -rfc -alias $ALIAS_NIH_IRODS \
        -keystore $HPC_CACERTS_STORE -storepass $HPC_CACERTS_STORE_PASSWORD \
        > $TMP_NIH_IRODS_CERT_FILE 2>&1

keytool -list -rfc -alias $ALIAS_NIH_ROOT \
        -keystore $HPC_CACERTS_STORE -storepass $HPC_CACERTS_STORE_PASSWORD \
        > $TMP_NIH_ROOT_CERT_FILE 2>&1

keytool -list -rfc -alias $ALIAS_NIH_INTERMED \
        -keystore $HPC_CACERTS_STORE -storepass $HPC_CACERTS_STORE_PASSWORD \
        > $TMP_NIH_INTERMED_CERT_FILE 2>&1

COMMON_SSL_CERT_CHECK_OPTIONS="-x $EXPIRY_THRESHOLD_DAYS -i -a -e $TARGET_EMAIL_ADDR -E $SENDER_EMAIL_ADDR"

$SSL_CERT_CHECK_HOME/ssl-cert-check -c "$TMP_HPC_SERVER_KEY_CERT_FILE" \
 -j $HPC_SERVER_KEYSTORE -w $ALIAS_HPC_SERVER_KEY \
 $COMMON_SSL_CERT_CHECK_OPTIONS > $LOG_FILE 2>&1

$SSL_CERT_CHECK_HOME/ssl-cert-check -c "$TMP_NIH_IRODS_CERT_FILE" \
 -j $HPC_CACERTS_STORE -w $ALIAS_NIH_IRODS \
  $COMMON_SSL_CERT_CHECK_OPTIONS >> $LOG_FILE 2>&1

$SSL_CERT_CHECK_HOME/ssl-cert-check -c "$TMP_NIH_ROOT_CERT_FILE" \
 -j $HPC_CACERTS_STORE -w $ALIAS_NIH_ROOT \
 $COMMON_SSL_CERT_CHECK_OPTIONS >> $LOG_FILE 2>&1

$SSL_CERT_CHECK_HOME/ssl-cert-check -c "$TMP_NIH_INTERMED_CERT_FILE" \
 -j $HPC_CACERTS_STORE -w $ALIAS_NIH_INTERMED \
 $COMMON_SSL_CERT_CHECK_OPTIONS >> $LOG_FILE 2>&1

$SSL_CERT_CHECK_HOME/ssl-cert-check -f $SSLDOMAINS_FILE \
 $COMMON_SSL_CERT_CHECK_OPTIONS >> $LOG_FILE 2>&1

rm -rf $TMP_FILES_DIR

# Delete any log files that this script generated 90 or more days ago
find $LOG_FILE_PATTERN -type f -mtime +$((DAYS_AGE_FOR_LOG_DELETE - 1)) -exec rm {} \;
