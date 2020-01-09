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

#Usage:
#python app.py tarfiles /is2/projects/CCR-SF/archive/illumina/2015_new /mnt/IRODsScratch/bulk-uploads/CCR-SF/2015_new/uploads/ 2015_new_dryrun_with_size True True

def main(args):

    if len(sys.argv) < 6:
        print("\n Usage: python app.py tarfile_list tarfile_dir extract_path audit_dir dryrun include_file_size")
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

    #If filesize has to be recorded or not - applicable only for dry run
    if dryrun:
        if len(sys.argv) > 6:
            record_file_size = args[6]
        else:
            record_file_size = False
    else:
        record_file_size = True

    bytes_stored = 0
    files_registered = 0

    #if (args[6] is not None):
    #    bytes_stored = args[6]
    #    if (args[7] is not None):
    #        files_registered = args[7]

    sf_audit = SFAudit(audit_dir, extract_path, bytes_stored, files_registered)
    sf_audit.prep_for_audit()

    for line_filepath in open(tarfile_list).readlines():

        tarfile_name = line_filepath.rstrip()

        tarfile_path = tarfile_dir + '/' + tarfile_name.rstrip()

        # This is a valid tarball, so process
        logging.info("Processing file: " + tarfile_path)

        if (tarfile_name.endswith("supplement.tar") or 'singlecell' in tarfile_name or '10x' in tarfile_name):

            # Register PI collection
            register_collection(tarfile_path, "PI_Lab", tarfile_name, False, sf_audit, dryrun)

            # Register Flowcell collection with Project type parent
            register_collection(tarfile_path, "Flowcell", tarfile_name, True, sf_audit, dryrun)

            # create Object metadata with Flowcell type parent and register object
            register_object(tarfile_path, "Flowcell", tarfile_name, False, tarfile_path, sf_audit, dryrun)

            logging.info('Done processing file: ' + tarfile_path)

            continue;

        #Get or create list file (if not present)
        tarfile_contents = SFUtils.get_tarball_contents(tarfile_name, tarfile_dir, sf_audit)
        if tarfile_contents is None:
            continue

        # Extract all files and store in extract_path directory
        if record_file_size:
            if not (SFUtils.extract_files_from_tar(tarfile_path, extract_path)):
                # Something wrong with this file path, go to
                # next one and check logs later
                continue;

        #loop through each line in the contents file of this tarball
        #We need to do an upload for each fastq.gz or BAM file
        for line in tarfile_contents.readlines():
            logging.info('processing line in tarfile: ' + line)

            if(line.rstrip().endswith("/")):
                #This is a directory, nothing to do
                continue

            #Get full path of the extracted file
            filepath = SFUtils.get_filepath_to_archive(line.rstrip(), extract_path)
            logging.info('filepath to archive: ' + filepath)

            #if SFUtils.path_contains_exclude_str(tarfile_name, line.rstrip()):
            exclusion_list = ['10X', 'demux', 'demultiplex']
            if any(ext in line.rstrip() for ext in exclusion_list):
                sf_audit.record_exclusion(tarfile_name, line.rstrip(), filepath,
                'Path contains substring from exclusion list')
                continue

            if filepath.endswith('fastq') or filepath.endswith('fastq.gz') or filepath.endswith('fastq.gz.md5'):

                # Extract the info for PI metadata
                path = SFUtils.get_meta_path(filepath)
                ext = SFUtils.get_unaligned_ext(filepath)

                # Register PI collection
                register_collection(path, "PI_Lab", tarfile_name, False, sf_audit, dryrun)

                #Register Flowcell collection with Project type parent
                register_collection(path, "Flowcell", tarfile_name, True, sf_audit, dryrun, ext)

                #create Object metadata with Sample type parent and register object
                register_object(path, "Sample", tarfile_name, True, filepath, sf_audit, dryrun, ext)

            elif line.rstrip().endswith('laneBarcode.html') and '/all/' in line and not 'Control_Sample' in line:

                #Remove the string after the first '/all' because metadata path if present will be before that
                head, sep, tail = line.partition('all/')

                #Remove everything upto the Flowcell_id, because metadata path if present will be after that
                flowcell_id = SFHelper.get_flowcell_id(tarfile_name)
                if flowcell_id in head:

                    path = head.split(flowcell_id + '/')[-1]
                    ext = SFUtils.get_unaligned_ext(filepath)

                    #Ensure that metadata path does not have the Sample sub-directory and that it is valid
                    if path.count('/') == 1 and '_' in path:

                        #Register the html in flowcell collection

                        path = path + 'laneBarcode.html'
                        logging.info('metadata base: ' + path)

                        # Register PI collection
                        register_collection(path, "PI_Lab", tarfile_name, False, sf_audit, dryrun)

                        # Register Flowcell collection with Project type parent
                        register_collection(path, "Flowcell", tarfile_name, True, sf_audit, dryrun, ext)

                        # create Object metadata with Flowcell type parent and register object
                        register_object(path, "Flowcell", tarfile_name, False, filepath, sf_audit, dryrun, ext)

                    else:
                        # ignore this html
                        sf_audit.record_exclusion(tarfile_name, line.rstrip(), filepath, 'html path not valid - may have other sub-directory')
                        continue

                else:
                    #ignore this html
                    sf_audit.record_exclusion(tarfile_name, line.rstrip(), filepath, 'html path not valid - could not extract flowcell_id')
                    continue

            else:
                #For now, we ignore files that are not fastq.gz or html
                sf_audit.record_exclusion(tarfile_name , line.rstrip(), filepath, 'Not fastq.gz or valid html file')

        logging.info('Done processing file: ' + tarfile_path)

        # delete the extracted file
        if record_file_size:
            os.system("rm -rf " + extract_path + "*")

    sf_audit.audit_summary()



