import logging
import json

from metadata.sf_parent import SFParent
from collections import OrderedDict
from metadata.sf_helper import SFHelper

class SFCollection(SFParent):


    def __init__(self, path, type, tarfile, addParent, ext = None):
        SFParent.__init__(self, path, type, tarfile, ext)
        self.metadata = OrderedDict()
        self.metadata["metadataEntries"] = []
        self.addParent = addParent
        self.parent_types = {"Sample": "Flowcell", "Flowcell" : "Project", "Project" : "PI_Lab"}


    def set_metadataEntries(self):
        self.metadata["metadataEntries"] = self.get_metadata_items()
        logging.info(self.metadata)


    def build_metadata(self):
        self.build_metadata_items()
        self.metadata["metadataEntries"] = self.get_metadata_items()

        if (self.addParent):
            self.build_parent_metadata()

        logging.info(self.metadata)


    def build_parent_metadata(self):

        parent_metadata = SFParent(self.path, self.parent_types[self.type], self.tarfile, self.ext).build_metadata_items()
        self.metadata["createParentCollections"] = True

        pathsMetadataEntry = OrderedDict()
        pathsMetadataEntry["path"] = self.get_archive_path(self.tarfile, self.path, self.parent_types[self.type], self.ext)
        pathsMetadataEntry["pathMetadataEntries"] = parent_metadata
        parentCollectionsBulkMetadataEntries = OrderedDict()
        parentCollectionsBulkMetadataEntries["pathsMetadataEntries"] = []
        parentCollectionsBulkMetadataEntries["pathsMetadataEntries"].append(pathsMetadataEntry)
        self.metadata["parentCollectionsBulkMetadataEntries"] = parentCollectionsBulkMetadataEntries

        #self.metadata["parentCollectionMetadataEntries"] = parent_metadata



    def get_metadata(self):
        if(not any(self.metadata["metadataEntries"])):
            self.build_metadata()
        return self.metadata

    @staticmethod
    def get_archive_path(tarfile_name, path, type, ext = None):

        logging.info("Getting collection archive path for type: " + type + " and path " + path)

        archive_path = "/FNL_SF_Archive" #super(SFCollection, self).get_archive_path()
        
        pi_coll_path = "/PI_Lab_" + SFHelper.get_pi_name(path, False) 
        project_path = "/Auto_Project_" + SFHelper.get_project_name(path, tarfile_name, ext).replace(' ', '_')
        flowcell_path = "/Flowcell_" + SFHelper.get_flowcell_id(tarfile_name, False)

        if(type == "PI_Lab"):
            archive_path =  archive_path + pi_coll_path
        elif (type == "Project"):
            archive_path = archive_path + pi_coll_path + project_path
        elif (type == "Flowcell"):
            archive_path = archive_path + pi_coll_path + project_path + flowcell_path
        elif (type == "Sample"):
            sample_path = "/Sample_" + SFHelper.get_sample_name(path)
            archive_path = archive_path + pi_coll_path + project_path + flowcell_path + sample_path
        else:
            raise Exception("Incorrect collection type: " + type)

        logging.info("Collection archive path for type: " + type + " is: "+ archive_path)
        return archive_path




