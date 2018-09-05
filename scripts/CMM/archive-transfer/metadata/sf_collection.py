import logging
import json

from collections import OrderedDict
from metadata.meta_helper import MetaHelper

class SFCollection(object):


    def __init__(self, path, type, parent):

        self.metadata_items = []
        self.type = type
        self.parent = parent
        self.path = path
        self.name = None
        self.archive_path = None
        self.base_path = "/CCR_CMM_Archive"


        self.metadata = OrderedDict()
        self.metadata["metadataEntries"] = []
        #self.metadata_attributes = {'PI_Lab' : ['pi_name'],
         #                           'Project': ['affiliation', 'project_name', 'start_date', 'project_title', 'description', 'method', 'collaborator', 'publications'],
          #                          'Run': ['run_date']}





    def build_metadata(self, proj_meta):

        metadata_items = []
        if self.type is not None:
            metadata_items = MetaHelper.add_metadata(metadata_items, "collection_type", self.type)
        else:
            metadata_items = MetaHelper.add_metadata(metadata_items, "collection_type", 'Folder')

        #if self.type in self.metadata_attributes.keys():
            #attribute_list = self.metadata_attributes[self.type]
            #print attribute_list
        metadata_items = MetaHelper.add_metadata_list(metadata_items, proj_meta)

        metadata_items =  self.extract_mandatory_metadata(metadata_items)

        self.metadata["metadataEntries"] = metadata_items
        logging.info(self.metadata)
        #print self.metadata
        return self.metadata


    def extract_mandatory_metadata(self, metadata_items):

        # Add Extractable mandatory metadata if not present in metadata_items

        if (self.type == 'Run'):
            match_found = False
            for item in metadata_items:
                if item['attribute'] == 'run_date':
                    match_found = True
                    break

            if  not match_found:
                item = {}
                item['attribute'] = 'run_date'
                item['value'] = self.path.split('/')[-1].split('_')[0]
                metadata_items. append(item)

        return metadata_items


    def get_name(self):
        if self.name:
            return self.name
        else:
            match_found = False
            for item in self.metadata["metadataEntries"]:
                if item['attribute'] == 'collection_name':
                    match_found = True
                    self.name = item['value']
                    break

            if not match_found:
                self.name = self.path.split('/')[-1]

            return self.name



    def get_metadata(self):
        #if(not any(self.metadata["metadataEntries"])):
            #self.build_metadata()
        return self.metadata




    def get_archive_path(self):
        if self.archive_path is None:
            self.set_archive_path()

        return self.archive_path



    def set_archive_path(self):

        logging.info("Getting collection archive path for " + self.path)
        if self.parent is None:
            parent_archive_path = self.base_path
        else:
            parent_archive_path = self.parent.archive_path

        if self.type is not None:
            self.archive_path = parent_archive_path + '/' + self.type + '_' + self.get_name()
        else:
            self.archive_path = parent_archive_path + '/'  + self.get_name()

        logging.info("Collection archive path for " + self.path + "is: " + self.archive_path)





