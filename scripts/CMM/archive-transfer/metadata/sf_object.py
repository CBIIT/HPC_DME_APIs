import logging

from metadata.sf_helper import SFHelper


from collections import OrderedDict

import json

class SFObject(object):


    def __init__(self, filepath):
        self.filepath = filepath
        self.metadata = OrderedDict()
        self.metadata["metadataEntries"] = []
        self.extensions = {"md5": "MD5SUM", "bai": "INDEX", "bam": "BAM", "fastq.gz": "FASTQ", "html": "HTML"}
        self.archive_path = None


    def register(self):
        self.build_metadata()
        logging.info(self.metadata)



    def build_metadata(self):
        self.set_object_name()
        self.set_file_type()


    def set_object_name(self):
        name = self.filepath.split("/")[-1]
        self.set_attribute("object_name", name)


    def set_file_type(self):
        for ext, type in self.extensions.items():
            if self.filepath.endswith(ext):
                self.set_attribute("file_type", type)
                return


    def set_attribute(self, key, value):
        item = {}
        item["attribute"] = key;
        item["value"] = value;
        self.metadata["metadataEntries"].append(item);



    def get_metadata(self):
        if(not any(self.metadata["metadataEntries"])):
            self.build_metadata()
        return self.metadata