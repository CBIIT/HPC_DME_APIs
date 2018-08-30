import logging
import sys
import os
import json
import subprocess
import re

from metadata.sf_collection import SFCollection
from metadata.sf_object import SFObject
from metadata.csv_reader import CSVMetadataReader
from metadata.sf_helper import SFHelper



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

    #pi_mapping = {'0001': '0001', '0002': '0002', '0003': 'Yawen_Bai', '0004': '0004', '0005': '0005',
     #             '0006': '0006', '0007': 'Kylie_Walters', '0008': '0008', '0009': '0009', '0010': '0010',
      #            '0011': '0011', '0012': '0012', '0013': '0013', '0014': '0014', '0015': '0015',
       #           '0016': '0016'}

    #hierarchy_dict  = {'0001': ['0001/Latitude_runs', '0001/screening_images']}
    hierarchy_list = ['Latitude_runs', 'screening_images']
    match_dict = {'Latitude_runs' : '\d{8}_\d{4}'}
    base_path = '/CMM_Archive'

    if len(sys.argv) < 4:
        print("\n Usage: python app.py project_list project_dir audit_dir dryrun metadata_file initial_bytes initial_file_count")
        return

    # The file containing the project list - projectList
    project_list = args[1]

    # The path containing the projects - /data/CMM_CryoEM
    projects_path = args[2]

    # sub-directory to hold the log and audit files
    audit_dir = args[3]

    #If this is a dryrun or not
    dryrun = args[4].lower() == 'true'

    #Metadata input file
    input_filepath = args[5]

    bytes_stored = 0
    files_registered = 0

    if (args[6] is not None):
        bytes_stored = args[6]
        if (args[7] is not None):
            files_registered = args[7]

    sf_audit = SFAudit(audit_dir, bytes_stored, files_registered)

    for line in open(project_list).readlines():

        project_dir = line.split(':')[0]
        name = line.split(':')[-1].rstrip()

        metadata_reader = CSVMetadataReader(input_filepath)
        project_metadata = metadata_reader.find_metadata_row('project_dir', project_dir.split('/')[-1].strip('0'))

        #Create a PI_Lab collection with appropriate json
        pi_collection = register_collection(name, 'PI_Lab', None, sf_audit, dryrun, project_metadata)

        #Create a Project collection with appropriate json
        project_collection = register_collection(name, 'Project', pi_collection, sf_audit, dryrun, project_metadata)


        parent_collection = project_collection
        for dirName, subdirList, fileList in os.walk(projects_path + project_dir):
            #print dirName, subdirList, fileList
            logging.info(str(dirName) + ', ' + str(subdirList) + ', ' + str(fileList))

            #We dont need to create the project collection, it is already created above
            if(project_dir == dirName):
                continue


            parent = os.path.abspath(os.path.join(dirName, os.pardir)).split(os.sep)[-1]
            #Either this dirName or it's ancestor should be in dict_list which is the
            #approved list of directories or their children
            match_found = False;
            for value in hierarchy_list:
                if value in dirName:
                    match_found = True
                    break

            if not match_found:
                continue


            #Check if there is a format specified for this directory name
            #in the match dict
            if parent in match_dict.keys():
                #format is specified
                dirBaseName = os.path.basename(dirName)
                #print'dirBaseName: ' + dirBaseName + ', pattern: ' + match_dict.get(parent)
                logging.info('dirBaseName: ' + dirBaseName + ', pattern: ' + match_dict.get(parent))
                if not re.match(match_dict.get(parent), dirBaseName):
                    print 'No match found'
                    del subdirList[:]
                    continue


            #Print subdirectory
            #print('scanned directory: %s' % dirName)
            logging.info('scanned directory: %s', dirName)
            #Register list of files in subdirectory
            for fname in fileList:
                #upload the individual files
                #print('File to register \t%s' % fname)
                logging.info('File to register: %s', fname)
                full_path = dirName + '/' + fname
                register_object(project_collection, project_dir, full_path, sf_audit, dryrun, project_metadata)




def register_collection(name, type, parent, sf_audit, dryrun, proj_metadata):

    logging.info("Registering collection for " + name)

    #name = col_name.split('/')[-1]

    #Build metadata for the collection
    collection = SFCollection(name, type, parent)
    collection_metadata = collection.build_metadata(proj_metadata)

    #Create the metadata json file
    json_path = sf_audit.audit_path + '/jsons'
    json_file_name = SFHelper.create_json_file(collection_metadata, name, json_path)

    #Prepare the command
    archive_path = collection.get_archive_path()
    command = "dm_register_collection " + json_file_name + " " + archive_path

    #Audit the command
    logging.info(command)
    #print command


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




def register_object(parent, project_dir, full_path, sf_audit, dryrun, proj_metadata):


    parent_path = full_path.split(project_dir + '/')[-1]
    parent_path = parent_path.rsplit('/', 1)[0]

    collection_names = parent_path.split('/')
    #Create collection with each collection name
    for name in collection_names:
        parent = register_collection(name, None, parent, sf_audit, dryrun, proj_metadata)

    #Build metadata for the object
    object_to_register = SFObject(full_path, parent)
    object_metadata = object_to_register.build_metadata()

    # create the metadata json file
    json_path = sf_audit.audit_path + '/jsons'
    json_file_name = SFHelper.create_json_file(object_metadata, full_path, json_path)

    #Prepare the command
    archive_path = object_to_register.get_archive_path()
    command = "dm_register_dataobject " + json_file_name + " " + archive_path + " " + full_path

    #Audit the command
    sf_audit.audit_command(command)
    #print command


    #Run the command
    if not dryrun:
        response_header = "dataObject-registration-response-header.tmp"
        os.system("rm - f " + response_header + " 2>/dev/null")
        os.system(command)

    #Audit the result
    sf_audit.audit_upload(full_path, archive_path, dryrun)

    return object_to_register





main(sys.argv)
