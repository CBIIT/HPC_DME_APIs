import logging
import re
import json
import os

class MetaHelper(object):



    @staticmethod
    def get_metadata_for_attributes(row, attr_list):
        metadata_items = []

        for key in attr_list:
            metadata_items = MetaHelper.add_metadata(metadata_items, key, row[key])

        return metadata_items


    @staticmethod
    def add_metadata(metadata_items, key, value):
        item = {}
        item['attribute'] = key;
        item['value'] = value;
        metadata_items.append(item)
        return metadata_items



    @staticmethod
    def add_metadata_list(metadata_items, proj_meta, attrib_list):
        metadata_items.extend(MetaHelper.get_metadata_for_attributes(proj_meta, attrib_list))
        return metadata_items




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

