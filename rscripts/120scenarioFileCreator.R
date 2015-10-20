#Placeholders

# Step 1 building the scenarios: insert dataframe and read the scenario file. Name parameters
# that need to be replaced with #
xmlFilePath<-"~/Desktop/emlabGen/scenarios/RDJ-RT-TC-ThreeMultiNodes-Template.xml"
filestump<-'RDJ-RT-TC-ThreeMultiNodes-'
# Step 2 building the scenarios: make separate data vectors
noOfRepetitions = 120 
for(runID in seq(1:noOfRepetitions))
  {
  xmlFileContent<-readLines(xmlFilePath, encoding = "UTF-8")
  xmlFileContent<-gsub("#repetitionNumber", runID, xmlFileContent)
  writeLines(xmlFileContent, paste("~/Desktop/emlabGen/scenarios/RDJ-RT-TC-ThreeMultiNodes-Template/", filestump, runID, ".xml", sep=""))
  }

xmlFilePath<-"~/Desktop/emlabGen/scenarios/RDJ-RT-TC-OneMultiNodes-Template.xml"
filestump<-'RDJ-RT-TC-OneMultiNodes-'
# Step 2 building the scenarios: make separate data vectors
noOfRepetitions = 120 
for(runID in seq(1:noOfRepetitions))
{
  xmlFileContent<-readLines(xmlFilePath, encoding = "UTF-8")
  xmlFileContent<-gsub("#repetitionNumber", runID, xmlFileContent)
  writeLines(xmlFileContent, paste("~/Desktop/emlabGen/scenarios/RDJ-RT-TC-OneMultiNodes-Template/", filestump, runID, ".xml", sep=""))
}
