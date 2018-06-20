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


def main(args):

    #Get the file containing the tarlist
    tarfile_list = args[1]
    tarfile_dir = args[2]

    for line_filepath in open(tarfile_list).readlines():

        tarfile_name = line_filepath.rstrip()

        tarfile_contents = get_tarball_contents(tarfile_name, tarfile_dir)
        if tarfile_contents is None:
            record_exclusion(
                tarfile_name + ': Could not extract contents')
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

                if('Undetermined' in line):
                    record_exclusion(
                        tarfile_name + ':' + line + ': Tar contains Undetermined files')
                    continue

                filepath = extract_file_to_archive(tarfile_name, tarfile_path, line.rstrip())
                if filepath is None:
                    continue

                # Extract the info for PI metadata

                path = re.sub(r'.*Unaligned[^/]*/', '', filepath)

                #strip 'Project_' if it exists
                path = path.replace("Project_", "")

                logging.info('metadata base: ' + path)

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

                        filepath = extract_file_to_archive(tarfile_name, tarfile_path, line)
                        if filepath is None:
                            record_exclusion(tarfile_name + ':' + line + ': could not extract file for archiving')
                            continue

                        #Register the html in flowcell collection

                        path = path + 'laneBarcode.html'
                        logging.info('metadata base: ' + path)

                        #Ensure tha the path has extractable PI name
                        #if(len(path.split('_')) < 3 or path.split('_')[0].isdigit() or path.split('_')[1].isdigit()):
                            #record_exclusion(tarfile_name + ':' + line + ': PI name not available')
                            #continue

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
                record_exclusion(tarfile_name + ':'  + line + ': Not fastq.gz or valid html file')

        logging.info('Done processing file: ' + tarfile_path)

def record_exclusion(str):
    excludes.writelines(str + '\n')
    excludes.flush()
    logging.warning('Ignoring file ' + str)



def extract_file_to_archive(tarfile_name, tarfile_path, line):
    # Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
    filepath = line.rstrip().split("../")[-1]
    filepath = filepath.split(" ")[-1]
    filepath = filepath.lstrip('/')


    if len(filepath.split("/")) < 3:
        # There is no subdirectory structure - something not right
        record_exclusion(tarfile_name + ':' + line)
        return

    # extract the fastq file from the archive
    command = "tar -xf " + tarfile_path + " -C uploads/ " + filepath
    logging.info(command)
    #os.system(command)
    filepath = 'uploads/' + filepath

    logging.info("file to archive: " + filepath)

    return filepath



def get_tarball_contents(tarfile_name, tarfile_dir):

    logging.info("Getting contents for: " + tarfile_name)

    if not tarfile_name.rstrip().endswith('tar.gz') and not tarfile_name.rstrip().endswith('tar'):

        # If this is not a .list, _archive.list, or .md5 file also, then record exclusion. Else
        # just ignore, do not record because we may find the associated tar later
        if (not tarfile_name.rstrip().endswith('tar.gz.list') and
                not tarfile_name.rstrip().endswith('_archive.list') and
                not tarfile_name.rstrip().endswith('list.txt') and
                not tarfile_name.rstrip().endswith('.md5')):
            excludes_str = ': Invalid file format - not tar.gz, _archive.list, tar.gz.list or tar.gz.md5 \n'
            record_exclusion(tarfile_name + excludes_str)
        return

    if '-' in tarfile_name:
        # this tarball contains '-', hence ignore for now because we wont be able to extract metadata correctly
        excludes_str = ': Invalid file format - contains - in filename, cannot parse for metadata \n'
        record_exclusion(tarfile_name + excludes_str)
        return

    tarfile_path = tarfile_dir + '/' + tarfile_name.rstrip()

    contentFiles = [tarfile_path + '.list', tarfile_path.split('.tar')[0] + '_archive.list',
                    tarfile_path.split('.gz')[0] + '.list.txt', tarfile_path.split('.gz')[0] + '_list.txt']

    tarfile_contents = None

    for filename in contentFiles:
        if os.path.exists(filename):
            tarfile_contents = open(filename)
            break

    if tarfile_contents is None:
        command = "tar tvf " + tarfile_path + " > " + tarfile_name + ".list"
        # os.system(command)
        subprocess.call(command, shell=True)
        logging.info("Created contents file: " + command)
        tarfile_contents = open(tarfile_name + '.list')

    #try:
    #    tarfile_contents = open(tarfile_path + '.list')

    #except IOError as e:
        #tar.gz.list is not present, so try _archive.list
    #    try:
    #        tarfile_contents = open(tarfile_path.split(".tar.gz")[0] + "_archive.list")
    #    except IOError as e:
    #        try:
    #            tarfile_contents = open(tarfile_path.split(".gz")[0] + '.list.txt')
    #        except IOError as e:

                # There is no contents file for this tarball, so create one
    #            command = "tar tvf " + tarfile_path + " > " + tarfile_name + ".list"
                #os.system(command)
    #            subprocess.call(command, shell=True)
    #            logging.info("Created contents file: " + command)
    #            tarfile_contents = open(tarfile_name + '.list')

    return tarfile_contents


def register_collection(filepath, type, tarfile_name, has_parent):

    logging.info("Registering " + type + " collection for " + filepath)

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




def register_object(filepath, type, tarfile_name, has_parent, fullpath):

    #Build metadata for the object
    global files_registered, bytes_stored
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

    command = "dm_register_dataobject jsons/" + json_file_name + " " + archive_path + " " + fullpath

    logging.info(command)
    includes.write(command)

    #Get size of file in bytes
    #filesize = os.path.getsize(fullpath)

    #Record the result
    #includes.write("\nFile size = {0}\n".format(filesize))

    #Compute total number of files registered so far, and total bytes
    files_registered = files_registered + 1
    bytes_stored = 0 #+= bytes_stored + filesize
    includes.write("Files registered = {0}, Bytes_stored = {1} \n".format(files_registered, bytes_stored))
    includes.flush()




files_registered = 0
bytes_stored = 0L
excludes = open("excluded_files_dryrun", "a")
includes = open("registered_files_dryrun", "a")
ts = time.gmtime()
formatted_time = time.strftime("%Y-%m-%d_%H-%M-%S", ts)
# 2018-05-14_07:56:07
logging.basicConfig(filename='ccr-sf_transfer_dryrun' + formatted_time + '.log', level=logging.DEBUG)
main(sys.argv)
excludes.close()
includes.write("Number of files uploaded = {0}, total bytes so far = {1}".format(files_registered, bytes_stored))
includes.close()
logging.info("Number of files uploaded = {0}, total bytes so far = {1}".format(files_registered, bytes_stored))
