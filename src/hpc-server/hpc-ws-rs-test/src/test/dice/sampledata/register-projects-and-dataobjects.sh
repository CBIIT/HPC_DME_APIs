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

function permission_verify {

    RESPONSE_HEADER="permission-response-header.tmp"
    RESPONSE_MSG="permission-response-message.json.tmp"


    if [ -e  "$RESPONSE_HEADER" ]
    then
        rm $RESPONSE_HEADER
    fi

    if [ -e  "$RESPONSE_MSG" ]
    then
        rm $RESPONSE_MSG
    fi

    update_single_permission $1 $2 $3 
    #verify_registration $RESPONSE_HEADER

    if [[ $(cat $RESPONSE_MSG ) == *"true"* ]]; then
        echo "Successfully gave $2 $3 permission on $1"
    else  
        echo "ERROR in giving $2 $3 permission on $1"
        exit
    fi
}


source $HPC_DM_TEST/utils/functions


#Give dice_user_sys_admin OWN permission on the root folder
update_single_permission collection/$(get_basefolder) dice_user_sys_admin OWN


BASE_FOLDER=$HPC_DM_TEST/sampledata/

#Register all the collections

register_verify $BASE_FOLDER/project1_metadata.json "$(get_basefolder)/dice_project1" collection
permission_verify collection/$(get_basefolder)/dice_project1  dice_user_group_admin WRITE 
permission_verify collection/$(get_basefolder)/dice_project1  dice_user NONE

register_verify $BASE_FOLDER/project2_metadata.json "$(get_basefolder)/dice_project2" collection
permission_verify collection/$(get_basefolder)/dice_project2  dice_user_group_admin WRITE 
permission_verify collection/$(get_basefolder)/dice_project2  dice_user WRITE 

register_verify $BASE_FOLDER/project3_metadata.json "$(get_basefolder)/dice_project3" collection
permission_verify collection/$(get_basefolder)/dice_project3  dice_user_group_admin WRITE 
permission_verify collection/$(get_basefolder)/dice_project3  dice_user NONE

register_verify $BASE_FOLDER/project4_metadata.json "$(get_basefolder)/dice_project4" collection
update_single_permission collection/$(get_basefolder)/dice_project4  dice_user_group_admin WRITE 
update_single_permission collection/$(get_basefolder)/dice_project4  dice_user NONE

register_verify $BASE_FOLDER/project1_sub1_metadata.json "$(get_basefolder)/dice_project1/sub1" collection

register_verify $BASE_FOLDER/project2_sub2_metadata.json "$(get_basefolder)/dice_project2/sub2" collection

#Register all the dataObject 
#Create a dummy dataObject
OBJECT_FILE="dummy-file.txt"
echo "test-data" > $OBJECT_FILE


register_verify $BASE_FOLDER/project1_datafile1_metadata.json "$(get_basefolder)/dice_project1/dice_object_1" dataObject $OBJECT_FILE 

register_verify $BASE_FOLDER/project2_datafile2_metadata.json "$(get_basefolder)/dice_project2/dice_object_2" dataObject $OBJECT_FILE 

register_verify $BASE_FOLDER/project1_sub1_object1_metadata.json "$(get_basefolder)/dice_project1/sub1/object1" dataObject $OBJECT_FILE

register_verify $BASE_FOLDER/project1_sub1_object2_metadata.json "$(get_basefolder)/dice_project1/sub1/object2" dataObject $OBJECT_FILE

register_verify $BASE_FOLDER/project2_sub2_object1_metadata.json "$(get_basefolder)/dice_project2/sub2/object1" dataObject $OBJECT_FILE 

register_verify $BASE_FOLDER/project2_sub2_object2_metadata.json "$(get_basefolder)/dice_project2/sub2/object2" dataObject $OBJECT_FILE 

#Add more than 100 objects in project 4 to test the pagination
for i in `seq  1 101`
do 
    register_verify $BASE_FOLDER/project4_datafile.json "$(get_basefolder)/dice_project4/dice_object_$i" dataObject dummy-file.txt
done


#Update the views 
refresh_views 
