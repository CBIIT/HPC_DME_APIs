import logging
import re
from datetime import datetime


class SFHelper(object):


    @staticmethod
    def get_pi_name(path, log = True):

        pi_names = {"Amundadottir": "Laufey_Amundadottir", "Basrai": "Munira_Basrai", "Beldaid": "Yasmine_Belkaid", "Bottaro": "Donald_Bottaro", "Bernal": "Frederico_Bernal",
                    "Brognard": "John_Brognard", "LiangCao": "Liang_Cao", "Carrington": "Mary_Carrington", "Haobin": "Haobin_Chen",
                    "JuliaCooper": "Julia_Cooper", "JulieCooper": "Julie_Cooper", "Durum": "Scott_Durum", "Gardner": "Kevin_Gardner", "Gress": "Ronald_Gress", "Curtis": "Curtis_Harris",
                    "Hattar": "Samer_Hattar", "Hickstein_Robert": "Dennis_Hickstein", "DebraHope":"Debra_Hope", "StevenHou": "Steven_Hou", "DingJin": "Ding_Jin", "PeterJohnson": "Peter_Johnson", "AshishLal": "Ashish_Lal",
                    "Kraemer": "Kenneth_Kraemer", "Kreitman": "Robert_Kreitman", "Larson": "Dan_Larson", "Lowy": "Douglas_Lowy", "Pengnian": "Pengnian_Lin",
                    "Lipkowitz": "Stanley_Lipkowitz", "Meier": "Jordan_Meier", "Moscow": "Jeffrey_Moscow", "Neuman": "Maria_Merino_Neuman", "Oppenheim": "Joost_Oppenheim",
                    "Perantoni": "Alan_Perantoni", "Raznaha": "Armin_Raznaha", "Karlyne": "Karlyne_Reilly", "JohnShern": "John_Shern", "Schneider": "Joel_Schneider", "staudt": "Louis_Staudt",
                    "Staudt": "Louis_Staudt", "Soppet": "Daniel_Soppet", "Schrump": "David_Schrump", "Shrump": "David_Schrump", "Steeg": "Patricia_Steeg",
                    "Sterneck": "Esta_Sterneck", "Tessarollo": "Lino_Tessarollo", "Giorgio": "Giorgio_Trinchieri", "Electron": "Electron_Kabebew", "Hager": "Gordon_Hager", "Hunter": "Kent_Hunter", "KentHuter": "Kent_Hunter",
                    "Jonathan_Keller_Sun": "Jonathan_Keller", "Nagao": "Keisuke_Nagao", "Bustin": "Michael_Bustin", "Restifo": "Nicholas_Restifo",
                    "Philipp_Oberdoerffer_Kim": "Philipp_Oberdoerffer", "Xin_Wei_Wang": "Xin_Wang", "Pommier": "Yves_Pommier", "Vinson": "Chuck_Vinson",
                    "Batchelor": "Eric_Batchelor", "Brownell": "Issac_Brownell", "Ji_Luo": "Ji_Luo", "ShivGrewal": "Shiv_Grewal",
                    "Raffeld": "Mark_Raffeld", "Javed": "Javed_Khan",
                    "JingHuang": "Jing_Huang", "Aladjem": "Mirit_Aladjem", "Alajem": "Mirit_Aladjem", "Muegge": "Kathrin_Muegge", "Li_Yang": "Li_Yang",
                    "Thiele": "Carol_Thiele", "Bosselut": "Remy_Bosselut", "Frederick_Barr": "Frederick_Barr", "Trinchieri": "Giorgio_Trinchieri",
                    "Ripley": "Taylor_Ripley", "Alfred_Singer": "Alfred_Singer", "Sample_SPECS_2070": "Louis_Staudt", "Pastan": "Ira_Pastan",
                    "Merlino": "Glenn_Merlino", "Udayan": "Udayan_Guha", "LiYang": "Li_Yang", "Bhandoola":"Avinash_Bhandoola",
                    "Levens": "David_Levens", "SteveHughes": "Stephen_Hughes", "StephenHuges": "Stephen_Hughes", "Shalini": "Shalini_Oberdoerffer",
                    "Strathern": "Jeff_Strathern", "HonpingZheng": "Honping_Zheng", "Wakefield": "Lalage_Wakefield",
                    "LiWang": "Li_Wang", "Guerrerio": "Pamela_Guerrerio", "KathyKelly": "Kathy_Kelly", "ShuoGu": "Shuo_Gu",
                    "MarkGilbert": "Mark_Gilbert", "Yamini": "Yamini_Dalal", "AartiGautam": "Aarti_Gautam", "Hernandez": "Jonathan_Hernandez",
                    "DinahSinger": "Dinah_Singer", "Ried": "Thomas_Ried", "JingHuang": "Jing_Huang", "YingZhang": "Ying_Zhang",
                    "Nickerson": "Mike_Nickerson", "Brownell": "Issac_Brownell", "Jung-Min": "Jung-Min_Lee",
                    "PhilippOberdoerffer": "Philipp_Oberdoerffer", "Ambs": "Stefan_Ambs", "JackShern": "Jack_Shern", "Tofilon": "Philip_Tofilon",
                    "Doroshow": "James_Doroshow", "Alewine": "Christine_Alewine", "JonathanKeller": "Jonathan_Keller",
                    "HowardYoung": "Howard_Young", "Klinman": "Dennis_Klinman", "Dean": "Micheal_Dean",
                    "Pinto": "Ligia_Pinto", "Fountaine": "Thomas_Fountaine", "Rudloff": "Udo_Rudloff",
                    "Sowalsky": "Adam_Sowalsky", "Franchini": "Genoveffa_Franchini",
                    "Myong-Hee": "Myong-Hee_Sung", "Myong-hee": "Myong-hee_Sung", "YinlingHu": "Yinling_Hu", "Agdashian": "David_Agdashian",
                    "AlfredSinger": "Alfred_Singer", "Szabova": "Ludmila_Szabova", "XiWang":"Xi_Wang", "Gottesman": "Michael_Gottesman",
                    "Yuspa": "Stuart_Yuspa", "Roberts": "David_Roberts", "Mistelli": "Tom_Misteli", "Misteli": "Tom_Misteli",
                    "Tomozumi": "Tomozumi_Imamichi", "Raffit": "Raffit_Hassan", "Bartolome": "Ramiro_Iglesias-Bartolome",
                    "RobertWest_Dennis": "Robert_West", "RobertWest_CS": "Robert_West", "Citrin": "Deborah_Citrin", "XinWang": "Xin_Wang",
                    "Wolin": "Sandra_Wolin", "Chunzhang": "Chunzhang_Yang", "ChunZhang": "Chunzhang_Yang",
                    "VanderWeele": "David_VanderWeele", "Kylie": "Kylie_Walters", "Whitby" : "Denise_Whitby",
                    "Xiaolin": "Xiaolin_Wu", "Yamaguchi": "Terence_Yamaguchi", "Zhi-Ming": "Zhi-Ming_Zheng", "ZhiMing": "Zhi-Ming_Zheng",
                    "Ziegelbauer": "Joe_Ziegelbauer", "ZhengpingZhuang": "Zhengping_Zhuang"}

        pi_name = 'CCRSF'

        if log is True:
            logging.info("Getting pi_name from path: " + path)


        if 'Undetermined' in path or path.endswith('supplement.tar') or 'singlecell' in path:
            pi_name = 'SF_Archive_Flowcell_Info'

        elif 'NEBnext_UltraII' not in path and 'Neoprep' not in path \
                and 'testing' not in path and 'SEER' not in path:
            for element in (pi_names):
                if element in path:
                    #Perform mapping using pi_names if match is found
                    pi_name = pi_names[element]
                    break


            if 'CCRSF' in pi_name:

                # derive pi name
                path_elements = (path.split("/")[0]).split("_")

                # Assumes that PI name is in the beginning, and last and first names are separated by an '_'

                if len(path_elements) > 4 and path_elements[3].isalpha() and path_elements[4].isdigit():
                    # If the 4th is alpha, and 5th is a number, then pick the first 2
                    pi_name = path_elements[0] + "_" + path_elements[1]
                elif len(path_elements) > 2 and path_elements[1].isalpha() and path_elements[2].isdigit():
                    # If the 2nd is alpha, and 3rd is a number, then pick the first 2
                    pi_name = path_elements[0] + "_" + path_elements[1]
                #if len(path_elements) > 2 and path_elements[2].isalpha() and path_elements[2] not in ['RAS', 'cegx', 'swift']:
                    # else if the first 3 are alpha pick 0 and 2
                    #pi_name = path_elements[0] + "_" + path_elements[2]
                #else:
                    #if len(path_elements) > 1 and path_elements[1].isalpha():
                        # else if the first 2 are alpha, pick 0 and 1
                        #pi_name = path_elements[0] + "_" + path_elements[1]
                    #else:
                        #pi_name = path_elements[0]


            #Assumes that PI name is in the beginning, and the format is FirstnameLastname
            #pi_name = re.sub(r'([A-Z])', r' \1', path_elements[0])

        if log is True:
            logging.info("pi_name from " + path + " is " + pi_name)
        return pi_name


    @staticmethod
    def get_contact_name(path):

        # derive pi name
        #path_elements = path.split("_")
        path_elements = (path.split("/")[0]).split("_")

        # Assumes the contact name follows the PI name separated from it by a '_',

        # the contact last and first names are separated by an '_'
        if len(path_elements) > 4 and path_elements[3].isalpha() and path_elements[4].isdigit() and len(str(path_elements[4] is 5)):
            contact_name = path_elements[2] + "_" + path_elements[3]
        else:
            contact_name = None

        # the contact name format is FirstnameLastname
        #if path_elements[1].isalpha():
            #contact_name = re.sub(r'([A-Z])', r'_\1', path_elements[1])
        #else:
            #contact_name = ""

        return contact_name



    @staticmethod
    def get_project_id(path, log = True):

        if log is True:
            logging.info("Getting project_id from path: " + path)
        project_id = 'Unspecified'

        if 'Undetermined' not in path:
            #path_elements = path.split("_")
            path_elements = (path.split("/")[0]).split("_")

            #The project_id is the first string containing only digits. If this string
            #is not a 5 digit number then use default project_id
            for element in path_elements:
                if element.isdigit():
                    if len(str(element)) >= 5:
                        project_id = element
                    break

                #If there is a string of the format 'CSXXXXXX' immediately after the
                #name fields where 'XXXXXX' has only digits, that is the project_id
                if element.startswith('CS') and element[4:].isdigit():
                    project_id = element
                    break

            #Assumes that PI and contact names are in the format 'FirstnameLastname'
            #project_id = path_elements[2]

        if log is True:
            logging.info("project_id from " + path + " is " + project_id)
        return project_id


    @staticmethod
    def get_project_name(path, tarfile, ext = None):

        if 'Undetermined' in path or tarfile.endswith('supplement.tar') or 'singlecell' in tarfile or len(path.split("/")) == 1:
            project_name =  SFHelper.get_run_name(tarfile)
            #if 'Undetermined' in path and ext is not None:
                #project_name = project_name + '_' + ext



        else:
            # derive project name
            if len(path.split("/")) > 2:
                project_name = path.split("/")[-3]
            else:
                project_name = path.split("/")[0]
                #Hardcoded exclusion
                if(project_name == 'Sample_SPECS_2070'):
                    project_name = 'Staudt_Roland_49mRNA_11_2_15'


        if ext is not None and ext != 'Unaligned':
            project_name = project_name + '_' + ext
            logging.info("project_name from " + path + " and ext " + ext + " is " + project_name)
        else:
            logging.info("project_name from " + path + " is " + project_name)

        return project_name


    @staticmethod
    def get_sample_name(path):
        logging.info("Getting sample_name from path: " + path)

        if 'Sample_' not in path:
            #sample_name = 'Undetermined'
            #Use part of the file name i.e. upto '_S' for the sample_path
            file_name = path.rsplit("/", 1)[-1]
            sample_name = file_name.rsplit("_S", 1)[0]

        else:
            # derive sample name - first remove the filename part
            sample_path = path.rsplit("/", 1)[0]
            #Then get the sample name part
            sample_name = sample_path.split("Sample_")[-1]

        logging.info("sample_name from " + path + " is " + sample_name)
        return sample_name


    @staticmethod
    def get_flowcell_id(tarfile, log = True):

        if log is True:
            logging.info("Getting flowcell_id from tarfile: " + tarfile)

        #Rule: After the last underscore in tar filename
        #flowcell_str = tarfile.split(".")[0].split("_")[-1]
        flowcell_str = tarfile.split(".")[0].split("_")[3]
        flowcell_id = flowcell_str[1:len(flowcell_str)]

        if log is True:
            logging.info("Flowcell_id from tarfile: " + tarfile + " is " + flowcell_id)

        return flowcell_id


    @staticmethod
    def get_run_date(tarfile):
        #Rule: String before the first underscore in tar filename - in the form YYMMDD
        #Change to MM/DD/YY
        run_date_str = tarfile.split(".")[0].split("_")[0]
        run_date = datetime.strptime(run_date_str, "%y%m%d").strftime("%m-%d-%y")
        return run_date


    @staticmethod
    def get_run_name(tarfile):
        #Rule: String before the '.tar' in the tar filename
        run_name = tarfile.split(".")[0]
        # Remove '_supplement' from the project_name if present
        run_name = run_name.split("_supplement")[0]
        # Remove '_lane' from the project_name if present
        run_name = run_name.split("_lane")[0]
        return run_name


    @staticmethod
    def get_sequencing_platform(tarfile):
        sequencing_platform = 'Unspecified'
        #Rule: First letter after the first '_' (i.e. 2nd column) in the tar filename
        sequencing_platform_code = tarfile.rstrip().split('_')[1][0]
        if(sequencing_platform_code == 'N'):
            sequencing_platform = 'NextSeq'
        elif (sequencing_platform_code == 'J' or sequencing_platform_code == 'D'):
            sequencing_platform = 'HiSeq'
        else:
            flowcell_id = SFHelper.get_flowcell_id(tarfile)
            if re.match("(\d){8}-(\w){5}", flowcell_id):
                sequencing_platform = 'MiSeq'


        return sequencing_platform

    @staticmethod
    def get_sequencing_application_type(path):
        sequencing_application_type = 'Unspecified'
        if('RNA_' in path):
            sequencing_application_type = 'RNA'
        elif('Chip_' in path):
            sequencing_application_type = 'Chip'
        elif('exomelib' in path):
            sequencing_application_type = 'exomelib'


        return sequencing_application_type


