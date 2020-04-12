import logging

from metadata.sf_helper import SFHelper


class SFParent(object):


    def __init__(self, path, type, tarfile, ext = None):

        self.metadata_items  = []
        self.type = type
        self.path = path
        self.tarfile = tarfile
        self.archive_path = None
        self.base_path = "/FNL_SF_Archive/"
        self.ext = ext



    def build_metadata_items(self):

        self.set_attribute("collection_type", self.type)

        if(self.type == "PI_Lab"):
            self.set_pi_lab_attributes()

        elif(self.type == "Sample"):
            self.set_sample_attributes()

        elif(self.type == "Flowcell"):
            self.set_flowcell_attributes()

        elif(self.type == "Project"):
            self.set_project_attributes()

        else:
            raise Exception("Incorrect collection type in parent: " + self.type)

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
        pi_name = SFHelper.get_pi_name(self.path)
        self.set_attribute("pi_name", pi_name)
        self.archive_path = self.base_path + self.type + "_" + pi_name


    def set_sample_attributes(self):
        self.set_attribute("sample_name", SFHelper.get_sample_name(self.path))

        #temporary - for sample_id, #source_organism - Fixme
        self.set_attribute("sample_id", "Unspecified")
        self.set_attribute("source_organism", "Unspecified")


    def set_flowcell_attributes(self):
        self.set_attribute("flowcell_id", SFHelper.get_flowcell_id(self.tarfile))
        self.set_attribute("run_date", SFHelper.get_run_date(self.tarfile))
        self.set_attribute("run_name", SFHelper.get_run_name(self.tarfile))
        self.set_attribute("sequencing_platform", SFHelper.get_sequencing_platform(self.tarfile))

        #temporary - for pooling, read length, sequencing_application_type - Fixme
        self.set_attribute("sequencing_application_type", SFHelper.get_sequencing_application_type(self.path))
        self.set_attribute("pooling", "Unspecified")
        self.set_attribute("read_length", "Unspecified")


    def set_project_attributes(self):
        contact_name = SFHelper.get_contact_name(self.path)
        if contact_name is not None:
            self.set_attribute("contact_name", contact_name)
        self.set_attribute("project_name", SFHelper.get_project_name(self.path, self.tarfile, self.ext))
        self.set_attribute("project_id_CSAS_NAS", SFHelper.get_project_id(self.path))


    def get_metadata_items(self):
        return self.metadata_items


    def get_archive_path(self):
        if(self.type == "PI_Lab"):
            pi_name = self.get_attribute("pi_name")
            archive_path = "PI_Lab" + "_" + pi_name

        elif (self.type == "Project"):
            project_name = self.get_attribute("project_name")
            archive_path = "Project" + "_" + project_name

        elif (self.type == "Flowcell"):
            flowcell_id = self.get_attribute("flowcell_id")
            archive_path = "Flowcell" + "_" + flowcell_id

        elif (self.type == "Sample"):
            sample_name = self.get_attribute("sample_name")
            archive_path = "Sample" + "_" + sample_name

        else:
            archive_path = None

        return archive_path