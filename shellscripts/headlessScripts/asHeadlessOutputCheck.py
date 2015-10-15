#!/usr/bin/env python
# -*- coding: utf-8 *-*

import json
import glob
import re
import csv
import sys
import os.path


def check_that_dict_has_equal_length(resultDict):
    i = 0
    for key, value in resultDict.iteritems():
        if i == 0:
            length = len(value)
        if not length == len(value):
            print("Key of unequal length: " + key)
            return False
        i = i + 1
    return True


def read_query_of_runid(path, runName, runId, queryName, resultDict):
    filename = runId + "-" + queryName
    #print("queryName:" +queryName)
    filepath = path + runName + "/" + filename
    json_data = open(filepath, 'r')
    resultList = []
    line = json_data.readline()
    jsonLine = json.loads(line[:-2], encoding="ascii")
    #In case its a pure list of numbers or strings
    if len(jsonLine) == 1 and \
    isinstance(jsonLine[0], (int, float, long, complex, basestring)):
        while line:
            resultList.append(json.loads(line[:-2], encoding="ascii")[0])
            line = json_data.readline()
        resultDict[queryName] = resultList
        return resultDict
    wrapperCounter = 0
    tempJsonLine = jsonLine
    #Unwrapping the json lists, in case it is wrapped
    while (tempJsonLine is not None) and len(tempJsonLine) == 1 and \
     isinstance(tempJsonLine, (list)) and \
     (not isinstance(tempJsonLine[0][0], (basestring))):
        tempJsonLine = tempJsonLine[0]
        wrapperCounter = wrapperCounter + 1
    #Checking if it conforms to the standard and starting the dictionary.
    if tempJsonLine is None:
        return None
    elif len(tempJsonLine[0]) == 2:
        return read_standard_conform_key_value_pairs(tempJsonLine,
            wrapperCounter, runId, queryName, json_data, resultDict)
    else:
        #print("Non-Standard: " + queryName)
        raise NameError("Query returns in " + runId + " ," + queryName
        + " are not standard [Name, Value] for each tick!")
        #return read_nonstandard_conform_key_value_pairs(tempJsonLine,\
        # wrapperCounter,runId,queryName,json_data,resultDict)


def read_nonstandard_conform_key_value_pairs(tempJsonLine, wrapperCounter,
runId, queryName, json_data, resultDict):
    subElementNumber = 0
    for subelement in tempJsonLine:
        for subsubelement in subelement:
            if not len(subsubelement) == 2:
                print(wrapperCounter)
                print(subsubelement)
                raise NameError("Query returns in " + runId + " ,"
                + queryName + " are not [Name, Value] for each tick!")
            for content in subsubelement:
                if isinstance(content, list):
                    raise NameError("Query returns in " + runId + " ,"
                    + queryName + " are not [Name, Value] for each tick!")
            resultDict[(queryName + "_"
            + subsubelement[0]).encode("ascii")] = [subsubelement[1]]
            subElementNumber = subElementNumber + 1
    #Reading the rest of the file
    line = json_data.readline()
    while line:
        counter = 0
        tempJsonLine = json.loads(line[:-2], encoding="ascii")
        while counter < wrapperCounter:
            tempJsonLine = tempJsonLine[0]
            counter = counter + 1
        subElementCounter = 0
        for subelement in tempJsonLine:
            for subsubelement in subelement:
                if not len(subsubelement) == 2:
                    raise NameError("Query returns should be \
                    [Name, Value] for each tick!")
                for content in subsubelement:
                    if isinstance(content, list):
                        raise NameError("Query returns should be \
                        [Name, Value] for each tick!")
                resultDict[(queryName + "_"
                + subsubelement[0]).encode("ascii")].append(subsubelement[1])
                subElementCounter = subElementCounter + 1
        if not subElementCounter == subElementNumber:
            raise NameError("Number of subresults is not consistent!:" + queryName + ", " + ''.join(str(e) for e in tempJsonLine))
        line = json_data.readline()
    #print(resultDict)
    return resultDict


