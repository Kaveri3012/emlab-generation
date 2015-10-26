#File and folder initiation
nameFile <- "TargetRoleVerification-FinancialReports"
analysisFolder <- "~/Desktop/emlabGen/output/"
analysisFolder <- paste(analysisFolder, nameFile, "/", sep="")
analysisFolder
setwd(analysisFolder)
analysisFile <- paste(nameFile, ".csv", sep="")
analysisFile
scaleFactor <- 1
filePrefix <- nameFile

#Read csv-file
bigDF <- read.csv(analysisFile)

#Clean csv-file
library(stringr)
#bigDF$runId <- str_replace(bigDF$runId,"\\..*","")
bigDF$runNumber <- bigDF$runId
# Some colnames start with "X.", get rid of this 
colnames(bigDF) = gsub("X\\.", "", colnames(bigDF))
# Get rid of periods at the start and end of the names
colnames(bigDF) = gsub("^\\.|\\.$", "", colnames(bigDF))
# Convert all periods into underscores
colnames(bigDF) = gsub("\\.", "_", colnames(bigDF))
summary(bigDF)



library(gridExtra)
library(TeachingDemos)
library(grid)
library(ggplot2)
library(reshape2)