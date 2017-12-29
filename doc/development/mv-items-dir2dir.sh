#!/bin/sh
#
# Moves all items in a given iRODS directory to another specified iRODS directory,
# rendering source directory empty.  Then deletes empty source directory.
#
# Expects 2 arguments: 
#   First is iRODS directory having items to move, as fully qualified iRODS path
#   Second is iRODS directory to receive moved items, as fully qualified iRODS path
#
# Assumptions: 
#   1. OS user who runs this script has iRODS access (can execute iCommands).
#

if [ "$1" != "" -a "$2" != "" ]
then
   SRC_COLL="$1"
   DST_COLL="$2"
   echo "Source Collection: $SRC_COLL"
   echo "Destination Collection: $DST_COLL"
else
  echo "Arguments required: <source Collection> <destination Collection>"
  exit 1
fi

TIMESTAMP=`date +%Y-%m-%d-%H%M%S`
TMP_FILE_ESCPD_SC=/tmp/escpd-sc-$TIMESTAMP.tmp
echo "$SRC_COLL" | sed 's/\//\\\//g' > $TMP_FILE_ESCPD_SC
SRC_COLL_ESCPD=`cat $TMP_FILE_ESCPD_SC`
TMP_FILE_SRC_ITEMS=/tmp/items-2-mv-$TIMESTAMP.tmp
ils $SRC_COLL | sed -E "s/^\s+C-\s+//g; s/^\s+/$SRC_COLL_ESCPD\//g ; 1d" > $TMP_FILE_SRC_ITEMS

# Loop thru $TMP_FILE_SRC_ITEMS
#  For each print a imv command of the item to $DST_COLL
while read in; do imv  $in  $DST_COLL; done < $TMP_FILE_SRC_ITEMS

echo "All items in Source Collection have been successfully moved to Destination Collection."
echo "Now it is time to try deleting Source Collection."

COUNT_ITEMS=`ils $SRC_COLL | sed -n '$='`
COUNT_ITEMS=$((COUNT_ITEMS - 1))
if [ $COUNT_ITEMS -eq 0 ] 
then
  irm -r $SRC_COLL
  echo "Source Collection successfully deleted."
else
  echo "Source Collection could not be deleted because it is not empty."
fi


# Clean up
rm -f $TMP_FILE_ESCPD_SC
rm -f $TMP_FILE_SRC_ITEMS
