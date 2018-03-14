import re
import os
import csv
import json
import logging
import tarfile
import subprocess
from time import strftime
import time
import argparse
import traceback
import json
from pprint import pprint
import urllib
from time import sleep

#A script to automatically archive hitif projects

class registration_log:
    """ A class to managed the files that keeps the registration logs"""

    def __init__(self, log_file):
        
        if os.path.exists(log_file):
            registered_items_file = open(log_file, "r") 
            self.registered_items = registered_items_file.read()
            registered_items_file.close()
        else:
            self.registered_items=""
     
        #now open it again to append
        self.registered_items_file = open(log_file, "a") 


    def item_exists(self, item_name):

        if item_name in self.registered_items:
            return True
        else:
            return False

    def add_item(self, item_name):
        self.registered_items_file.write("{0}\n".format(item_name))

    def close(self):
        self.registered_items_file.close()


def send_email(subject, message):
    """Email a message an optionally exit"""

    #recipients_list = ['george.zaki@nih.gov', 'gudlap@mail.nih.gov']
    recipients_list = ['george.zaki@nih.gov']
    recipients=','.join(recipients_list)

    email_command ='blat.exe -to {0} -server mailfwd.nih.gov -f hpcdme_cronjob@mail.nih.gov -subject "{1}" -body "{2}"'.format(recipients, subject, message)
    print email_command 
    logging.debug(email_command)
    message=os.popen(email_command).read()
    print message



def error_exit():
    """Exit the program and send notification emails"""
    subject="ERROR: HPCDME during registration" 
    body="See the log file {0}".format(log_path)
    send_email(subject, body)
    logging.shutdown()
    exit(1)

def email_error(message):
    """Email an error message"""
    subject="ERROR: HPCDME during registration" 
    send_email(subject, message)
    global n_errors
    global max_errors
    n_errors = n_errors + 1
    logging.error("Setting the number of errors to:{0}".format(n_errors))
    if n_errors >= max_errors:
        error_exit()


def email_warning(message):
    """Email a warning message"""
    subject="WARNING: HPCDME during registration" 
    send_email(subject, message)

def email_completion(message):
    """Email a warning message"""
    subject="COMPLETED: HPCDME script completed a registration job" 
    send_email(subject, message)



def get_immediate_subdirectories(a_dir):
    now = time.time()
    return [name for name in os.listdir(a_dir)
                if os.path.isdir(os.path.join(a_dir, name)) and os.stat(os.path.join(a_dir, name)).st_mtime < now - 7 * 86400]


def make_tarfile(output_filename, source_dir):
    with tarfile.open(output_filename, "w:") as tar:
            tar.add(source_dir, arcname=os.path.basename(source_dir))

def create_folder(folder_path):
    """Create a folder if it does not exist"""
    if not os.path.exists(folder_path):
        os.mkdir(folder_path)

def analyse_registration_output(output_string):

    """Parse the registration command output and return appropriate error"""
    parse_error="ERROR:Unable to parse error message:" + output_string
    success=0
    fail=1
    status_regex = re.compile("Status\s*:\s*(?P<status>[A-Z]+).*") 
    try:
        status = status_regex.search(output_string).groupdict()['status']
    except:
        return fail, parse_error 
    if status == "FAILED":
        return_exit = fail
        code_regex = re.compile("Result Code\s*:\s*CLI_(?P<code>[0-9]).*") 
        try:
            code = code_regex.search(output_string).groupdict()['code']
        except:
            return fail, parse_error
        if code ==  '0':
            message = "CLI_0: Authentication error"
        elif code == '1':        
            message = "CLI_1: Error reading file references from the properties file"
        elif code == '2':
            message = "CLI_2: Invalid user input"
        elif code == '3':
            message = "CLI_3: No input files to process"
        elif code == '4': 
            message = "CLI_4: Failed to process collection"
        elif code == '5': 
            message = "CLI_5: Failed to process data file"
        else: 
            message = "Unknown error"
    elif status == "COMPLETED":
        return_exit = success
        message = "Successful registration"
    else:
        return_exit = fail
        message = parse_error
        
    return return_exit, message


