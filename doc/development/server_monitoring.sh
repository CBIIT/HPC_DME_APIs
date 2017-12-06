#!/bin/bash

NOTIFYEMAIL=HPC_DME_Admin@mail.nih.gov

#for SERVERIP in "129.43.164.41" "129.43.164.40" "129.43.165.137" "129.43.165.210" "29.43.164.125" "129.43.165.92" "129.43.165.62"

for SERVENAME in "fr-s-hpcdm-web-p.ncifcrf.gov" "fr-s-dmeapi-t-p.ncifcrf.gov" "fr-s-hpcdm-irods-p.ncifcrf.gov" "fr-s-hpcdm-api-p.ncifcrf.gov" "fr-s-dmedb-t-p.ncifcrf.gov" "fr-s-hpcdm-db-p.ncifcrf.gov" "fr-s-hpcdm-uat-p.ncifcrf.gov" "fr-s-hpcdm-gp-d.ncifcrf.gov"
end=$((SECONDS+30))
while [ $SECONDS -lt $end ]; do
 ping -c 3 $SERVENAME > /dev/null 2>&1

 if [ $? -ne 0 ]
 then
   # Use your favorite mailer here:
   mailx -s "Server $SERVENAME is down" -t "$NOTIFYEMAIL" < /dev/null
 fi
done