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
        path = re.sub(r'.*Unalignd[^/]*/', '', path)
        path = re.sub(r'.*Unlianged[^/]*/', '', path)

        #hardcoded exception for 2017 directory only. Can remove after that.
        path = path.replace("oldMarkRaffeld/", "")

        # strip 'Project_' if it exists
        path = path.replace("Project_", "")

        if log is True:
            logging.info('metadata base: ' + path)

        return path


    @staticmethod
    def get_unaligned_ext(filepath):

        #Get the characters after 'Uanligned_' or 'Unalignd_'
        # and before '/'. This will be appended to project_name
        #for Undetermined files to make their path unique

        if 'Unaligned_' in filepath:
            ext = filepath.split('Unaligned_')[-1]
            ext = ext.split('/')[0]

        elif 'Unalignd_' in filepath:
            ext = filepath.split('Unalignd_')[-1]
            ext = ext.split('/')[0]

        elif 'Unaligned' in filepath:
            ext = filepath.split('Unaligned')[-1]
            ext = 'Unaligned' + ext.split('/')[0]

        else:
            ext = None

        if ext is not None:
            logging.info('Unaligned ext for filepath + ' + filepath + ' : ' + ext)
        else:
            logging.info('No ext. for Unaligned filepath + ' + filepath)

        return ext



    @staticmethod
    def extract_files_from_tar(tarfile_path, extract_path):

        # extract files from the archive
        command = "tar -xf " + tarfile_path + " -C " + extract_path
        logging.info(command)
        if (os.system(command) == 0):
            logging.info("Extracted tarball " + tarfile_path)
            return True
        else:
            logging.fatal("ERROR: Unable to untar tarfile_path")
            return False



    @staticmethod
    def get_filepath_to_archive(line, extract_path):
        # Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
        filepath = line.rstrip().split("../")[-1]
        filepath = filepath.split(" ")[-1]
        filepath = filepath.lstrip('/')

        filepath = extract_path + "/" + filepath

        logging.info("file to archive: " + filepath)
        return filepath


    @staticmethod
    def get_tarball_contents(tarfile_name, tarfile_dir, sf_audit, extract_path):

        logging.info("Getting contents for: " + tarfile_name)
        tarfile_name = tarfile_name.rstrip()
        tarfile_path = tarfile_dir + '/' + tarfile_name


        if not tarfile_name.endswith('tar.gz') and not tarfile_name.endswith('tar'):
            logging.info(tarfile_name + ': No contents to extract')
            return

        tarfile_list_path = extract_path + "/" + tarfile_name + ".list"
        command = "tar tvf " + tarfile_path + " > " + tarfile_list_path
        os.system(command)
        subprocess.call(command, shell=True)
        logging.info("Created contents file: " + command)
        tarfile_contents = open(tarfile_list_path)

        logging.info("Obtained contents for: " + tarfile_name)
        return tarfile_contents

