import logging

from metadata.sf_parent import SFParent
from metadata.sf_helper import SFHelper


from collections import OrderedDict

import json

class SFObject(object):


    def __init__(self, filepath, tarfile, addParent):
        self.filepath = filepath
        self.tarfile = tarfile
        self.addParent = addParent
        self.metadata = OrderedDict()
        self.metadata["metadataEntries"] = []
        self.extensions = {"md5": "MD5SUM", "bai" : "INDEX", "bam" : "BAM", "fastq.gz" : "FASTQ"}
        self.archive_path = None


    def register(self):
        self.build_metadata()


    def build_metadata(self):
        self.build_object_metadata()

        if(self.addParent):
            self.build_parent_metadata()

        logging.info(self.metadata)


    def build_object_metadata(self):
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


    def build_parent_metadata(self):
        parent_name = self.filepath.split("/")[-2]
        sf_parent = SFParent(parent_name, "Sample", self.tarfile)
        sf_parent.build_metadata_items()

        self.metadata["createParentCollections"] = True
        self.metadata["parentCollectionMetadataEntries"] = sf_parent.get_metadata_items()


    def get_metadata(self):
        if(not any(self.metadata["metadataEntries"])):
            self.build_metadata()
        return self.metadata