def read_standard_conform_key_value_pairs(tempJsonLine, wrapperCounter, runId,
queryName, json_data, resultDict):
    subElementNumber = 0
    for subelement in tempJsonLine:
        if not len(subelement) == 2:
            print(wrapperCounter)
            print(subelement)
            raise NameError("Query returns in " + runId + " ," + queryName
            + " are not [Name, Value] for each tick!")
        for content in subelement:
            if isinstance(content, list):
                raise NameError("Query returns in " + runId + " ," + queryName
                + " are not [Name, Value] for each tick!")
        resultDict[(queryName + "_"
        + subelement[0]).encode("ascii")] = [subelement[1]]
        subElementNumber = subElementNumber + 1
    #Reading the rest of the file
    line = json_data.readline()
    while line:
        counter = 0
        tempJsonLine = json.loads(line[:-2], encoding="ascii")
        while counter < wrapperCounter:
            tempJsonLine = tempJsonLine[0]
            counter = counter + 1
        subElementCounter = 0
        for subelement in tempJsonLine:
            if not len(subelement) == 2:
                raise NameError("Query returns should be [Name, Value] for \
                each tick!")
            for content in subelement:
                if isinstance(content, list):
                    raise NameError("Query returns should be [Name, Value]\
                     for each tick!")
            resultDict[(queryName + "_" +
            subelement[0]).encode("ascii")].append(subelement[1])
            subElementCounter = subElementCounter + 1
        if not subElementCounter == subElementNumber:
            raise NameError("Number of subresults is not consistent! " + queryName + ", " + ''.join(str(e) for e in tempJsonLine))
        line = json_data.readline()
    #print(resultDict)
    return resultDict


def find_query_names_in_directory_for_runId(path, runName, runId):
    listOfQueryPaths = glob.glob(path + runName + "/" + runId + "-*")
    #print(path)
    #print(runName)
    #print listOfQueryPaths
    #ERROR
    listOfQueries = []
    listOfTableQueries = []
    for query in listOfQueryPaths:
	#print("query is" + query)
        m = re.search('(?<={0}{1}/{2}-).*'.format(path, runName, runId), query)
        if not m.group(0).startswith("TABLE_"):
            listOfQueries.append(m.group(0))
        else:
            listOfTableQueries.append(m.group(0))
    listOfQueries.sort()
    listOfQueryPaths.sort()
    #return listOfQueries,listOfQueryPaths
    return listOfQueries, listOfTableQueries


def find_runIds_based_on_logfiles_and_runname(path, runName):
    listOfQueryPaths = glob.glob(path + runName + "/*.log")
    listOfRunIds = []
    for query in listOfQueryPaths:
        m = re.search('(?<={0}{1}/).*'.format(path, runName), query)
        n = re.sub("^(.*).log$", "\\1", m.group(0))
        listOfRunIds.append(n)
    return listOfRunIds


def read_runId_to_dictionary(path, runName, runId, ignoredQueries):
    print("Reading " + runId)
    queryNames, tableQueryNames = find_query_names_in_directory_for_runId(path,
    runName, runId)
    #print(queryNames)
    for ignoredQuery in ignoredQueries:
        queryNames.remove(ignoredQuery)
    resultDict = {}
    for queryName in queryNames:
        singleQueryResult = read_query_of_runid(path, runName, runId,
        queryName, resultDict)
        if singleQueryResult is not None:
            resultDict.update(singleQueryResult)
    if not check_that_dict_has_equal_length(resultDict):
        raise NameError("Results of uneven time step length in ." + runName)
    return resultDict


def write_first_runid_dictionary_to_csv(path, runName, runId,
resultDict, noOfTicks):
    queryNames = ["tick", "runId"]
    queryNames = set(queryNames)
    queryNames.update(resultDict.keys())
    with open(path + runName + ".csv", 'w') as csvfile:
        csvwriter = csv.DictWriter(csvfile, fieldnames=queryNames)
        headers = {}
        for n in csvwriter.fieldnames:
            headers[n] = n
        csvwriter.writerow(headers)
        i = 0
        while i < noOfTicks:
            singleTickDict = {}
            for key, value in resultDict.iteritems():
                singleTickDict.update({key: value[i]})
            singleTickDict.update({"tick": i})
            singleTickDict.update({"runId": runId})
            csvwriter.writerow(singleTickDict)
            i = i + 1


