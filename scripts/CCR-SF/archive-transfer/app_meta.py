import logging
import sys
import os
import json
import time
import re
import subprocess

from metadata.sf_object import SFObject
from metadata.sf_collection import SFCollection
from metadata.sf_helper import SFHelper
from common.sf_utils import SFUtils


def main(args):

    #Get the file containing the tarlist
    tarfile_list = args[1]
    tarfile_dir = args[2]

    for line_filepath in open(tarfile_list).readlines():

        tarfile_name = line_filepath.rstrip()

        tarfile_contents = SFUtils.get_tarball_contents(tarfile_name, tarfile_dir)
        if tarfile_contents is None:
            continue

        tarfile_path = tarfile_dir + '/' + tarfile_name.rstrip()

        # This is a valid tarball, so process
        logging.info("Processing file: " + tarfile_path)

        #loop through each line in the contents file of this tarball
        #We need to do an upload for each fatq.gz or BAM file
        for line in tarfile_contents.readlines():

            if(line.rstrip().endswith("/")):
                #This is a directory, nothing to do
                continue

            if line.rstrip().endswith('fastq.gz') or line.rstrip().endswith('fastq.gz.md5'):

                if SFUtils.path_contains_exclude_str(tarfile_name, path):
                    continue

                filepath = SFUtils.extract_file_to_archive(tarfile_name, tarfile_path, line.rstrip())
                if filepath is None:
                    continue

                # Extract the info for PI metadata

                path = SFUtils.get_meta_path(filepath)

                # Register PI collection
                register_collection(path, "PI_Lab", tarfile_name, False)

                #Register Flowcell collection with Project type parent
                register_collection(path, "Flowcell", tarfile_name, True)

                #create Object metadata with Sample type parent and register object
                register_object(path, "Sample", tarfile_name, True, filepath)

                #delete the extracted tar file
                os.system("rm -rf uploads/*")

            elif line.rstrip().endswith('laneBarcode.html') and '/all/' in line:

                #Remove the string after the first '/all' because metadata path if present will be before that
                head, sep, tail = line.partition('all/')

                #Remove everything upto the Flowcell_id, because metadata path if present will be after that
                flowcell_id = SFHelper.get_flowcell_id(tarfile_name)
                if flowcell_id in head:

                    path = head.split(flowcell_id + '/')[-1]

                    #Ensure that metadata path does not have the Sample sub-directory and that it is valid
                    if path.count('/') == 1 and '_' in path:

                        filepath = SFUtils.extract_file_to_archive(tarfile_name, tarfile_path, line)
                        if filepath is None:
                            SFUtils.record_exclusion(tarfile_name, line.rstrip(), 'could not extract file for archiving')
                            continue

                        #Register the html in flowcell collection

                        path = path + 'laneBarcode.html'
                        logging.info('metadata base: ' + path)

                        # Register PI collection
                        register_collection(path, "PI_Lab", tarfile_name, False)

                        # Register Flowcell collection with Project type parent
                        register_collection(path, "Flowcell", tarfile_name, True)

                        # create Object metadata with Flowcell type parent and register object
                        register_object(path, "Flowcell", tarfile_name, False, filepath)

                    else:
                        # ignore this html
                        SFUtils.record_exclusion(tarfile_name, line.rstrip(), 'html path not valid - may have other sub-directory')
                        continue

                else:
                    #ignore this html
                    SFUtils.record_exclusion(tarfile_name, line.rstrip(), 'html path not valid - could not extract flowcell_id')
                    continue

            else:
                #For now, we ignore files that are not fastq.gz or html
                SFUtils.record_exclusion(tarfile_name, line.rstrip(), 'Not fastq.gz or valid html file')

        logging.info('Done processing file: ' + tarfile_path)



def register_collection(filepath, type, tarfile_name, has_parent):

    logging.info("Registering " + type + " collection for " + filepath)

    #Build metadata for the collection
    collection = SFCollection(filepath, type, tarfile_name, has_parent)
    collection_metadata = collection.get_metadata()

    #Create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = type + "_" + file_name + ".json"
    with open('jsons_dryrun/' + json_file_name, "w") as fp:
        json.dump(collection_metadata, fp)

    #Register the collection
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type)

    command = "dm_register_collection jsons_dryrun/" + json_file_name + " " + archive_path
    logging.info(command)



def register_object(filepath, type, tarfile_name, has_parent, fullpath):

    global files_registered, bytes_stored
    #Build metadata for the object
    object_to_register = SFObject(filepath, tarfile_name, has_parent, type)
    object_metadata = object_to_register.get_metadata()

    # create the metadata json file
    file_name = 'DataObject_' + filepath.split("/")[-1]
    json_file_name = file_name + ".json"
    with open('jsons_dryrun/' + json_file_name, "w") as fp:
        json.dump(object_metadata, fp)

    #register the object
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath.rsplit("/", 1)[0], type)
    archive_path = archive_path + '/' + file_name

    command = "dm_register_dataobject jsons_dryrun/" + json_file_name + " " + archive_path + " " + fullpath

    logging.info(command)

    includes = open("registered_files_dryrun", "a")

    includes.write(command)

    #Compute total number of files registered so far, and total bytes
    files_registered = files_registered + 1
    bytes_stored = 0 #+= bytes_stored + filesize

    includes.write("Files registered = {0}, Bytes_stored = {1} \n".format(files_registered, bytes_stored))
    includes.close()

    SFUtils.record_to_csv(tarfile_name, filepath, fullpath, archive_path)



files_registered = 0
bytes_stored = 0L

includes_csv = open("sf_included.csv", "a")
includes_csv.write("Tarfile, Extracted File, ArchivePath in HPCDME, Flowcell_Id, PI_Name, Project_Id, Project_Name, Sample_Name, Run_Name, Sequencing_Platform\n")
includes_csv.close()

excludes_csv = open("sf_excluded.csv", "a")
excludes_csv.write("Tarfile, Extracted File, Reason\n")
excludes_csv.close()

ts = time.gmtime()
formatted_time = time.strftime("%Y-%m-%d_%H-%M-%S", ts)
# 2018-05-14_07:56:07
logging.basicConfig(filename='ccr-sf_transfer_dryrun' + formatted_time + '.log', level=logging.DEBUG)

main(sys.argv)

includes = open("registered_files_dryrun", "a")
includes.write("Number of files uploaded = {0}, total bytes so far = {1}".format(files_registered, bytes_stored))
includes.close()

logging.info("Number of files uploaded = {0}, total bytes so far = {1}".format(files_registered, bytes_stored))
