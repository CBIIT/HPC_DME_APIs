import logging
import json

from collections import OrderedDict
from metadata.sf_helper import SFHelper

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
        #self.parent_types = {"Sample": "Flowcell", "Flowcell" : "Project", "Project" : "PI_Lab"}


    def build_metadata_items(self):

        if self.type is not None:
            self.set_attribute("collection_type", self.type)
        else:
            self.set_attribute("collection_type", 'Folder')

        if(self.type == "PI_Lab"):
            self.set_pi_lab_attributes()


        elif(self.type == "Project"):
            self.set_project_attributes()

        #else:
         #   raise Exception("Incorrect collection type in parent: " + self.type)

        return self.metadata_items


    def set_attribute(self, key, value):
        item = {}
        item["attribute"] = key;
        item["value"] = value;
        self.metadata_items.append(item)


    def get_attribute(self, key, meta_list = None):

        if(meta_list is None):
            meta_list = self.metadata_items

        for item in meta_list:
            if item['attribute'] == key:
                item_val = item['value']
                break
        else:
            item_val = None

        return item_val


    def set_pi_lab_attributes(self):
        self.set_attribute("pi_name", SFHelper.get_pi_name(self.name))


    def set_project_attributes(self):
        self.set_attribute("project_name", SFHelper.get_project_name(self.name))



    def get_metadata_items(self):
        return self.metadata_items



    def set_metadataEntries(self):
        self.metadata["metadataEntries"] = self.get_metadata_items()
        logging.info(self.metadata)


    def build_metadata(self):

        self.metadata["metadataEntries"] = self.build_metadata_items()

        logging.info(self.metadata)



    def get_metadata(self):
        if(not any(self.metadata["metadataEntries"])):
            self.build_metadata()
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





