import logging
import sys
import os
import json
import shutil
import subprocess

#from metadata.sf_object import SFObject
#from metadata.sf_collection import SFCollection
#from metadata.sf_helper import SFHelper
from common.sf_utils import SFUtils
from common.sf_audit import SFAudit
from metadata.sf_helper import SFHelper

#Usage:
#python app.py tarfiles /is2/projects/CCR-SF/archive/illumina/2015_new /mnt/IRODsScratch/bulk-uploads/CCR-SF/2015_new/uploads/ 2015_new_dryrun_with_size True True

#python app.py pi_dir_list


def main(args):
    if len(sys.argv) <> 3:
        print("\n Usage: python app.py base_dir pi_dir_list")
        return

    # The root dir of the source
    base_dir = args[1]

    # The file containing the PI directory
    pi_dir_list = args[2]

    # path containing the extracted file
    dest_base_dir = base_dir + "/staged"

    # path containing the extracted file
    extract_path = base_dir + "/work"

    # sub-directory to hold the log and audit files
    audit_dir = base_dir + "/work"

    bytes_stored = 0
    files_registered = 0

    sf_audit = SFAudit(audit_dir, extract_path, bytes_stored, files_registered)
    sf_audit.prep_for_audit()

    pi_dir_list_path = base_dir + "/" + pi_dir_list
    for line_dirname in open(pi_dir_list_path).readlines():

        pi_dir_path = base_dir + '/' + line_dirname.rstrip()
        destDir = dest_base_dir + "/" + line_dirname.rstrip()
        print "destDir = " + destDir
        if not os.path.exists(destDir):
            os.mkdir(destDir)

        # This is a valid pi directory, so process
        logging.info("Processing pi dir: " + pi_dir_path)

        #number go through each dir here that get the one that has the name 'fastq' it it
        #for element in os.listdir(pi_dir_path):
        for(dirName, subdirList, fileList) in os.walk(pi_dir_path):
            for fileName in fileList:
                if fileName.startswith('Seq') and fileName.endswith('.tar'):
                    logging.info("fileName: %s", fileName)
                    #Untar to extract_path
                    tarPath = dirName + "/" + fileName

                    # Get or create list file (if not present)
                    tarfile_contents = SFUtils.get_tarball_contents(fileName, dirName, sf_audit, extract_path)
                    if tarfile_contents is None:
                        continue

                    if not (SFUtils.extract_files_from_tar(tarPath, extract_path)):
                        # Something wrong with this file path, go to
                        # next one and check logs later
                        continue;

                    # loop through each line in the contents file of this tarball
                    # We need to do a copy for each fastq.gz or BAM file
                    for line in tarfile_contents.readlines():
                        logging.info('processing line in tarfile: ' + line)

                        if (line.rstrip().endswith("/")):
                        # This is a directory, nothing to do
                            continue

                        # Get full path of the extracted file
                        filePath = SFUtils.get_filepath_to_archive(line.rstrip(), extract_path)
                        logging.info('Extracted filePath ' + filePath)

                        if filePath.endswith('fastq') or filePath.endswith('fastq.gz') \
                                or filePath.endswith('fastq.gz.md5') \
                                or (filePath.endswith('laneBarcode.html') and '/all/' in filePath):
                           copy_file(tarPath, filePath, destDir, sf_audit)

                    os.system("rm -rf " + extract_path + "/" + fileName.split(".tar")[0])

                elif fileName.endswith('bam'):
                    filePath = dirName + "/" + fileName
                    copy_file(None, filePath, destDir, sf_audit)


        logging.info('Done processing directory: ' + pi_dir_path)

    sf_audit.audit_summary()


def copy_file(tarFile, filePath, destBaseDir, sf_audit):

    # Extract the info for PI metadata
    # path = SFUtils.get_meta_path(filepath)
    destDir = destBaseDir

    logging.info("filePath = %s", filePath)
    print "filePath = " + filePath
    # Extract MRN number - split the string at /SCAF, get the 4 digit starting at 10th index
    if '/SCAF' in filePath:
        mrnSubPath = filePath.split('/SCAF')[1]
        logging.info("mrnSubPath = %s", mrnSubPath)
        print "mrnSubPath = " + mrnSubPath
        if not mrnSubPath.startswith('/'):
            mrnNumber = mrnSubPath.split('_')[1][0:4]
            logging.info("MRN Number = %s", mrnNumber)
            print "MRN Number = " + mrnNumber
            # Create Patient folder using that MRN number if the folder does not already exist
            mrnDir = destBaseDir + '/Patient_' + mrnNumber
            print "mrnDir = " + mrnDir
            if not os.path.exists(mrnDir):
                os.mkdir(mrnDir)
            desDir = mrnDir

            #Create Run folder in the above Patient folder if it does not exist
            if(tarFile is not None):
                flowcell_id = SFHelper.get_flowcell_id(tarFile.split('/')[-1])
                print "flowcell_id = " + flowcell_id
                runDir = mrnDir + '/Run_' + flowcell_id
                if not os.path.exists(runDir):
                    os.mkdir(runDir)
                destDir = runDir

            #name of file to copy from filePath
            fileName = filePath.split('/')[-1]
            print "file to copy = "  + fileName
            # copy to that Patient directory
            destPath = destDir + '/' + fileName
            shutil.copy(filePath, destPath)
            logging.info("copied file to: %s", destPath)
            print "copied file to " + destPath
            sf_audit.audit_copy(tarFile, filePath, destPath)


main(sys.argv)
