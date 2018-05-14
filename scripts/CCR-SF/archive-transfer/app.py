import logging
import sys
import os
import json
import time

from metadata.sf_object import SFObject
from metadata.sf_collection import SFCollection
from metadata.sf_helper import SFHelper

def main(args):

    excludes = open("excluded_files", "a")
    #Get the file containing the tarlist
    tarfile_list = args[1]
    tarfile_dir = args[2]

    for line_filepath in open(tarfile_list).readlines():

        if not line_filepath.rstrip().endswith('tar.gz'):

            #If this is not a .list or .md5 file also, then record exclusion. Else
            #just ignore, do not record because we may find the associated tar later
            if (not line_filepath.rstrip().endswith('.list') and
               not line_filepath.rstrip().endswith('.md5')):
                excludes.write(line_filepath)
                logging.info("Ignoring file " + line_filepath.rstrip())
            continue

        #This is a tarball, so process
        logging.info("Processing file: " + line_filepath)
        #name of the tarfile
        #tarfile_path = line_filepath.rstrip()
        tarfile_name = line_filepath.rstrip()

        #Name of the tarfile
        #tarfile_name = tarfile_path.split("/")[-1]

        tarfile_path = tarfile_dir + "/" + tarfile_name
        try:
            tarfile_contents = open(tarfile_path + ".list")

        except IOError as e:
            #There is no contents file for this tarball, so
            #exclude the tarball found earlier
            excludes.write(line_filepath)
            logging.info("Ignoring file " + line_filepath.rstrip())
            continue


        #loop through each line in the contents file
        #We need to do an upload for each fatq.gz or BAM file
        for line in tarfile_contents.readlines():

            if(line.rstrip().endswith("/")):
                #This is a directory, nothing to do
                continue

            if line.rstrip().endswith('fastq.gz'):

                #extract the fastq file from the list

                #Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
                filepath = line[3:].rstrip()
                logging.info("file to archive: " + filepath)

                # extract the fastq file from the archive
                os.system("tar -xf " + tarfile_path + " " + filepath)

                #Create PI metadata
                path = filepath.split("Unaligned/")[1]
                #path = filepath.split("/")[1]
                logging.info("metadata base: " + path)

                if len(path.split("/")) == 1:
                    continue

                # Register PI collection
                register_collection(path, "PI_Lab", tarfile_name, False)

                #Register Flowcell collection with Project type parent
                register_collection(path, "Flowcell", tarfile_name, True)

                #create Object metadata with Sample type parent and register object
                register_object(path, "Sample", tarfile_name, True, filepath)

                #delete the extracted tar file
                os.system("rm " + filepath)

            else:
                excludes.write(tarfile_name + ": " + line)
                logging.info("Ignoring file " + line.rstrip())


def register_collection(filepath, type, tarfile_name, has_parent):

    #Build metadata for the collection
    collection = SFCollection(filepath, type, tarfile_name, has_parent)
    collection_metadata = collection.get_metadata()

    #Create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = type + "_" + file_name + ".json"
    with open(json_file_name, "w") as fp:
        json.dump(collection_metadata, fp)

    #Register the collection
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type)
    os.system("dm_register_collection " + json_file_name + " " + archive_path)


def register_object(filepath, type, tarfile_name, has_parent, fullpath):

    #Build metadata for the object
    object_to_register = SFObject(filepath, tarfile_name, has_parent)
    object_metadata = object_to_register.get_metadata()

    # create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = "Object_" + file_name + ".json"
    with open(json_file_name, "w") as fp:
        json.dump(object_metadata, fp)

    #register the object
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type)
    os.system("dm_register_dataobject " + json_file_name + " " + archive_path + " " + fullpath)


ts = time.gmtime()
formatted_time = time.strftime("%Y-%m-%d_%H-%M-%S", ts)
# 2018-05-14_07:56:07
logging.basicConfig(filename='ccr-sf_transfer' + formatted_time + '.log', level=logging.DEBUG)
main(sys.argv)

