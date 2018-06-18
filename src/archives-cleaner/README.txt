Archives Cleaner Application

NOTES
1.  3 Maven profiles named dev, uat, and prod correspond to environment tiers.

2.  Program arguments are treated as DOCs whose base paths you wish to clean.  Without any 
    program arguments, application assumes the only DOC whose base paths you wish to clean is TEST.

3.  Customize application settings in application.properties file.

* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
To run with Spring Boot, do:

  $> cd [<path-elements>]\archives-cleaner
  $> mvn spring-boot:run [-Dspring-boot.run.profiles={dev|uat|prod}] \
  $>                     [-Dspring-boot.run.arguments=<comma-separated list of DOCs whose \
  $>                                                   base paths you wish to clean>]
  
  Example: 
  $> mvn spring-boot:run -Dspring-boot.run.profiles=uat -Dspring-boot.run.arguments=TEST,FNLCR


* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
To run as executable JAR, do:

  $> cd [<path-elements>]\archives-cleaner
  $> mvn clean package -P{dev|uat|prod}
  $> java -jar target\archives-cleaner-<VERSION>.jar [list of DOCs whose base paths you wish to 
  $>                                                  clean]
  
  Example:
  $> mvn clean package -Pprod
  $> java -jar target\archives-cleaner-1.0.jar TEST FNLCR