def write_following_runids_to_csv(path, runName, runId, resultDict, noOfTicks):
    queryNames = {}
    with open(path + runName + ".csv", 'rb') as csvtoupdate:
        dictReader = csv.DictReader(csvtoupdate)
        queryNames = dictReader.fieldnames
    with open(path + runName + ".csv", 'a') as csvfile:
        csvwriter = csv.DictWriter(csvfile, fieldnames=queryNames)
        i = 0
        while i < noOfTicks:
            singleTickDict = {}
            for key, value in resultDict.iteritems():
                singleTickDict.update({key: value[i]})
            singleTickDict.update({"tick": i})
            singleTickDict.update({"runId": runId})

            csvwriter.writerow(singleTickDict)
            i = i + 1


def write_csv_for_run_name(path, runName, ignoredQueries):
    runIds = find_runIds_based_on_logfiles_and_runname(path, runName)
    totalRunIdNo = len(runIds)
    j = 0
    for runId in runIds:
        resultDict = read_runId_to_dictionary(path, runName,
        runId, ignoredQueries)


def write_first_runid_dictionary_to_csv(path, runName, runId,
resultDict, noOfTicks):
    queryNames = ["tick", "runId"]
    queryNames = set(queryNames)
    queryNames.update(resultDict.keys())
    with open(path + runName + ".csv", 'w') as csvfile:
        csvwriter = csv.DictWriter(csvfile, fieldnames=queryNames)
        headers = {}
        for n in csvwriter.fieldnames:
            headers[n] = n
        csvwriter.writerow(headers)
        i = 0
        while i < noOfTicks:
            singleTickDict = {}
            for key, value in resultDict.iteritems():
                singleTickDict.update({key: value[i]})
            singleTickDict.update({"tick": i})
            singleTickDict.update({"runId": runId})
            csvwriter.writerow(singleTickDict)
            i = i + 1


def write_following_runids_to_csv(path, runName, runId, resultDict, noOfTicks):
    queryNames = {}
    with open(path + runName + ".csv", 'rb') as csvtoupdate:
        dictReader = csv.DictReader(csvtoupdate)
        queryNames = dictReader.fieldnames
    with open(path + runName + ".csv", 'a') as csvfile:
        csvwriter = csv.DictWriter(csvfile, fieldnames=queryNames)
        i = 0
        while i < noOfTicks:
            singleTickDict = {}
            for key, value in resultDict.iteritems():
                singleTickDict.update({key: value[i]})
            singleTickDict.update({"tick": i})
            singleTickDict.update({"runId": runId})
            csvwriter.writerow(singleTickDict)
            i = i + 1
            
def file_len(fname):
    with open(fname) as f:
        for i, l in enumerate(f):
            pass
    return i + 1

#Checks 1. Which RunIds are missing due to a missing log file. 
#Checks 2. Which RunIds have missing queries
#Checks 3. Which Queries are missing in the RunIds that have missing queries
def checkOutputFolderForIncompleteRuns(path, runName, noOfRuns, expectedNoOfTicks, ignoreRunIdsWithUnexpectedTicks):
    ignoredQueries=set()
    ignoredRunIds=set()
    runIdsWithUnexpectedNoOfTicks=set()
    missingRunIdsBasedOnLog=set()
    runIdDict = {} 
    maxNoOfQueries = 0
    for i in range(1,noOfRuns):
        runIdStr = runName + "-" + `i`
        #Checks 1. Which RunIds are missing due to a missing log file. 
        doesLogExist = os.path.isfile(path + runName + "/" + runIdStr + ".log")
        queries = glob.glob(path + runName + "/" + runIdStr + "-*")
        noOfQueries = len(queries)
        if doesLogExist:
            runIdDict[runIdStr] = noOfQueries
        else:
            missingRunIdsBasedOnLog.add(runIdStr)
            continue
        maxNoOfQueries = noOfQueries if noOfQueries >= maxNoOfQueries else maxNoOfQueries
        maxNoOfTicks = 0
        queryDict = {}
        for query in queries:
            noOfTicks = file_len(query)
            queryDict[query] = noOfTicks
            maxNoOfTicks = noOfTicks if noOfTicks >= maxNoOfTicks else maxNoOfTicks
        if maxNoOfTicks != expectedNoOfTicks:
            runIdsWithUnexpectedNoOfTicks.add(runIdStr)
            if ignoreRunIdsWithUnexpectedTicks:
                ignoredRunIds.add(runIdStr)
        incompleteQueryList={k:v for (k,v) in queryDict.items() if v < maxNoOfTicks}
        for k in incompleteQueryList.keys():
            ignoredQueries.add(k.rsplit("-", 1)[-1])
    listOfRunIdsMissingQueries={k:v for (k,v) in runIdDict.items() if v < maxNoOfQueries}
    for k in listOfRunIdsMissingQueries.keys():
        ignoredRunIds.add(k)
    return ignoredRunIds, ignoredQueries, runIdsWithUnexpectedNoOfTicks, missingRunIdsBasedOnLog
    #print("missingRunIdsBasedOnLog " + `missingRunIdsBasedOnLog`)
        #print(runIdStr + ": " + `doesLogExist` + ", " + `nrOfQueries`)

