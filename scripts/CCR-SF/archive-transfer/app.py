import logging
import sys
import os
import json
import time

from metadata.sf_object import SFObject
from metadata.sf_collection import SFCollection
from metadata.sf_helper import SFHelper


def main(args):

    #Get the file containing the tarlist
    tarfile_list = args[1]
    tarfile_dir = args[2]

    for line_filepath in open(tarfile_list).readlines():

        tarfile_name = line_filepath.rstrip()

        tarfile_contents = get_tarball_contents(tarfile_name, tarfile_dir)
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

            if line.rstrip().endswith('fastq.gz'):

                filepath = extract_file_to_archive(tarfile_name, tarfile_path, line)
                if filepath is None:
                    continue

                # Extract the info for PI metadata
                path = filepath.split("Unaligned/")[1]

                logging.info('metadata base: ' + path)

                # Register PI collection
                register_collection(path, "PI_Lab", tarfile_name, False)

                #Register Flowcell collection with Project type parent
                register_collection(path, "Flowcell", tarfile_name, True)

                #create Object metadata with Sample type parent and register object
                register_object(path, "Sample", tarfile_name, True, filepath)

                #delete the extracted tar file
                os.system("rm -rf ./Unaligned/*")

            elif line.rstrip().endswith('laneBarcode.html') and '/all/' in line:

                #Remove the string after the first '/all' because metadata path if present will be before that
                head, sep, tail = line.partition('all/')

                #Remove everything upto the Flowcell_id, because metadata path if present will be after that
                flowcell_id = SFHelper.get_flowcell_id(tarfile_name)
                if flowcell_id in head:

                    path = head.split(flowcell_id + '/')[-1]

                    #Ensure that metadata path does not have the Sample sub-directory and that it is valid
                    if path.count('/') == 1 and '_' in path:

                        filepath = extract_file_to_archive(tarfile_name, tarfile_path, line)
                        if filepath is None:
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
                        record_exclusion(tarfile_name + ':' + line + ': html path not valid, may have Sample or other sub-directory')
                        continue

                else:
                    #ignore this html
                    record_exclusion(tarfile_name + ':' + line + ': html path not valid, could not extract flowcell_id')
                    continue

            else:
                #For now, we ignore files that are not fastq.gz or html
                record_exclusion(tarfile_name + ':'  + line + ': Not tar.gz or html file')




def record_exclusion(str):
    excludes.writelines(str + '\n')
    logging.warning('Ignoring file ' + str)



def extract_file_to_archive(tarfile_name, tarfile_path, line):
    # Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
    #filepath = line[3:].rstrip()
    filepath = line.split("../")[-1]
    filepath = filepath.split(" ")[-1]


    if len(filepath.split("/")) < 3:
        # There is no subdirectory structure - something not right
        record_exclusion(tarfile_name + ':' + line)
        return

    logging.info("file to archive: " + filepath)

    # extract the fastq file from the archive
    #os.system("tar -xf " + tarfile_path + " -C ./uploads " + filepath)
    os.system("tar -xf " + tarfile_path + " " + filepath)
    #filepath = './uploads/' + filepath

    return filepath



def get_tarball_contents(tarfile_name, tarfile_dir):

    if not tarfile_name.rstrip().endswith('tar.gz'):

        # If this is not a .list or .md5 file also, then record exclusion. Else
        # just ignore, do not record because we may find the associated tar later
        if (not tarfile_name.rstrip().endswith('tar.gz.list') and
                not tarfile_name.rstrip().endswith('.md5')):
            excludes_str = ': Invalid file format - not tar.gz, tar.gz.list or tar.gz.md5 \n'
            excludes.write(tarfile_name + excludes_str)
            logging.info('Ignoring file ' + tarfile_name.rstrip() + excludes_str)
        return

    if '-' in tarfile_name:
        # this tarball contains '-', hence ignore for now because we wont be able to extract metadata correctly
        excludes_str = ': Invalid file format - contains - in filename, cannot parse for metadata \n'
        excludes.write(tarfile_name + excludes_str)
        logging.info('Ignoring file' + tarfile_name.rstrip() + excludes_str)
        return

    tarfile_path = tarfile_dir + '/' + tarfile_name.rstrip()
    try:
        tarfile_contents = open(tarfile_path + '.list')

    except IOError as e:
        # There is no contents file for this tarball, so
        # exclude the tarball
        excludes_str = ': No contents file located \n'
        excludes.write(tarfile_name + excludes_str)
        logging.warning("Ignoring file " + tarfile_name.rstrip() + excludes_str)
        return

    return tarfile_contents


def register_collection(filepath, type, tarfile_name, has_parent):

    #Build metadata for the collection
    collection = SFCollection(filepath, type, tarfile_name, has_parent)
    collection_metadata = collection.get_metadata()

    #Create the metadata json file
    file_name = filepath.split("/")[-1]
    json_file_name = type + "_" + file_name + ".json"
    with open('jsons/' + json_file_name, "w") as fp:
        json.dump(collection_metadata, fp)

    #Register the collection
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath, type)
    command = "dm_register_collection jsons/" + json_file_name + " " + archive_path
    logging.info(command)
    os.system(command)


def register_object(filepath, type, tarfile_name, has_parent, fullpath):

    #Build metadata for the object
    object_to_register = SFObject(filepath, tarfile_name, has_parent, type)
    object_metadata = object_to_register.get_metadata()

    # create the metadata json file
    file_name = 'DataObject_' + filepath.split("/")[-1]
    json_file_name = "DataObject_" + file_name + ".json"
    with open('jsons/' + json_file_name, "w") as fp:
        json.dump(object_metadata, fp)

    #register the object
    archive_path = SFCollection.get_archive_path(tarfile_name, filepath.rsplit("/", 1)[0], type)
    archive_path = archive_path + '/' + file_name

    response_message = "dataObject-registration-response-message.json.tmp"
    response_header = "dataObject-registration-response-header.tmp"

    command = "dm_register_dataobject jsons/" + json_file_name + " " + archive_path + " " + fullpath \
              + " -D " + response_header

    logging.info(command)
    includes.write(command)
    os.system(command)

    with open(response_header) as f:
        with open(includes, "a") as f1:
            for line in f:
                f1.write(line)

    os.system("rm - f " + response_message + " 2>/dev/null")
    os.system("rm - f " + response_header + " 2>/dev/null")






excludes = open("excluded_files", "a")
includes = open("registered_files", "a")
ts = time.gmtime()
formatted_time = time.strftime("%Y-%m-%d_%H-%M-%S", ts)
# 2018-05-14_07:56:07
logging.basicConfig(filename='ccr-sf_transfer' + formatted_time + '.log', level=logging.DEBUG)
main(sys.argv)

