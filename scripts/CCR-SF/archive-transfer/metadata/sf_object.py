import logging

from metadata.sf_parent import SFParent
from metadata.sf_helper import SFHelper
from metadata.sf_collection import SFCollection


from collections import OrderedDict

import json

class SFObject(object):


    def __init__(self, filepath, tarfile, ext, addParent, parentType = None):
        self.filepath = filepath
        self.tarfile = tarfile
        self.ext = ext
        self.addParent = addParent
        self.metadata = OrderedDict()
        self.metadata["generateUploadRequestURL"] = True
        self.metadata["metadataEntries"] = []
        self.extensions = {"md5": "MD5SUM", "bai" : "INDEX", "bam" : "BAM", "fastq.gz" : "FASTQ", "html" : "HTML"}
        self.archive_path = None
        self.parentType = parentType


    def register(self):
        self.build_metadata()


    def build_metadata(self):
        self.build_object_metadata()

        if(self.addParent):
            self.build_parent_metadata()
        else:
            self.metadata["createParentCollections"] = False

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
        #This assumes we are building for sample - Fixme
        if len(self.filepath.split("/")) > 1:
            parent_path = self.filepath.split("/")[-2]
        else:
            parent_path = self.filepath

        type = 'Sample'
        if self.parentType is not None:
            type = self.parentType

        sf_parent = SFParent(parent_path, type, self.tarfile, self.ext)
        #sf_parent.build_metadata_items()
        self.metadata["createParentCollections"] = True
        #self.metadata["parentCollectionMetadataEntries"] = sf_parent.get_metadata_items()


        pathsMetadataEntry = OrderedDict()
        pathsMetadataEntry["path"] = SFCollection.get_archive_path(self.tarfile, self.filepath, type, self.ext)
        pathsMetadataEntry["pathMetadataEntries"] = sf_parent.build_metadata_items()
        parentCollectionsBulkMetadataEntries = OrderedDict()
        parentCollectionsBulkMetadataEntries["pathsMetadataEntries"] = []
        parentCollectionsBulkMetadataEntries["pathsMetadataEntries"].append(pathsMetadataEntry)
        self.metadata["parentCollectionsBulkMetadataEntries"] = parentCollectionsBulkMetadataEntries




    def get_metadata(self):
        if(not any(self.metadata["metadataEntries"])):
            self.build_metadata()
        return self.metadata