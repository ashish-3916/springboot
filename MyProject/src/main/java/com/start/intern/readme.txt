



RUN :
	- add the file to the project.
	- call the method findAllAnnotatedClassesInPackage passing the package_name as an argument from Main.
	- make sure to update a ROOT inside the classScanner.
	- run the project.

OUTPUT :
	- creates a new package which contains the annotated classes in hierarchy at the ROOT location.
	- outputs the dependency graph of the classes in form of adjacency list on the console.

TESTING :
	- Make a maven project.
	- Add the newly created package to the project.
	- Update the maven dependencies in pom.xml.
	- Run the application.

WORKING :
	- PART 1 -> (serialization) get classes from classPath.
		- add the required annotation to ClassPathScanningCandidateComponentProvider object.
		  (currently filters added : Component , Service , Controller , Autowired, Repository)
		- findCandidateComponents method gives all the required beans
	
	- part 2 -> (deserialization) making a new project.
		- created new files with directory assurance.
		- populating the files

			overview of a file

             -----------------------------------------------------------------------
            |    ----------------                                                   |
            |   | packageContent |                                                  |
            |    ----------------                                                   |
            |	                                                                    |
            |    ----------------                                                   |
            |   | libraryContent |                                                  |
            |    ----------------                                                   |
            |	                                                                    |
            |    ---------------                                                    |
            |   | classContent  |   {												|
            |    ---------------                                                    |
            |	                                                                    |
            |        --------------------------------------------------             |
            |       |                                                  |            |
            |       |                                                  |            |
            |       |                fieldContent                       |            |
            |       |                                                  |            |
            |       |                                                  |            |
            |        --------------------------------------------------	            |
            |	                                                                    |
            |        --------------------------------------------------             |
            |       |                                                  |            |
            |       |                                                  |            |
            |       |                constructorContent                |            |
            |       |                                                  |            |
            |       |                                                  |            |
            |        --------------------------------------------------	            |
            | }	                                                                    |
             -----------------------------------------------------------------------
		    
		    fileContent = packageContent + libraryContent + classContent + fieldContent + constructorContent
		 