import logging
import re
import json
import os

class SFHelper(object):



    @staticmethod
    def get_metadata_for_attributes(row, attr_list):
        metadata_items = []

        for key in attr_list:
            item = {}
            item["attribute"] = key;
            item["value"] = row[key]
            metadata_items.append(item)

        print metadata_items
        return metadata_items


    @staticmethod
    def add_metadata(metadata_items, key, value):
        item = {}
        item["attribute"] = key;
        item["value"] = value;
        metadata_items.append(item)
        return metadata_items



    @staticmethod
    def add_metadata_list(metadata_items, proj_meta, attrib_list):
        metadata_items.extend(SFHelper.get_metadata_for_attrbutes(proj_meta, attrib_list))




    @staticmethod
    def create_json_file(metadata, full_path, json_path):

        if not os.path.exists(json_path):
            os.mkdir(json_path)

        # create the metadata json file
        #name = full_path.rsplit('/', 1)[-1]
        name = full_path.replace('/', '_')
        json_file_name = json_path + '/' + name + ".json"
        with open(json_file_name, "w") as fp:
            json.dump(metadata, fp)

        return json_file_name

