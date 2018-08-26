import logging
import sys
import os
import json
import subprocess

from metadata.sf_object import SFObject
from metadata.sf_collection import SFCollection
from metadata.sf_helper import SFHelper
from common.sf_utils import SFUtils
from common.sf_audit import SFAudit

def main(args):

    if len(sys.argv) < 5:
        print("\n Usage: python app.py tarfile_list tarfile_dir extract_path audit_dir dryrun initial_bytes initial_file_count")
        return

    # The file containing the tarlist
    tarfile_list = args[1]

    # The directory containing the tarfiles
    tarfile_dir = args[2]

    # path containing the extracted file
    extract_path = args[3]

    # sub-directory to hold the log and audit files
    audit_dir = args[4]

    #If this is a dryrun or not
    dryrun = args[5].lower() == 'true'


    sf_audit = SFAudit(audit_dir, extract_path, 0, 0)
    sf_audit.prep_for_audit()

    for line_filepath in open(tarfile_list).readlines():

        tarfile_name = line_filepath.rstrip()

        tarfile_path = tarfile_dir + '/' + tarfile_name.rstrip()

        # This is a valid tarball, so process
        logging.info("Processing file: " + tarfile_path)

        if (tarfile_name.endswith("supplement.tar") or 'singlecell' in tarfile_name or '10x' in tarfile_name):

            # Register Flowcell collection
            register_collection(tarfile_path, "Flowcell", tarfile_name, False, tarfile_path, sf_audit, dryrun)

            logging.info('Done processing file: ' + tarfile_path)

            continue;

        tarfile_contents = SFUtils.get_tarball_contents(tarfile_name, tarfile_dir, sf_audit)
        if tarfile_contents is None:
            continue


        #loop through each line in the contents file of this tarball
        #We need to do an upload for each fatq.gz or BAM file
        for line in tarfile_contents.readlines():

            if(line.rstrip().endswith("/")):
                #This is a directory, nothing to do
                continue

            #if SFUtils.path_contains_exclude_str(tarfile_name, line.rstrip()):
            exclusion_list = ['10X', 'Phix', 'PhiX', 'demux', 'demultiplex']
            if any(ext in line.rstrip() for ext in exclusion_list):
                sf_audit.record_exclusion(tarfile_name, line.rstrip(),
                'Path contains substring from exclusion list')
                continue

            filepath = SFUtils.get_filepath_to_archive(line.rstrip(), extract_path)

            if filepath.endswith('fastq.gz') or filepath.endswith('fastq.gz.md5'):

                # Extract the info for PI metadata
                path = SFUtils.get_meta_path(filepath)

                #Register Flowcell collection with Project type parent
                register_collection(path, "Flowcell", tarfile_name, False, filepath, sf_audit, dryrun)


            else:
                #For now, we ignore files that are not fastq.gz or html
                sf_audit.record_exclusion(tarfile_name , line.rstrip(), 'Not fastq.gz or valid html file')

        logging.info('Done processing file: ' + tarfile_path)






def register_collection(filepath, type, tarfile_name, has_parent, fullpath, sf_audit, dryrun):

    logging.info("Registering " + type + " collection for " + filepath)
    json_path = sf_audit.audit_path + '/jsons'

    # create the audit directory if it does not exist
    if not os.path.exists(json_path):
        os.mkdir(json_path)

    #Build metadata for the collection
    collection = SFCollection(filepath, type, tarfile_name, has_parent)
    collection_metadata = collection.get_metadata()

    #Create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = json_path + '/' + type + "_" + file_name + ".json"
    with open(json_file_name, "w") as fp:
        json.dump(collection_metadata, fp)

    #Prepare the command
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type)
    command = "dm_register_collection " + json_file_name + " " + archive_path

    #Audit the command
    logging.info(command)

    #Run the command
    response_header = "collection-registration-response-header.tmp"
    if not dryrun:
        os.system("rm - f " + response_header + " 2>/dev/null")
        os.system(command)

    sf_audit.audit_collection_update(tarfile_name, filepath, fullpath, archive_path, dryrun)

main(sys.argv)
