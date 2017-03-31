# HPC_DME_APIs
NCI High Performance Computing Data Management Services Common APIs

Common command line utilities to access the API.

-Configuration:

I-Edit the following configuration files in this directory:

  1- ./test-conf

    - Edit the top level folder where collections will be registered "base-folder"
    - Edit the server name. 

  2- ./curl-conf

     If your are fimiliar with curl, add in a separate line all the arguments you would like to send with the REST request. 
     Otherwise, leave the file unchanged.



II- Define the environment variable HPC_DM_UTILS and let it point to the directory where this README.md is located.
    Export this varialbe in your ~/.bashrc or ~/.profile file

    source the functions script 

    For example:

      export HPC_DM_UTILS=/path/to/this/README.md

      source  $HPC_DM_UTILS/functions