def check_dataobject(data_object_path):
    """
        Checks if the dataobject is registered and return its status
        Return: 
                'EMPTY' if the object does not exit.
                <status> The data transfer status of that object.
                'ERROR' Can not retrieve the status of that object
    """

    cmd_line="dm_get_dataobject '{0}'".format(data_object_path)
    try:
        metadata_string = subprocess.check_output(cmd_line, stderr=subprocess.STDOUT, shell=True)    
    except  subprocess.CalledProcessError as e:
        #If zero is returned, the file does not exists    
        return 'EMPTY' 

    metadata_dic = json.loads(metadata_string)

    #Make sure it is the correct dataObject, as the metadata is returned as a list:  
    unquoted_data_object_path = urllib.unquote_plus(data_object_path)
    if metadata_dic['dataObjects'][0]['dataObject']['absolutePath'] != unquoted_data_object_path:
        logging.error('Cannot retrieve metadata for {0}'.format(data_object_path))
        logging.error('returned metadat: {0}'.format(metadata_string))
        return 'ERROR'
    self_metadata = metadata_dic['dataObjects'][0]['metadataEntries']['selfMetadataEntries']
    for pair in self_metadata:
        if pair['attribute'] == 'data_transfer_status':
            return pair['value']
    return 'ERROR'

def register_collection(path, metadata, collection_type="Folder"):

  #Generate metadata file    
  #Make a list of dictionaries  for the metadata
  metadata["collection_type"] = collection_type
  formated_metadata = [ dict([("attribute", attribute), ("value",metadata[attribute])]) for attribute in metadata.keys() ]
  metadata_dict={"metadataEntries":formated_metadata} 
  metadata_path="collecton_metadata.json"
  metadata_file= open(metadata_path, "w")
  metadata_file.write(json.dumps(metadata_dict))
  metadata_file.close()

  #Submit the request 
  register_command = "dm_register_collection '{0}' '{1}'".format(metadata_path, path)
  logging.debug(register_command)

  try:
    output = subprocess.check_output(register_command, stderr=subprocess.STDOUT, shell=True)    
  except  subprocess.CalledProcessError as e:
    logging.error("Can not register collection")
    logging.error("Status: {0}, message: {1}".format(e.returncode, e.output))
    error_exit()

def register_dataObject_sync(path, metadata, src_file):

  #Generate metadata file    
  #Make a list of dictionaries  for the metadata
  formated_metadata = [ dict([("attribute", attribute), ("value",metadata[attribute])]) for attribute in metadata.keys() ]
  metadata_dict={"metadataEntries":formated_metadata} 
  metadata_path= src_file + ".metadata.json"
  metadata_file= open(metadata_path, "w")
  metadata_file.write(json.dumps(metadata_dict))
  metadata_file.close()
  #Submit the request
  os.chdir(os.path.dirname(src_file))
  #Create a list of the tar file.
  file_list="files.txt"
  with open("files.txt", 'w') as file_pointer:
    file_pointer.write(os.path.basename(src_file))
  file_pointer.close()
  register_command = "dm_register_directory -s -l '{0}' . '{1}'".format(file_list, path)
  logging.debug(register_command)
  output = os.popen(register_command).read()
  with open('cli-output.txt', 'w') as cli_output:
    cli_output.write(output)
  cli_output.close()
  [code, message]= analyse_registration_output(output)
  if code != 0:
    logging.debug("ERROR: failed to register file\nMessage:{0}".format(message))
    email_error("Registering the file {0} returned the error message:{1}".format(path, message))
  return [code, message]