def write_csv_for_run_name(path, runName, ignoredRunIds, ignoredQueries):
    runIds = find_runIds_based_on_logfiles_and_runname(path, runName)
    runIdSet = set(runIds)
    runIdSet = runIdSet.difference(ignoredRunIds)
    runIds = list(runIdSet)
    #print("runIds" + `runIds`)
    #print("ignored: " + `ignoredRunIds`)
    #print(runIds)
    totalRunIdNo = len(runIds)
    print(totalRunIdNo)
    j = 0
    for runId in runIds:
        resultDict = read_runId_to_dictionary(path, runName,
        runId, ignoredQueries)
        #print(resultDict)
        noOfTicks = len(resultDict.items()[1][1])
        if j == 0:
            write_first_runid_dictionary_to_csv(path, runName, runId,
            resultDict, noOfTicks)
        else:
            write_following_runids_to_csv(path, runName, runId,
            resultDict, noOfTicks)
        j = j + 1
        percentage = 1.0 * j / totalRunIdNo * 100
        print((str(percentage) + "% done."))


def main(outputPath, runName, noOfRuns, noOfTicks, ignoreRunIdsWithUnexpectedTicks):
    if(not outputPath.endswith("/")):
        outputPath = outputPath + "/"
    ignoredRunIds, ignoredQueries, runIdsWithUnexpectedNoOfTicks, missingRunIdsBasedOnLog = checkOutputFolderForIncompleteRuns(outputPath, runName, noOfRuns, noOfTicks, ignoreRunIdsWithUnexpectedTicks)
    if len(ignoredRunIds) > 0:
        with open(outputPath + runName + "-" + "ignoredRunIds" + ".txt", 'w') as ignoredRunIdsOutput:
            for item in list(ignoredRunIds):
                ignoredRunIdsOutput.write("%s\n" % item)
    if len(ignoredQueries) > 0:
        with open(outputPath + runName + "-" +  "ignoredQueries" + ".txt", 'w') as ignoredQueriesOutput:
            for item in list(ignoredQueries):
                ignoredQueriesOutput.write("%s\n" % item)
    if len(missingRunIdsBasedOnLog) > 0:
        with open(outputPath + runName + "-" +  "missingRunIdsBasedOnLog" + ".txt", 'w') as missingRunIdsBasedOnLogOutput:
            for item in list(missingRunIdsBasedOnLog):
                missingRunIdsBasedOnLogOutput.write("%s\n" % item)
    if len(runIdsWithUnexpectedNoOfTicks) > 0:
        with open(outputPath + runName + "-" +  "runIdsWithUnexpectedNoOfTicks" + ".txt", 'w') as runIdsWithUnexpectedNoOfTicksOutput:
            for item in list(runIdsWithUnexpectedNoOfTicks):
                runIdsWithUnexpectedNoOfTicksOutput.write("%s\n" % item)
    write_csv_for_run_name(outputPath, runName, ignoredRunIds, ignoredQueries)
    if len(ignoredRunIds) > 0:
        if ignoreRunIdsWithUnexpectedTicks:
            print("WARNING, ignored runIds due to missing queries and unexpected tick length: " + `ignoredRunIds`)
        else: 
            print("WARNING, ignored runIds due to missing queries: " + `ignoredRunIds`)
    if len(ignoredQueries) > 0:
        print("WARNING, ignored queries due to missing ticks: " + `ignoredQueries`)
    if len(runIdsWithUnexpectedNoOfTicks) > 0:
        print("WARNING, thes runIds have an unexpected tick length: " + `runIdsWithUnexpectedNoOfTicks`)

if __name__ == "__main__":
    if len(sys.argv[1:]) == 5:
	#print("hello0")
        main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
    else:
        print("This script needs to be called with: outputPath, \
        runName, noOfRuns, noOfTicks, ignoreRunIdsWithUnexpectedTicks (True or False)")
