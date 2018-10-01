import logging
import time
import os
import commands


class SFAudit(object):


    def __init__(self, audit_path, byte_count = 0, file_count = 0):
        self.audit_path = audit_path
        self.byte_count = byte_count
        self.file_count = file_count
        self.includes_csv_path = audit_path + "/sf_included.csv"
        self.includes_path = audit_path + "/registered_files"
        self.logging_path = audit_path + "/cmm_transfer"
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
            "FilePath, ArchivePath in HPCDME, Filesize\n")
        includes_csv.close()

        #Configure the logging
        ts = time.gmtime()
        formatted_time = time.strftime("%Y-%m-%d_%H-%M-%S", ts)
        # 2018-05-14_07:56:07
        logging.basicConfig(filename=self.logging_path + formatted_time + '.log',
                        format='%(levelname)s: %(asctime)s %(message)s', level=logging.DEBUG)
        logging.info("Begin processing....")



    #Record the command
    def audit_command(self, command):
        logging.info(command)
        includes = open(self.includes_path, "a")
        includes.write("\n{0}".format(command))
        includes.close()



    #Record the upload
    def audit_upload(self, filepath, archive_path, dryrun):

        archived = False
        includes = open(self.includes_path, "a")

        filesize = os.path.getsize(filepath)
        logging.info("\nFile size = {0}\n".format(filesize))

        if not dryrun:
            # Record the result
            response_header = "dataObject-registration-response-header.tmp"
            if os.path.isfile(response_header):
                with open(response_header) as f:
                    for line in f:
                        logging.info(line)
                        if ('200 OK' in line or '201 Created' in line):
                            archived = True

        else:
            archived = True
            filesize = 0

        # Compute total number of files registered so far, and total bytes
        if archived:
            self.inc_file()
            self.inc_bytes(filesize)
            includes.write("\nFile size = {0} \n".format(filesize))
            includes.write("Files registered = {0}, Bytes_stored = {1} \n".format(self.file_count, self.byte_count))
            self.record_to_csv(filepath, archive_path, filesize)

        else:
            includes.write("\nError registering file \n")

        includes.close()



    def audit_summary(self):
        includes = open(self.includes_path, "a")
        includes.write(
            "Number of files uploaded = {0}, total bytes so far = {1}".format(self.file_count, self.byte_count))
        includes.close()

        logging.info("Number of files uploaded = {0}, total bytes so far = {1}".format(self.file_count, self.byte_count))



    # Record to csv file: tarfile name, file path, archive path
    def record_to_csv(self, filepath, archive_path, filesize):

        includes_csv = open(self.includes_csv_path, "a")
        includes_csv.write(filepath + "," + archive_path + "," + str(filesize) + "\n")
        includes_csv.close()