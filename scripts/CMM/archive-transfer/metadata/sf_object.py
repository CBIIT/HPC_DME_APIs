import logging

from metadata.meta_helper import MetaHelper


from collections import OrderedDict

import json

class SFObject(object):


    def __init__(self, filepath, parent):
        self.file_path = filepath
        self.metadata = OrderedDict()
        self.parent = parent
        self.metadata["metadataEntries"] = []
        self.archive_path = None
        self.base_path = "/CCR_CMM_Archive"
        self.extensions = {"md5": "MD5SUM", "bai": "INDEX", "bam": "BAM", "fastq.gz": "FASTQ", "html": "HTML", "dm4": "DM4"}



    def build_metadata(self):

        name = self.file_path.split("/")[-1]
        MetaHelper.add_metadata(self.metadata["metadataEntries"], "object_name", name)

        match_found = False;
        for ext, type in self.extensions.items():
            if self.file_path.endswith(ext):
                MetaHelper.add_metadata(self.metadata["metadataEntries"], "file_format", type)
                match_found = True
                break

        if not match_found:
            extension = self.file_path.split('.')[-1]
            MetaHelper.add_metadata(self.metadata["metadataEntries"], "file_format", extension)

        logging.info(self.metadata)
        #print self.metadata

        return self.metadata



    def get_metadata(self):
        #if(not any(self.metadata["metadataEntries"])):
            #self.build_metadata()
        return self.metadata



    def get_archive_path(self):
        if self.archive_path is None:
            self.set_archive_path()

        return self.archive_path



    def set_archive_path(self):

        if self.parent is None:
            parent_archive_path = self.base_path
        else:
            parent_archive_path = self.parent.archive_path

        file_name = self.file_path.rsplit('/', 1)[-1]
        self.archive_path = parent_archive_path + '/' + file_name


        logging.info("Object archive path for " + self.file_path + "is: " + self.archive_path)