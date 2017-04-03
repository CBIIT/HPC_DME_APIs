#!/bin/bash

# list all files in a source main directory as well as sub- directories
# Ex: /test
SOURCEDIR=/home/paaleputr/test

# keep track list of the old files .
#LAST=/tmp/last.log
export LAST=""

if [ "$LAST" = "" ]; then

 # first time we create the log file
  touch /tmp/last_files.log
  export LAST=/tmp/last_files.log
else
  echo $LAST is not set
fi


# keep track the list of the current files .
CURRENT=/tmp/current.log

# list all files
find $SOURCEDIR -type f > $CURRENT

# list new file list
diff $LAST $CURRENT > /dev/null 2>&1

# If there is no difference exit
if [ $? -eq 0 ]
then
    echo "No changes"
else
     echo "List of new files"
   diff $LAST $CURRENT | grep '^>' |awk '{print $2}' > /tmp/new_file_list
   # diff last current | grep '^>' |awk '{print $2}'

    # Lastly, move CURRENT to LAST
    mv $CURRENT $LAST
 echo '{''"'newfilelist'"'':' '{' > /tmp/new_fil_list.json

        while read LINE; do
         echo $LINE
          echo '"filepath"'':' '"'$LINE'"' >> /tmp/new_fil_list.json
        done < /tmp/new_file_list
fi
echo '}''}' >>/tmp/new_fil_list.json
