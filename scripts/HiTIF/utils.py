
import os


def get_measurments_mrf(a_dir):
    """
        Returns a list of all measurments in a directory. 
        Input:
            a_dir: The user directory
        Returns:
            list of tuples (user, experiment, measurment) 

    """
    matches = []
    for root, dirnames, filenames in os.walk(a_dir):
        for filename in filenames:
            if filename.endswith(".mrf"):
                hierarchy = root.split(os.sep)
                measurment_name = hierarchy[-1]
                experiment = hierarchy[-2]
                user = hierarchy[-3]
                measurment = {}
                measurment["user"] = user
                measurment["experiment"] = experiment
                measurment["name"] = measurment_name
                measurment["path"] = os.path.abspath(root)
                measurment["depth"] = a_dir.count(os.sep) - root.count(os.sep)
                matches.append(measurment)

    return matches


def verify_mrf_depth(root):
    """
        Make sure that all the mrl files are within the depth of 1 from root.
        Inputs: 
            root: The top level directory.:w
            
    """
    mrf_file_list = get_measurments_mrf(root)
    if len(mrf_file_list) != 1:
        return False
    
    if mrf_file_list[0]["depth"] != 0:
        return False
    else :
        return True
