import logging
import json

from collections import OrderedDict
from metadata.meta_helper import MetaHelper

class SFCollection(object):


    def __init__(self, name, type, parent):

        self.metadata_items = []
        self.type = type
        self.parent = parent
        self.name = name
        self.archive_path = None
        self.base_path = "/CCR_CMM_Archive"


        self.metadata = OrderedDict()
        self.metadata["metadataEntries"] = []
        self.metadata_attributes = {'PI_Lab' : ['pi_name'],
                                    'Project': ['affiliation', 'project_name', 'start_date', 'project_title', 'description', 'method', 'collaborator', 'publications']}





    def build_metadata(self, proj_meta):

        metadata_items = []
        if self.type is not None:
            metadata_items = MetaHelper.add_metadata(metadata_items, "collection_type", self.type)
        else:
            metadata_items = MetaHelper.add_metadata(metadata_items, "collection_type", 'Folder')

        if self.type in self.metadata_attributes.keys():
            attribute_list = self.metadata_attributes[self.type]
            #print attribute_list
            metadata_items = MetaHelper.add_metadata_list(metadata_items, proj_meta, attribute_list)

        self.metadata["metadataEntries"] = metadata_items
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

        logging.info("Getting collection archive path for " + self.name)
        if self.parent is None:
            parent_archive_path = self.base_path
        else:
            parent_archive_path = self.parent.archive_path

        if self.type is not None:
            self.archive_path = parent_archive_path + '/' + self.type + '_' + self.name
        else:
            self.archive_path = parent_archive_path + '/'  + self.name

        logging.info("Collection archive path for " + self.name + "is: " + self.archive_path)





