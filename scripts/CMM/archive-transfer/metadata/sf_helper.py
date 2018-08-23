import logging
import re
from datetime import datetime


class SFHelper(object):


    @staticmethod
    def get_pi_name(name, log = True):


        if log is True:
            logging.info("Getting pi_name for: " + name)

        pi_name = name

        if log is True:
            logging.info("pi_name for " + name + " is " + pi_name)
        return pi_name






    @staticmethod
    def get_project_name(name, log = True):

        if log is True:
            logging.info("Getting project_name for: " + name)

        project_name = name

        if log is True:
            logging.info("pi_name for " + name + " is " + project_name)

        return project_name