#Register a PI collecton
def register_pi_lab(row, registered_pis):
  pi_name = row['piname']  
  pi_email = row['nihpiusername'] 

  pi_path = "PI_" + pi_name.replace(" ", "_")
  pi_collection_path = os.path.join(base_path, pi_path)

  if not registered_pis.item_exists(pi_name):
    #Register the pi lab collection
    pi_metadata = {}  
    pi_metadata["pi_name"] = pi_name
    pi_metadata["pi_email"] = pi_email
    pi_metadata["institute"] = row['institute']
    pi_metadata["lab"] = row['lab']
    register_collection(pi_collection_path, pi_metadata)
    registered_pis.add_item(pi_name)

  return pi_collection_path
  
def register_user_collection(row, pi_collection_path, registered_users):
  user_name = row['username']
  user_email = row['nihusername']
  user_path = "User_"  + user_name.replace(" ", "_")
  user_collection_path = os.path.join(pi_collection_path, user_path)
  comment = row['comments'].strip()
  if not registered_users.item_exists(user_name):
    #Register the user collection 
    user_metadata = {}
    user_metadata["name"] = user_name
    user_metadata["email"] = user_email
    user_metadata["branch"] = row['branch']
    #Add a comment if it exists
    if comment != '':
        user_metadata["comment"] = comment
    register_collection (user_collection_path, user_metadata)
    registered_users.add_item(user_name)

  return user_collection_path
   
def register_experiment(user_collection_path, experiment_dir, registered_experiments):

  experiment_name = experiment_dir.replace(" ", "_")
  experiment_name = urllib.quote_plus(experiment_name)
  exp_path = "Exp_"  + experiment_name
  #exp_path = "Exp_"  + experiment_dir.replace(" ", "_").replace("%", "%25").replace("#", "%23").replace("[", "%5B").replace("]", "%5D")
  exp_collection_path = os.path.join(user_collection_path, exp_path)

  if not registered_experiments.item_exists(experiment_dir):
    #Register the experiment collection 
    exp_metadata = {}
    exp_metadata["experiment_name"] = experiment_dir 
    register_collection (exp_collection_path, exp_metadata)
    registered_experiments.add_item(experiment_dir)

  return exp_collection_path


parser = argparse.ArgumentParser()
parser.add_argument("users_file", help = 'A csv file that contains the users information')
parser.add_argument("archive_database", help = 'The path to the directory that contains the archive database and working area.')

args = parser.parse_args()


#Read the users CSV File 
users_file = args.users_file
archive_database=args.archive_database 
#archive_database="/cygdrive/V/HiTIF_Management/Archiving_Scripts/CV7000/CV7000_HPCDME"
base_path = "/HiTIF_Archive" 

current_time=time.strftime("%Y-%m-%d_%H-%M-%S")
log_path = os.path.join(archive_database, "logging-" + current_time + ".txt")
logging.basicConfig(filename=log_path, level=logging.ERROR, format='%(asctime)s %(message)s', datefmt='%m/%d/%Y %I:%M:%S %p')
print "started logging file {0}".format(log_path)
#logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(message)s', datefmt='%m/%d/%Y %I:%M:%S %p')

if not os.path.exists(archive_database):
    logging.debug("The archive database {0} is not found.".format(archive_database))
    error_exit()


logging.debug("registering experiments for users in the user file: {0}".format(users_file))
if not os.path.exists(users_file):
    logging.error("Can not find the users file:{0}".format(users_file))
    error_exit()

try:
    user_reader = csv.DictReader(open(users_file))
except Exception as inst:
    logging.error("Unable to parse the users file {0}".format(users_file))
    tb = traceback.format_exc()
    logging.debug(tb)
    error_exit()