def register_collection(filepath, type, tarfile_name, has_parent, sf_audit, dryrun, ext = None):

    logging.info("Registering " + type + " collection for " + filepath)
    json_path = sf_audit.audit_path + '/jsons'

    # create the audit directory if it does not exist
    if not os.path.exists(json_path):
        os.mkdir(json_path)

    #Build metadata for the collection
    collection = SFCollection(filepath, type, tarfile_name, has_parent, ext)
    collection_metadata = collection.get_metadata()

    #Create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = json_path + '/' + type + "_" + file_name + ".json"
    with open(json_file_name, "w") as fp:
        json.dump(collection_metadata, fp)

    #Prepare the command
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type, ext)
    command = "dm_register_collection " + json_file_name + " " + archive_path

    #Audit the command
    logging.info(command)

    #Run the command
    response_header = "collection-registration-response-header.tmp"
    if not dryrun:
        os.system("rm - f " + response_header + " 2>/dev/null")
        os.system(command)

        #Audit the result
        with open(response_header) as f:
            for line in f:
                logging.info(line)



def register_object(filepath, type, tarfile_name, has_parent, fullpath, sf_audit, dryrun, ext = None):

    global files_registered, bytes_stored
    #Build metadata for the object
    object_to_register = SFObject(filepath, tarfile_name, ext, has_parent, type)
    object_metadata = object_to_register.get_metadata()
    json_path = sf_audit.audit_path + '/jsons'

    # create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = json_path + '/' + file_name + ".json"
    with open( json_file_name, "w") as fp:
        json.dump(object_metadata, fp)

    #Prepare the command
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type, ext)
    archive_path = archive_path + '/' + file_name
    command = "dm_register_dataobject_presigned " + json_file_name + " " + archive_path + " " + fullpath

    #Audit the command
    sf_audit.audit_command(command)

    #Run the command
    if not dryrun:
        response_header = "presignedURL-registration-response-header.tmp"
        os.system("rm - f " + response_header + " 2>/dev/null")
        os.system(command)

    #Audit the result
    sf_audit.audit_upload(tarfile_name, filepath, fullpath, archive_path, dryrun, ext)



main(sys.argv)
