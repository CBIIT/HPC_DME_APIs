####################################################################################################
####################################################################################################

ARCHIVES CLEANER

####################################################################################################
####################################################################################################

Original Author:     William Y. Liu  
                     william.liu2@nih.gov

Description:     A utility to clear base paths of specified DOCs especially for deletion of data 
                 generated during testing.  In this context, clear means to remove all contents
                 so that affected base path becomes empty.

Prerequisites:     Java, Maven

Revision History:

Version       Date           Author                        Comments
              (YYYY-MM-DD)
----------------------------------------------------------------------------------------------------
1.0-BETA      2018-05-09     William Y. Liu                Initial version
                             william.liu2@nih.gov

####################################################################################################

Configuration:

Under src/main/resources/, the various application-*.properties files may initially require editing 
to replace the "<Configure-Me>" placeholders with applicable values for database URL, database user
name, and database password.

Modifications of any configuration files under src/main/resources/ other than what is mentioned 
above is not recommended unless you are confident in what you are doing.  It may be wise to make a
backup copy of any configuration file prior to modifying it.

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
   
   
Running as Executable JAR (alternative to running with Maven)

   $> cd {HPC local Git repo}/src/archives-cleaner
   $> mvn clean package [-Dmaven.test.skip=true]
   $> java -jar target/archives-cleaner-{version #}.jar

   For "mvn clean package ..." line above, -Dmaven.test.skip=true indicates to skip compilation and
   execution of automated tests.  Use this if tests have failures but you want to proceed with 
   packaging anyway.
   
   For "java -jar ..." line above, {version #} is placeholder for whichever version specifier is 
   applicable.
