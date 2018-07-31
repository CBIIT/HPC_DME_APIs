import logging
import os
import re
import subprocess

from metadata.sf_helper import SFHelper


class SFUtils(object):


    @staticmethod
    def get_meta_path(filepath, log = True):

        path = filepath.replace("uploads/", "")
        path = re.sub(r'.*Unaligned[^/]*/', '', path)

        # strip 'Project_' if it exists
        path = path.replace("Project_", "")

        if log is True:
            logging.info('metadata base: ' + path)

        return path



    @staticmethod
    def extract_files_from_tar(tarfile_path, extract_path):

        # extract files from the archive
        command = "tar -xf " + tarfile_path + " -C " + extract_path
        logging.info(command)
        os.system(command)
        logging.info("Extracted tarball " + tarfile_path)



    @staticmethod
    def get_filepath_to_archive(line, extract_path):
        # Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
        filepath = line.rstrip().split("../")[-1]
        filepath = filepath.split(" ")[-1]
        filepath = filepath.lstrip('/')

        filepath = extract_path + filepath

        logging.info("file to archive: " + filepath)
        return filepath


