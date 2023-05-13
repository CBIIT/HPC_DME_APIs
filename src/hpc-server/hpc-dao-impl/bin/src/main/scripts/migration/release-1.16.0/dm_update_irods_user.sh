#! /bin/bash

read -s -p "Enter key: " key
if [ -z "$key" ]
then
   echo "Key cannot be empty!"
   exit 1
fi

excluded_users=(rods dice_user dice_user_group_admin dice_user_sys_admin)
iadmin lu | while read user; do
    user_name=$(echo "$user" | cut -d'#' -f 1)
    #Skip the dice test created accounts
    if [[ $user_name == testid-* ]]
    then
      continue
    fi

    #Skip the excluded users
    (for e in "${excluded_users[@]}"; do [[ "$e" == "$user_name" ]] && exit 0; done; exit 1)
    if [ $? == 1 ]
    then
        echo "$user_name"
        new_pass=$(echo -n "$key$user_name"| openssl dgst -md5 -binary|base64)

        #Remove the LDAP temporary password
        iadmin rpp $user_name

        #Update the standard auth password for user
        iadmin moduser $user_name password $new_pass
    fi
done

