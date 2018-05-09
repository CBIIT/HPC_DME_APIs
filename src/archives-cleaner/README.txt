####################################################################################################
####################################################################################################

ARCHIVES CLEANER

####################################################################################################
####################################################################################################

Original Author:     William Y. Liu

Description:     A utility to clear base paths of specified DOCs especially for deletion of data 
                 generated during testing.  In this context, clear means to remove all contents
                 so that affected base path becomes empty.

Prerequisites:     Java, Maven

####################################################################################################

Configuration:

Under src/main/resources/, the various application-*.properties files may initially require editing 
to replace the "<Configure-Me>" placeholders with applicable values for database URL, database user
name, and database password.

Modifications of any configuration files under src/main/resources/ other than what is mentioned 
above is not recommended unless you are confident in what you are doing.  In that case, it may be
wise to make a backup copy of any configuration file prior to modifying it.

####################################################################################################

Usage:

Running with Maven and the Spring Boot Maven Plugin

   $> cd {HPC local Git repo}/src/archives-cleaner
   $> mvn clean compile
   $> mvn spring-boot:run [-Drun.arguments=DOC_X,DOC_Y,DOC_Z] [-Drun.profile={dev|uat|prod}]

   In spring-boot:run example line, there are various options shown.
   
   -Drun.arguments is to specify which DOCs have base paths that are desired to be cleared.  Its 
   value is interpreted as comma-separated list of DOCs.  By default, there is only 1 DOC to 
   target: TEST.
   
   -Drun.profile is to specify a Spring run profile associated with an environment/tier.  There
   are 3 possibilities: dev, uat, and prod.  The default run profile is dev.
   
   
   





