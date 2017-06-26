
This branch of tests requires disabling LDAB authentication before it runs.

To run this test, these pre-registered collections, dataObjects, and users should exist:


The attributes of these collections and dataObjects can be found at: 
/HPC_Data_Management/branches/hpc-prototype-dev/src/hpc/hpc-server/hpc-ws-rs-api/test/sampledata

Users:
dice_user_group_admin
dice_user_system_admin
dice_user
 
Password:
N/A - LDAP Authenticated
 
Collection path: /FNL_SF_Archive/dice_project_1
Data file path: /FNL_SF_Archive/dice_project_1/dice_object_1
Access:
dice_user_group_admin (WRITE)
dice_user_system_admin (OWN)
dice_user (NO ACCESS)
 
 
Collection path: /FNL_SF_Archive/dice_project_2
Metadata: Attached
Data file path: /FNL_SF_Archive/dice_project_2/dice_object_2
Access:
dice_user_group_admin (WRITE)
dice_user_system_admin (OWN)
dice_user (WRITE)


Collection path: /FNL_SF_Archive/dice_project_1/sub1
Data file path: /FNL_SF_Archive/dice_project_1/sub1/ object1
Data file path: /FNL_SF_Archive/dice_project_1/sub1/ object2
Access:
dice_user_group_admin (WRITE)
dice_user_system_admin (OWN)
dice_user (NO ACCESS)
 
 
Collection path: /FNL_SF_Archive/dice_project_2/sub2
Data file path: /FNL_SF_Archive/dice_project_2/sub2/ object1
Data file path: /FNL_SF_Archive/dice_project_2/sub2/ object2
Access:
dice_user_group_admin (WRITE)
dice_user_system_admin (OWN)
dice_user (WRITE)
