import logging
import os
import re
import subprocess
from metadata.sf_global import SFGlobal


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
    def record_exclusion(tarfile_name, file_name, str):

        SFGlobal.excludes.writelines(tarfile_name + ": " + file_name + " - " + str + '\n')
        SFGlobal.excludes.flush()
        SFGlobal.excludes_csv.write(tarfile_name + ", " + file_name + ", " + str + "\n")
        logging.warning('Ignoring file ' + str)


    @staticmethod
    def extract_file_to_archive(tarfile_name, tarfile_path, line):
        # Remove the ../ from the path in the list - TBD - Confirm that all content list files have it like that ?
        filepath = line.rstrip().split("../")[-1]
        filepath = filepath.split(" ")[-1]
        filepath = filepath.lstrip('/')


        if len(filepath.split("/")) < 3:
            # There is no subdirectory structure - something not right
            SFUtils.record_exclusion(tarfile_name, line, 'No subdirectory structure found')
            return

        # extract the fastq file from the archive
        command = "tar -xf " + tarfile_path + " -C uploads/ " + filepath
        logging.info(command)
        os.system(command)
        filepath = 'uploads/' + filepath

        logging.info("file to archive: " + filepath)

        return filepath


    @staticmethod
    def get_tarball_contents(tarfile_name, tarfile_dir):

        logging.info("Getting contents for: " + tarfile_name)
        tarfile_name = tarfile_name.rstrip()

        if '10x' in tarfile_name or 'singlecell' in tarfile_name:
            excludes_str = ': Invalid tar file -  10x or singlecell'
            SFUtils.record_exclusion(tarfile_name, "All files", excludes_str)
            return


        if not tarfile_name.endswith('tar.gz') and not tarfile_name.endswith('tar'):

            # If this is not a .list, _archive.list, or .md5 file also, then record exclusion. Else
            # just ignore, do not record because we may find the associated tar later
            if (not tarfile_name.endswith('tar.gz.list') and
                not tarfile_name.endswith('_archive.list') and
                not tarfile_name.endswith('list.txt') and
                not tarfile_name.endswith('.md5')):
                excludes_str = ': Invalid file format - not tar.gz, _archive.list, tar.gz.list or tar.gz.md5'
                SFUtils.record_exclusion(tarfile_name, "All files", excludes_str)
            else:
                logging.info(tarfile_name + ': No contents to extract')
            return


        tarfile_path = tarfile_dir + '/' + tarfile_name
        contentFiles = [tarfile_path + '.list', tarfile_name + '.list', tarfile_path + '_archive.list',
                    tarfile_path.split('.gz')[0] + '.list', tarfile_path.split('.tar')[0] + '.list',
                    tarfile_path.split('.tar')[0] + '_archive.list', tarfile_path.split('.tar')[0] + '.archive.list',
                    tarfile_path.split('.gz')[0] + '.list.txt', tarfile_path.split('.tar')[0] + '.list.txt',
                    tarfile_path.split('.gz')[0] + '_list.txt', tarfile_path.split('.tar')[0] + '_list.txt',
                    tarfile_path.split('.tar')[0] + '_file_list.txt']

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

        return tarfile_contents



    # Record to csv file: tarfile name, file path, archive path
    @staticmethod
    def record_to_csv(self, tarfile_name, filepath, fullpath):

        flowcell_id = SFHelper.get_flowcell_id(tarfile_name)
        normalized_filepath = fullpath.split("uploads/")[-1]
        if filepath.endswith('html'):
            head, sep, tail = fullpath.partition('all/')
            path = head.split(flowcell_id + '/')[-1]
        else:
            path = SFUtils.get_meta_path(fullpath, False)

        SFGlobal.includes_csv.write(tarfile_name + ", " + normalized_filepath + ", " + archive_path + ", " +
                   flowcell_id + ", " + SFHelper.get_pi_name(path) + ", " +
                   SFHelper.get_project_id(path, tarfile_name) + ", " +
                   SFHelper.get_project_name(path) + ", " +
                   SFHelper.get_sample_name(path) + ", " + SFHelper.get_run_name(tarfile_name) + "\n")