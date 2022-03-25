# PRIAM

## Short description
-
  ### What is the problem ? 
    The massive collection and use of personal data requires a reflection on the ethics of the practices developed in this field. A provision of the European Parliament of May 2016 (GDPR) summarizes the problem of personal data protection. Unfortunately, the main current practice in online applications is to send the personal data collected directly to the service provider. The terms of use signed by the user make the provider the owner of the collected data and give her/him the possibility to use it for derivative purposes.
-
  ### The solution 
  To propose a solution in which personal data remains strictly the property of the users. This solution will allow users to have full control over the use of their data, granting access to certain types of data to providers of their choice for specific services.
  
 ## Project roadmap
  Currently the project proposes a DSL (Domain Specific Language) to help software developers in their annotation of personal data and management of user consent and rights. It currently provide the following features: 
  - Annotate and identify all Personal Data.
  - Annotate all Applications’ Processings. 
  - Save Datasubjects’ consents and generate their Digital Contracts.
  - Allow the datasubject to make a request allowing her/him to enforce a right on her/his data and the provider to respond to the request.

 ## The used technologies
  ### To develop our DSL, we used: 
    - MPS JetBrains.
  ### To test the solution, you need: 
    - IntelliJ IDEA.
    - MySQL DBMS.
## Deployment
-
  ### Create database
     1.In MySQL create a new database “nutritionalCoaching”.
    
     2.Import the nutritionalCoachingDB.sql file.
-
  ### Integration of PRIAM_GDPR
    1.Open the “PRIAM_GDPR.zip” in Intellij idea.
    
    2.Add link to data source “nutritionalCoaching” in Intellij:
                ![DatabaseIntellij1](https://user-images.githubusercontent.com/72026369/160208422-ce6454ea-bf88-4210-adb7-11d9ac0f3f8f.jpg)
                
    3.Fill in the fields "user", "password" and the name of the database ("nutritionalCoaching").
                         ![addDatabaseIntellij](https://user-images.githubusercontent.com/72026369/160208273-3f5ba579-731a-47a4-8753-27ce2d57af40.jpg)
                         
    4.In the "Project" menu (on the left of the Intellij window), go to the folder:
          “PRIAM_GDPR/languages/GDPR_V1/sandbox/source_gen/PRIAM_LANGUAGE/sandbox”.
    
    5.Execute sql files in this order: 
    
          - “PRIAM_GDPR.sql” (Integrate confidentiality).
          - “Data type.sql” and “Definition of personal data categories.sql” (Add data type and personal data categories). 
          - “PersonalDataAnnotation” and “NonPersonalDataAnnotation” (Personal and non personal data annotation).
          - “Morphological Profiling.sql”, “Prospection.sql” and “Registration.sql” (Processing annotation).
          - “Anais.sql”, “Liliane.sql” (Add data subjects).
          - “ContractAnais.sql” (Generate digital contract).
          - “Anaisdata_of_birthrectification.sql” (Add rectification request).
          - “Answer0.sql” (Provider response).

  ## Note
    you can integrate PRIAM_GDPR in any  application, for that:
    
          - Import PRIAM_GDPR into MPS JetBrains.
          - Adapt "sandbox" to your application.
