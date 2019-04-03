# HPC_DME_APIs
NCI High Performance Computing Data Management Services Common APIs

Common command line utilities to access the API.

-Configuration:

NOTE: If you are using cygwin, please run this command form this directory to
make sure the Unix test file format is used : 

sed -i "s/\r$//" functions 

I-Make a copy of the following sample configuration files in this directory:

  1- ./hpcdme.properties-sample
 
    - Copy the sample file to make your own copy:
      cp hpcdme.properties-sample hpcdme.properties

    - In your hpcdme.properties file, edit:
        - The server name:hpc.server.url
        - The user name:hpc.user

        If you plan to use a default globus endpoint:
        - Edit:hpc.default.globus.endpoint 

  2- ./tokens/curl-conf-sample

     If your are fimiliar with curl, add in a separate line all the arguments
     you would like to send with the REST request.  Otherwise, leave the file
     unchanged.


II- Define the environment variable HPC_DM_UTILS and let it point to the
    directory where this README.md is located.  Export this varialbe in your
    ~/.bashrc or ~/.profile file and source the functions script 

    For example:

      export HPC_DM_UTILS=/path/to/this/README.md
      source  $HPC_DM_UTILS/functions

For more information, refer to the "Getting Started with DME CLU" wiki page (https://wiki.nci.nih.gov/x/go7RFg).