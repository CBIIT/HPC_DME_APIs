import logging
import sys
import os
import json

from metadata.sf_object import SFObject
from metadata.sf_collection import SFCollection
from metadata.sf_helper import SFHelper

def main(args):
    #Get the main gz file name 
    tarfile_name = args[1]
    
    #get corresponding the content list file
    try:
        contentList = open(tarfile_name + ".list")

    except IOError:

        print "No content list file found for " + tarfile_name
        return

    #loop through each line in the content list file
    #We need to do an upload for each fatq.gz or BAM file
    for line in open(tarfile_name + ".list").readlines():
        
        if line.rstrip().endswith('fastq.gz'):
            print line

            #extract the fastq file from the list

            #Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
            filepath = line[3:].rstrip()
            print filepath

            # extract the fastq file from the archive
            os.system("tar -xf " + tarfile_name + " " + filepath)

            #Create PI metadata
            path = filepath.split("Unaligned/")[1]
            #path = filepath.split("/")[1]
            print path

            # Register PI collection
            register_collection(path, "PI_Lab", tarfile_name, False)

            #Register Flowcell collection with Project type parent
            register_collection(path, "Flowcell", tarfile_name, True)

            #create Object metadata with Sample type parent and register object
            register_object(path, "Sample", tarfile_name, True, filepath)

            #delete the extracted tar file
            os.system("rm " + filepath)





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


main(sys.argv)

