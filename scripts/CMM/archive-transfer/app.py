import logging
import sys
import os
import json
import subprocess
import re

from metadata.sf_collection import SFCollection
from metadata.sf_object import SFObject



from common.sf_audit import SFAudit

############################################
# This program will parse through the PI folders
#listed in args[1]. For each PI folder, it will do the
#following:
#- create a PI_Lab_<PI folder> directory
#- create a Project_<PI_folder> directory
#- Walk the folder, checking to see if that directory path is as element of a value array
#in the hierarchy_dict dictionary. If so, it will upload that folder to HPCDME

############################################


def main(args):

    pi_mapping = {'0001': '0001', '0002': '0002'}
    hierarchy_dict  = {'sample-data': ['sample-data/Latitude_runs', 'sample-data/screening_images']}
    match_dict = {'Latitude_runs' : '\d{8}_\d{4}'}
    base_path = '/CMM_Archive'

    if len(sys.argv) < 4:
        print("\n Usage: python app.py project_list project_dir audit_dir dryrun initial_bytes initial_file_count")
        return

    # The file containing the project list - projectList
    project_list = args[1]

    # The path containing the projects - /data/CMM_CryoEM
    projects_path = args[2]

    # sub-directory to hold the log and audit files
    audit_dir = args[3]

    #If this is a dryrun or not
    dryrun = args[4].lower() == 'true'

    bytes_stored = 0
    files_registered = 0

    if (args[5] is not None):
        bytes_stored = args[5]
        if (args[6] is not None):
            files_registered = args[6]

    sf_audit = SFAudit(audit_dir, bytes_stored, files_registered)

    for project_dir in open(project_list).readlines():

        project_dir = project_dir.rstrip()

        #Create a PI_Lab collection with appropriate json
        pi_collection = register_collection(project_dir, 'PI_Lab', None, sf_audit, dryrun)

        #Create a Project collection with appropriate json
        project_collection = register_collection(project_dir, 'Project', pi_collection, sf_audit, dryrun)


        parent_collection = project_collection
        for dirName, subdirList, fileList in os.walk(project_dir):
            print dirName, subdirList, fileList

            if(project_dir == dirName):
                continue


            parent = os.path.abspath(os.path.join(dirName, os.pardir)).split(os.sep)[-1]
            if parent in hierarchy_dict.keys():
                if dirName not in hierarchy_dict.get(parent):
                    continue

            #Check if there is a format specified for this directory name
            #in the match dict
            if parent in match_dict.keys():
                #format is specified
                dirBaseName = os.path.basename(dirName)
                print'dirBaseName: ' + dirBaseName + ', pattern: ' + match_dict.get(parent)
                if not re.match(match_dict.get(parent), dirBaseName):
                    print 'No match found'
                    del subdirList[:]
                    continue

            #there is no format, or a match was found. So proceed
            #with uploading the objects
            #if os.listdir(dirName):


            #Print subdirectory
            print('scanned directory: %s' % dirName)
            #Print list of files in subdirectory
            for fname in fileList:
                #upload the individual files
                print('\t%s' % fname)
                full_path = projects_path + dirName + '/' + fname
                register_object(project_collection, project_dir, full_path, sf_audit, dryrun)




def register_collection(col_name, type, parent, sf_audit, dryrun):

    logging.info("Registering collection for " + col_name)
    json_path = sf_audit.audit_path + '/jsons'
    name = col_name.split('/')[-1]

    # create the audit directory if it does not exist
    if not os.path.exists(json_path):
        os.mkdir(json_path)

    #Build metadata for the collection
    collection = SFCollection(name, type, parent)
    collection_metadata = collection.get_metadata()

    #Create the metadata json file
    if type is not None:
        json_file_name = json_path + '/' + type + "_" + name + ".json"
    else:
        json_file_name = json_path + '/' + name + ".json"
    with open(json_file_name, "w") as fp:
        json.dump(collection_metadata, fp)

    #Prepare the command
    archive_path = collection.get_archive_path()
    command = "dm_register_collection " + json_file_name + " " + archive_path

    #Audit the command
    logging.info(command)
    print command

    #Run the command
    response_header = "collection-registration-response-header.tmp"
    if not dryrun:
        os.system("rm - f " + response_header + " 2>/dev/null")
        os.system(command)

        #Audit the result
        with open(response_header) as f:
            for line in f:
                logging.info(line)


    return collection




def register_object(project_collection, project_dir, full_path, sf_audit, dryrun):


    print register_object

    parent_path = full_path.split(project_dir + '/')[-1]
    parent_path = parent_path.rsplit('/', 1)[0]

    collection_names = parent_path.split('/')

    parent = project_collection

    #Create collection with each collection name
    for name in collection_names:
        parent = register_collection(name, None, parent, sf_audit, dryrun)

    #Build metadata for the object
    #file_path = parent_path + '/' + file_name
    object_to_register = SFObject(full_path)
    object_metadata = object_to_register.get_metadata()
    json_path = sf_audit.audit_path + '/jsons'

    # create the metadata json file
    file_name = full_path.rsplit('/', 1)[-1]
    json_file_name = json_path + '/' + file_name + ".json"
    with open( json_file_name, "w") as fp:
        json.dump(object_metadata, fp)

    #Prepare the command
    archive_path = parent.get_archive_path()
    archive_path = archive_path + '/' + file_name

    command = "dm_register_dataobject " + json_file_name + " " + archive_path + " " + full_path

    #Audit the command
    sf_audit.audit_command(command)
    print command

    #Run the command
    if not dryrun:
        response_header = "dataObject-registration-response-header.tmp"
        os.system("rm - f " + response_header + " 2>/dev/null")
        os.system(command)

    #Audit the result
    sf_audit.audit_upload(full_path, archive_path, dryrun)





main(sys.argv)