#The number of file registration errors that took place:
n_errors = 0
#The maximum number of allowed file registration erros
max_errors = 3
try:
    users_dir = os.path.dirname(os.path.abspath(users_file))
    registered_pis_file = os.path.join(archive_database, "registered_pis.txt")
    registered_users_file = os.path.join(archive_database, "registered_users.txt")
    registered_pis = registration_log(registered_pis_file)
    registered_users= registration_log(registered_users_file)

    #loop over all users
    for row in user_reader:
    
      folder_name = row['foldername']
    
      #Mirror the user direcotry in the database
      user_database_path = os.path.join(archive_database, folder_name)
      create_folder(user_database_path)
      os.chdir(user_database_path)
    
      #Get the path on the archive
      pi_collection_path = register_pi_lab(row, registered_pis) 
      user_collection_path = register_user_collection(row, pi_collection_path, registered_users)
    
      #Get the projects of that user
      user_dir = os.path.abspath(os.path.join(users_dir, folder_name))
    
      #Initialize the experiments log for that user
      experiments_database_file = os.path.join(user_database_path, "registered_experiments.txt")
    
      #prepare the experiments database file
      registered_experiments = registration_log(experiments_database_file)
    
      #loop over experiments
      experiments_path = get_immediate_subdirectories(user_dir) 
      for  experiment in experiments_path:
       
        exp_dir = os.path.abspath(os.path.join(user_dir,experiment))
    
        #Miror the experiment folder in the database
        experiment_database_path = os.path.join(user_database_path, experiment)
        create_folder(experiment_database_path)
        os.chdir(experiment_database_path)
    
        #Register the experiment collection in the archive
        exp_collection_path = register_experiment(user_collection_path, experiment, registered_experiments)
    
        #Initialize measurments log
        registered_measurments = registration_log(os.path.join(experiment_database_path, "registered_measurments.txt"))
    
        measurments_path = get_immediate_subdirectories(exp_dir)
        for measurment in measurments_path:
       
          tar_file_name = measurment.replace(" ", '_') + ".tar.gz"
          data_object_path = exp_collection_path + "/" + tar_file_name
    
          #Check if the measurment is already archieved in the local database
          if not registered_measurments.item_exists(measurment):
    
            #Check if the measurment is already registered in the archive.
            archive_status = check_dataobject(data_object_path) 
            #Generate the tar file name 
            tar_file_name = measurment.replace(" ", '_') + ".tar.gz"
            tar_file_path = os.path.join(experiment_database_path, tar_file_name)
            if archive_status == 'EMPTY' or archive_status == 'URL_EXPIRED':

                #Archive the measurment.
                measurment_src_path = os.path.join(exp_dir, measurment)
                logging.debug("Registering the measurment:{0}".format(measurment_src_path))
                #tar the measurment folder and upload it to cleversave 
                make_tarfile(tar_file_path, measurment_src_path)
                metadata={"experiment_name":measurment}
                try:
                    
                    code, message = register_dataObject_sync(exp_collection_path, metadata, os.path.abspath(tar_file_path))
                    if code == 0:
                        registered_measurments.add_item(measurment)
                except Exception as inst:
                    logging.error("FAILED: registration for the measurment {0}".format(measurment_src_path)) 
                    tb = traceback.format_exc()
                    logging.debug(tb)
                    error_exit()
                os.remove(tar_file_name)
            elif archive_status == 'ARCHIVED'  :
                #The measurment is in the archive but not in the databse:
                #Add the measurment to the database
                registered_measurments.add_item(measurment)
                #Check if the tar file got deleted
                if os.path.exists(tar_file_path):
                    message = "The tar file {0} exists while the measurment is already ARCHIVED".format(os.path.abspath(tar_file_path))
                    logging.warning(message)
                    email_warning(message)
            elif archive_status == 'ERROR'  :
                logging.error('Can not retrieve the transfer status for {0}'.format(data_object_path))
                error_exit()
            else:
                #Generate a warning
                message = "The transfer status of the measurment file {0} is: {1}".format(data_object_path, archive_status)
                logging.warning(message)
                email_warning(message)    

        registered_measurments.close()
        sleep(1)
    
      registered_experiments.close()
      sleep(1)
except Exception as inst:
    tb = traceback.format_exc()
    logging.error(tb)
    error_exit()


registered_pis.close()
registered_users.close()
message =  "The archive script completed the registration of the user file: {0} \n. See complete registration log at {1}.".format(users_file, log_path)
email_completion(message)
