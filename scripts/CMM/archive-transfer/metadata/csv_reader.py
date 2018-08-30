import logging
import re
from datetime import datetime
import csv


class CSVMetadataReader(object):
    def __init__(self, input_filepath):
        self.input_filepath = input_filepath




    def find_metadata_row(self, attribute_name, attribute_value):


        logging.info('project_dir value for metadata search: ' + attribute_value)

        input_file = open(self.input_filepath, 'r')
        reader = csv.DictReader(input_file)
        title = reader.fieldnames
        for row in reader:
            if row[attribute_name] == attribute_value:
                #print row
                logging.info('Metadata set for ' + attribute_name + ' with value ' + attribute_value + ' is: ')
                logging.info(row)
                return row




