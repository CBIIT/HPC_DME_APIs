import logging
import time
import os
import commands

from metadata.sf_helper import SFHelper
from common.sf_utils import SFUtils

class SFAudit(object):


    def __init__(self, audit_path, extract_path, byte_count = 0, file_count = 0):
        self.audit_path = audit_path
        self.extract_path = extract_path
        self.byte_count = byte_count
        self.file_count = file_count
        self.includes_csv_path = audit_path + "/sf_included.csv"
        self.excludes_csv_path = audit_path + "/sf_excluded.csv"
        self.includes_path = audit_path + "/registered_files"
        self.excludes_path = audit_path + "/excluded_files"
        self.logging_path = audit_path + "/ccr-sf_transfer"
        self.prep_for_audit()


    def inc_bytes(self, filesize):
        self.byte_count = int(self.byte_count) + filesize


    def inc_file(self):
        self.file_count = int(self.file_count) + 1



    def prep_for_audit(self):

        #create the audit directory
        if not os.path.exists(self.audit_path):
            os.mkdir(self.audit_path)

        #Write out the header for the csv file containing list of included files
        includes_csv = open(self.includes_csv_path, "a")
        includes_csv.write(
            "Tarfile, Extracted File, ArchivePath in HPCDME, Flowcell_Id, PI_Name, Project_Id, Project_Name, Sample_Name, Run_Name, Sequencing_Platform, Filesize, Result\n")
        includes_csv.close()

        #Write out the header for the csv file containing list of excluded files
        excludes_csv = open(self.excludes_csv_path, "a")
        excludes_csv.write("Tarfile, Extracted File, Filesize, Reason\n")
        excludes_csv.close()

        #Configure the logging
        ts = time.gmtime()
        formatted_time = time.strftime("%Y-%m-%d_%H-%M-%S", ts)
        # 2018-05-14_07:56:07
        logging.basicConfig(filename=self.logging_path + formatted_time + '.log',
                        format='%(levelname)s: %(asctime)s %(message)s', level=logging.DEBUG)
        logging.info("Begin processing....")


    #Record the excluded file
    def record_exclusion(self, tarfile_name, file_name, full_path, reason):
        filesize = 0
        if os.path.exists(full_path):
            filesize = os.path.getsize(full_path)

        excludes = open(self.excludes_path, "a")
        excludes.writelines(tarfile_name + ": " + file_name + " - " + reason + '\n')
        excludes.close()

        excludes_csv = open(self.excludes_csv_path, "a")
        excludes_csv.write(tarfile_name + ", " + file_name + ", " + str(filesize) + ", " + reason + "\n")
        excludes_csv.close()

        logging.warning('Ignoring file ' + reason)


    #Record the command
    def audit_command(self, command):
        logging.info(command)
        includes = open(self.includes_path, "a")
        includes.write("\n" + command)
        includes.close()

    # Record the copy
    def audit_copy(self, tarfileName, filePath, destPath):

        fileSize = os.path.getsize(filePath)

        logging.info("\nFile size = {0}\n".format(fileSize))
        print "File size = " + str(fileSize)

        # Compute total number of files registered so far, and total bytes
        self.inc_file()
        self.inc_bytes(fileSize)
        logging.info("\nFiles registered = {0}, Bytes_stored = {1} \n".format(self.file_count, self.byte_count))

        includes_csv = open(self.includes_csv_path, "a")
        includes_csv.write(tarfileName + ", " + filePath + ", " + destPath + ", " + str(fileSize) + "\n")
        includes_csv.close()




    # Record the collection metadata update
    def audit_collection_update(self, tarfile_name, filepath, fullpath, archive_path, dryrun):

        archived = False
        result = 'Fail'

        if not dryrun:

            # Record the result
            response_header = "collection-registration-response-header.tmp"
            with open(response_header) as f:
                for line in f:
                    logging.info(line)
                    if ('200 OK' in line):
                        archive = True
                        result = 'Update'

                    elif ('201 Created' in line):
                        archived = True
                        result = 'New'

        else:
            filesize = 0
            archived = True
            result = 'Update'


        self.record_to_csv(tarfile_name, filepath, fullpath, archive_path, 0, result)




    def audit_summary(self):
        includes = open(self.includes_path, "a")
        includes.write(
            "Number of files uploaded = {0}, total bytes so far = {1}".format(self.file_count, self.byte_count))
        includes.close()

        logging.info("Number of files uploaded = {0}, total bytes so far = {1}".format(self.file_count, self.byte_count))



    # Record to csv file: tarfile name, file path, archive path
    def record_to_csv(self, tarfile_name, filepath, fullpath, archive_path, filesize, result, ext = None):

        flowcell_id = SFHelper.get_flowcell_id(tarfile_name)
        normalized_filepath = fullpath.split("uploads/")[-1]
        if filepath.endswith('html'):
            head, sep, tail = fullpath.partition('all/')
            path = head.split(flowcell_id + '/')[-1]
        else:
            path = SFUtils.get_meta_path(fullpath, False)

        includes_csv = open(self.includes_csv_path, "a")
        includes_csv.write(tarfile_name + ", " + normalized_filepath + ", " + archive_path +
            ", " + flowcell_id + ", " + SFHelper.get_pi_name(path) + ", " + SFHelper.get_project_id(path) +
            ", " + SFHelper.get_project_name(path, tarfile_name, ext) + ", " + SFHelper.get_sample_name(path) +
            ", " + SFHelper.get_run_name(tarfile_name) + ", " + SFHelper.get_sequencing_platform(tarfile_name) +
            "," + str(filesize) + "," + result + "\n")
        includes_csv.close()