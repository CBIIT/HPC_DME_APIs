#!/bin/bash
#A script to populate the database with the collections and dataObjects before running the query tests

#Register and verify registration
#Parameters:
#       <attributes.json>
#       <path>
#       <type> 

function register_verify {



    RESPONSE_HEADER="$3-registration-response-header.tmp"

    if [ -e  "$RESPONSE_HEADER" ]
    then
        rm $RESPONSE_HEADER
    fi

    curl_register "$1" "$2" "$3" "$4"
    verify_registration $RESPONSE_HEADER
    echo "Successfully registered $2"
    


}
source $HPC_DM_TEST/utils/functions


#Give dice_user_sys_admin OWN permission on the root folder
#update_single_permission $BASE_FOLDER dice_user_sys_admin OWN


BASE_FOLDER=$HPC_DM_TEST/sampledata/

#Register all the collections
register_verify $BASE_FOLDER/project1_metadata.json "$(get_basefolder)/dice_project1" collection

register_verify $BASE_FOLDER/project2_metadata.json "$(get_basefolder)/dice_project2" collection

register_verify $BASE_FOLDER/project3_metadata.json "$(get_basefolder)/dice_project3" collection

register_verify $BASE_FOLDER/project1_sub1_metadata.json "$(get_basefolder)/dice_project1/sub1" collection

register_verify $BASE_FOLDER/project2_sub2_metadata.json "$(get_basefolder)/dice_project2/sub2" collection

#Register all the dataObject 
#Create a dummy dataObject
OBJECT_FILE="dummy-file.txt"
echo "test-data" > $OBJECT_FILE


register_verify $BASE_FOLDER/project1_datafile1_metadata.json "$(get_basefolder)/dice_project1/dice_object_1" dataObject dummy-file.txt

register_verify $BASE_FOLDER/project2_datafile2_metadata.json "$(get_basefolder)/dice_project2/dice_object_2" dataObject dummy-file.txt

register_verify $BASE_FOLDER/project1_sub1_object1_metadata.json "$(get_basefolder)/dice_project1/sub1/object1" dataObject dummy-file.txt

register_verify $BASE_FOLDER/project1_sub1_object2_metadata.json "$(get_basefolder)/dice_project1/sub1/object2" dataObject dummy-file.txt

register_verify $BASE_FOLDER/project2_sub2_object1_metadata.json "$(get_basefolder)/dice_project2/sub2/object1" dataObject dummy-file.txt

register_verify $BASE_FOLDER/project2_sub2_object2_metadata.json "$(get_basefolder)/dice_project2/sub2/object2" dataObject dummy-file.txt


#Update the views 
refresh_views 
