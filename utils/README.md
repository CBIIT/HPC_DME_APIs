# HPC_DME_APIs
NCI High Performance Computing Data Management Services Common APIs

Common command line utilities to access the API.

-Configuration:

I-Edit the following configuration files in this directory:

  1- ./hpcdme.properties

    - Edit the server name:hpc.server.url
    - Edit the user name:hpc.user

    If you plan to use a default globus endpoint:
    - Edit:hpc.default.globus.endpoint 

  2- ./tokens/curl-conf

     If your are fimiliar with curl, add in a separate line all the arguments
     you would like to send with the REST request.  Otherwise, leave the file
     unchanged.



II- Define the environment variable HPC_DM_UTILS and let it point to the
    directory where this README.md is located.  Export this varialbe in your
    ~/.bashrc or ~/.profile file and source the functions script 

    For example:

      export HPC_DM_UTILS=/path/to/this/README.md

      source  $HPC_DM_UTILS/functions
