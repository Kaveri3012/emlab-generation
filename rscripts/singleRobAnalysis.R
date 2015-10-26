#File and folder initiation
nameFile <- "InvestmentCheck5"
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
drops <- c("CapacityinMWinA_CcgtCCS", "CapacityinMWinA_CoalPscCSS","CapacityinMWinA_HydroPower","CapacityinMWinA_IgccCCS","CapacityinMWinA_Igcc")
drops <- c("CapacityinMWinB_CcgtCCS", "CapacityinMWinB_CoalPscCSS","CapacityinMWinB_HydroPower","CapacityinMWinB_IgccCCS","CapacityinMWinA_Igcc")


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
bigDF$tick


library(gridExtra)
library(TeachingDemos)
library(grid)
library(ggplot2)
library(reshape2)

# bigDF$tick <- bigDF$tick + 2015
# bigDF$tick


# System Costs
# We define the system cost in all scenario as the payments that leave
# the circle of consumers, producers and the government, or equivalently as the sum of
# fixed and variable costs over the entire simulation period and over all power plants

# consumer costs (expenditures)
ConsumerCostsA <- bigDF$ConsumerExpenditure_Country_A_electricity_spot_market 
ProducerCostsA <- bigDF$CountryAProdCosts_Fixed_O_M + 
  bigDF$CountryAProdCosts_Loan + 
  bigDF$CountryAProdCosts_Commodity + 
  bigDF$CountryAProdCosts_Downpayment
GovernmentCostsA <- bigDF$CountryAProdFinances_Tender_Subsidy

SystemCostsA <- ConsumerCostsA + ProducerCostsA + GovernmentCostsA

ConsumerCostsB <- bigDF$ConsumerExpenditure_Country_B_electricity_spot_market 
ProducerCostsB <- bigDF$CountryBProdCosts_Fixed_O_M + 
  bigDF$CountryBProdCosts_Loan + 
  bigDF$CountryBProdCosts_Commodity + 
  bigDF$CountryBProdCosts_Downpayment
GovernmentCostsB <- bigDF$CountryBProdFinances_Tender_Subsidy

SystemCostsB <- ConsumerCostsB + ProducerCostsB + GovernmentCostsB



# Average electricity wholesale price in country
AverageElectricityPriceCountryAplot = ggplot(data=bigDF, aes(x=tick, y=Avg_El_PricesinEURpMWh_Country_A, group=runNumber)) + 
  geom_smooth() + (aes(colour = runNumber)) +
  xlab("Year") +  
  ylab("Eur/MWh") + 
  ggtitle("Average Electricity Prices \n The Netherlands") #give the plot a title
plot(AverageElectricityPriceCountryAplot)
ggsave(filename = paste(filePrefix, "Avg_el_price_A.png", sep=""))

AverageElectricityPriceCountryBplot = ggplot(data=bigDF, aes(x=tick, y=Avg_El_PricesinEURpMWh_Country_B, group=runNumber)) + 
  geom_smooth() +  (aes(colour = runNumber)) + 
  xlab("Year") +  
  ylab("Eur/MWh") + 
  ggtitle("Average Electricity Prices \n Germany") #give the plot a title
plot(AverageElectricityPriceCountryBplot)
ggsave(filename = paste(filePrefix, "Avg_el_price_B.png", sep=""))

#Tender Clearing Prices
tenderClearingPriceCountryAplot = ggplot(data=bigDF, aes(x=tick, y=tenderClearingPrice_Country_A, group=runNumber)) + 
  geom_smooth() +  (aes(colour = runNumber)) + 
  xlab("Year") +  
  ylab("Eur/MWh") + 
  ggtitle("Tender Clearing Prices \n Netherlands") #give the plot a title
plot(tenderClearingPriceCountryAplot)
ggsave(filename = paste(filePrefix, "tender_clearing_price_A.png", sep=""))

tenderClearingPriceCountryBplot = ggplot(data=bigDF, aes(x=tick, y=tenderClearingPrice_Country_B, group=runNumber)) + 
  geom_line() +  (aes(colour = runNumber)) + 
  xlab("Year") +  
  ylab("Eur/MWh") + 
  ggtitle("Tender Clearing Prices \n Germany") #give the plot a title
plot(tenderClearingPriceCountryBplot)
ggsave(filename = paste(filePrefix, "tender_clearing_price_B.png", sep=""))

#Tender Clearing Volumes
tenderClearingVolumeCountryAplot = ggplot(data=bigDF, aes(x=tick, y=tenderClearingVolume_Country_A, group=runNumber)) + 
  geom_line() +  (aes(colour = runNumber)) + 
  xlab("Year") +  
  ylab("MWh") + 
  ggtitle("Tender Clearing Volumes \n Netherlands") #give the plot a title
plot(tenderClearingVolumeCountryAplot)
ggsave(filename = paste(filePrefix, "tender_clearing_volume_A.png", sep=""))

tenderClearingVolumeCountryBplot = ggplot(data=bigDF, aes(x=tick, y=tenderClearingVolume_Country_B, group=runNumber)) + 
  geom_line() +  (aes(colour = runNumber)) + 
  xlab("Year") +  
  ylab("MWh") + 
  ggtitle("Tender Clearing Volumes \n Germany") #give the plot a title
plot(tenderClearingVolumeCountryBplot)
ggsave(filename = paste(filePrefix, "tender_clearing_volume_B.png", sep=""))

#Tender Clearing price*volume
tenderClearingMoneyA <- bigDF$tenderClearingPrice_Country_A * bigDF$tenderClearingVolume_Country_A
tenderClearingMoneyB <- bigDF$tenderClearingPrice_Country_B * bigDF$tenderClearingVolume_Country_B

write.table(tenderClearingMoneyA, file = "tenderClearingMoneyA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(tenderClearingMoneyB, file = "tenderClearingMoneyB.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")


# bigDF$diffPriceAB <- bigDF$Avg_El_PricesinEURpMWh_Country_A - bigDF$Avg_El_PricesinEURpMWh_Country_B
# bigDF$diffPriceAB
# diff_el_price_AB = ggplot(data=bigDF , aes(x=tick, y=diffPriceAB)) + 
#   geom_line() +  #(aes(colour = runNumber))
#   xlab("Year") +  
#   ylab("Eur/MWh") + 
#   ggtitle("Difference Electricity Price \n The Netherlands - Germany") #give the plot a title
# plot(diff_el_price_AB)
# 
# ggsave(filename = paste(filePrefix, "Diff_ElectricityPriceAverageNL_DE.png", sep=""))

# ElectricityPricesAB <-multiplot(AverageElectricityPriceCountryAplot, AverageElectricityPriceCountryBplot, diff_el_price_AB, cols=2)
# ggsave(filename = paste(filePrefix, "ElectricityPriceAverageNL_DE.png", sep=""))

#Relative Generation Share of Renewables
renewableGenerationA <-  
  bigDF$GenerationinMWhCountryA_Photovoltaic + bigDF$GenerationinMWhCountryA_Wind + bigDF$GenerationinMWhCountryA_Biomass + 
  bigDF$GenerationinMWhCountryA_HydroPower + bigDF$GenerationinMWhCountryA_Biogas + bigDF$GenerationinMWhCountryA_WindOffshore 

totalGenerationA  <-bigDF$GenerationinMWhCountryA_IGCC + 
  bigDF$GenerationinMWhCountryA_Photovoltaic + bigDF$GenerationinMWhCountryA_Wind + bigDF$GenerationinMWhCountryA_CcgtCCS + 
  bigDF$GenerationinMWhCountryA_CoalPscCSS + bigDF$GenerationinMWhCountryA_Lignite + bigDF$GenerationinMWhCountryA_Biomass + 
  bigDF$GenerationinMWhCountryA_HydroPower + bigDF$GenerationinMWhCountryA_IgccCCS + bigDF$GenerationinMWhCountryA_CoalPSC + 
  bigDF$GenerationinMWhCountryA_Biogas + bigDF$GenerationinMWhCountryA_CCGT + bigDF$GenerationinMWhCountryA_WindOffshore + 
  bigDF$GenerationinMWhCountryA_Nuclear + bigDF$GenerationinMWhCountryA_OCGT

renewableGenerationB <-  
  bigDF$GenerationinMWhCountryB_Photovoltaic + bigDF$GenerationinMWhCountryB_Wind + bigDF$GenerationinMWhCountryB_Biomass + 
  bigDF$GenerationinMWhCountryB_HydroPower + bigDF$GenerationinMWhCountryB_Biogas + bigDF$GenerationinMWhCountryB_WindOffshore 

totalGenerationB  <-bigDF$GenerationinMWhCountryB_IGCC + 
  bigDF$GenerationinMWhCountryB_Photovoltaic + bigDF$GenerationinMWhCountryB_Wind + bigDF$GenerationinMWhCountryB_CcgtCCS + 
  bigDF$GenerationinMWhCountryB_CoalPscCSS + bigDF$GenerationinMWhCountryB_Lignite + bigDF$GenerationinMWhCountryB_Biomass + 
  bigDF$GenerationinMWhCountryB_HydroPower + bigDF$GenerationinMWhCountryB_IgccCCS + bigDF$GenerationinMWhCountryB_CoalPSC + 
  bigDF$GenerationinMWhCountryB_Biogas + bigDF$GenerationinMWhCountryB_CCGT + bigDF$GenerationinMWhCountryB_WindOffshore + 
  bigDF$GenerationinMWhCountryB_Nuclear + bigDF$GenerationinMWhCountryB_OCGT

renewableGenerationShareCountryA <-renewableGenerationA/totalGenerationA
renewableGenerationShareCountryB <-renewableGenerationB/totalGenerationB

RESEGenerationShareTableA <- renewableGenerationShareCountryA
RESEGenerationShareTableB <- renewableGenerationShareCountryB

GenerationShareTableA
GenerationShareTableB

write.table(RESEGenerationShareTableA, file = "RESEgenerationShareTableA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(RESEGenerationShareTableB, file = "RESEgenerationShareTableB.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")

write.table(renewableGenerationA, file = "renewableGenerationA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(renewableGenerationB, file = "renewableGenerationB.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")

write.table(totalGenerationA, file = "totalGenerationA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(totalGenerationB, file = "totalGenerationB.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")


#Relative Capacity Share of Renewables
renewableCapacityA <-  
  bigDF$CapacityinMWinA_Photovoltaic + bigDF$CapacityinMWinA_Wind + bigDF$CapacityinMWinA_Biomass + 
  bigDF$CapacityinMWinA_HydroPower + bigDF$CapacityinMWinA_Biogas + bigDF$CapacityinMWinA_WindOffshore 

totalCapacityA  <-bigDF$CapacityinMWinA_IGCC + 
  bigDF$CapacityinMWinA_Photovoltaic + bigDF$CapacityinMWinA_Wind + bigDF$CapacityinMWinA_CcgtCCS + 
  bigDF$CapacityinMWinA_CoalPscCSS + bigDF$CapacityinMWinA_Lignite + bigDF$CapacityinMWinA_Biomass + 
  bigDF$CapacityinMWinA_HydroPower + bigDF$CapacityinMWinA_IgccCCS + bigDF$CapacityinMWinA_CoalPSC + 
  bigDF$CapacityinMWinA_Biogas + bigDF$CapacityinMWinA_CCGT + bigDF$CapacityinMWinA_WindOffshore + 
  bigDF$CapacityinMWinA_Nuclear + bigDF$CapacityinMWinA_OCGT

renewableCapacityB <-  
  bigDF$CapacityinMWinB_Photovoltaic + bigDF$CapacityinMWinB_Wind + bigDF$CapacityinMWinB_Biomass + 
  bigDF$CapacityinMWinB_HydroPower + bigDF$CapacityinMWinB_Biogas + bigDF$CapacityinMWinB_WindOffshore 

totalCapacityB  <-bigDF$CapacityinMWinB_IGCC + 
  bigDF$CapacityinMWinB_Photovoltaic + bigDF$CapacityinMWinB_Wind + bigDF$CapacityinMWinB_CcgtCCS + 
  bigDF$CapacityinMWinB_CoalPscCSS + bigDF$CapacityinMWinB_Lignite + bigDF$CapacityinMWinB_Biomass + 
  bigDF$CapacityinMWinB_HydroPower + bigDF$CapacityinMWinB_IgccCCS + bigDF$CapacityinMWinB_CoalPSC + 
  bigDF$CapacityinMWinB_Biogas + bigDF$CapacityinMWinB_CCGT + bigDF$CapacityinMWinB_WindOffshore + 
  bigDF$CapacityinMWinB_Nuclear + bigDF$CapacityinMWinB_OCGT

renewableCapacityShareCountryA <-renewableCapacityA/totalCapacityA
renewableCapacityShareCountryB <-renewableCapacityB/totalCapacityB

write.table(renewableCapacityShareCountryA, file = "renewableCapacityShareCountryA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(renewableCapacityShareCountryB, file = "renewableCapacityShareCountryB.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")


#supply ratio
# An indicator therefore, would be the the ratio of operational capacity to the peak demand,
# This indicator is measured as the ratio of Total Operational Capacity Per
# Zone(in MW) to Peak Demand Per Zone (in MW). A value of supply ratio below 1 would clearly indicate a shortage.
# namely the Supply Ratio

SupplyRatioA <- bigDF$TotalOperationalCapacityPerZoneInMW_Country_A/bigDF$PeakDemandPerZoneInMW_Country_A
SupplyRatioB <- bigDF$TotalOperationalCapacityPerZoneInMW_Country_B/bigDF$PeakDemandPerZoneInMW_Country_B

bigDF$TotalOperationalCapacityPerZoneInMW_Country_A
bigDF$PeakDemandPerZoneInMW_Country_A

write.table(SupplyRatioA, file = "SupplyRatioA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(SupplyRatioB, file = "SupplyRatioB.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
SupplyRatioA
SupplyRatioB

SupplyRatioGenerationA <- bigDF$Total_DemandinMWh_Country_A/bigDF$NationalTotalProductioninMWh_Country_A
SupplyRatioGenerationA <- bigDF$Total_DemandinMWh_Country_B/bigDF$NationalTotalProductioninMWh_Country_B

SupplyRatioGenerationA
SupplyRatioGenerationB

write.table(SupplyRatioGenerationA, file = "SupplyRatioGenerationA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")
write.table(SupplyRatioGenerationA, file = "SupplyRatioGenerationA.csv",row.names=FALSE, na="",col.names=FALSE, sep=",")

# #CapacityMargin Check
totalCapacityA <- bigDF$CapacityinMWinA_IGCC + 
  bigDF$CapacityinMWinA_Photovoltaic + bigDF$CapacityinMWinA_Wind + bigDF$CapacityinMWinA_CcgtCCS + 
  bigDF$CapacityinMWinA_CoalPscCSS + bigDF$CapacityinMWinA_Lignite + bigDF$CapacityinMWinA_Biomass + 
  bigDF$CapacityinMWinA_HydroPower + bigDF$CapacityinMWinA_IgccCCS + bigDF$CapacityinMWinA_CoalPSC + 
  bigDF$CapacityinMWinA_Biogas + bigDF$CapacityinMWinA_CCGT + bigDF$CapacityinMWinA_WindOffshore + 
  bigDF$CapacityinMWinA_Nuclear + bigDF$CapacityinMWinA_OCGT
totalCapacityA

totalCapacityB <- bigDF$CapacityinMWinB_IGCC + 
  bigDF$CapacityinMWinB_Photovoltaic + bigDF$CapacityinMWinB_Wind + bigDF$CapacityinMWinB_CcgtCCS + 
  bigDF$CapacityinMWinB_CoalPscCSS + bigDF$CapacityinMWinB_Lignite + bigDF$CapacityinMWinB_Biomass + 
  bigDF$CapacityinMWinB_HydroPower + bigDF$CapacityinMWinB_IgccCCS + bigDF$CapacityinMWinB_CoalPSC + 
  bigDF$CapacityinMWinB_Biogas + bigDF$CapacityinMWinB_CCGT + bigDF$CapacityinMWinB_WindOffshore + 
  bigDF$CapacityinMWinB_Nuclear + bigDF$CapacityinMWinB_OCGT
totalCapacityB


capacityMargingA <- (totalCapacityA/bigDF$PeakDemandPerZoneInMW_Country_A) - 1
capacityMargingA 

capacityMargingB <- (totalCapacityB/bigDF$PeakDemandPerZoneInMW_Country_B) - 1
capacityMargingB

# #TEST Writing Results onto a Data Table
# DataTable <- c(capacityFractionPeakDemandA)
# DataTable <- rbind(DataTable, c(OperationalCapacityFractionPeakDemandA))
# colnames(DataTable) <- c("Capacity_Fraction_A","Capacity_Fraction_B")
# rownames(DataTable) <- c("GenerationinMWh_Biomass","CapacityinMWh_OCGT","NationalTotalProductioninMWh_Country A Mean")
# write.csv(DataTable, "DataTableBaseCase.csv")
# save()


#Verification Aggregate profits: are the profits zero? If not, why not?
# table with profits

#Verification Expected RES-E Generation & Demand/Consumption
# Is expected generation in tick t equal to actual RES-E generation in tick t + 5 minus the generation target (= generation volume of bids being paid out)?
# Is expected consumption in tick t equal to actual demand in tick t + 5?

#RES-E generation mix: are targets met? (per country if there is no JointTarget) and if not why not?
# table with targets
# table realized res-e
# graph with generation mix


#Demand
demandCountryAplot = ggplot(data=bigDF, aes(x=tick, y=Total_DemandinMWh_Country_A, group=runNumber)) + 
  geom_line() + #(aes(colour = runNumber))
  xlab("Year") +  
  ylab("Demand (MWh)") + 
  ggtitle("Demand \n The Netherlands") #give the plot a title
plot(demandCountryAplot)
ggsave(filename = paste(filePrefix, "demand_A.png", sep=""))

demandCountryBplot = ggplot(data=bigDF, aes(x=tick, y=Total_DemandinMWh_Country_B, group=runNumber)) + 
  geom_line() + #(aes(colour = runNumber))
  xlab("Year") +  
  ylab("Demand (MWh)") + 
  ggtitle("Demand \n Germany") #give the plot a title
plot(demandCountryBplot)
ggsave(filename = paste(filePrefix, "demand_B.png", sep=""))


# Capacity mix
plotStackedCapacities <- function(df) {
  localEnv <- environment()
  technologyCapacities <- df[grepl( "CapacityinMWinA_" , names( df ))]
  colnames(technologyCapacities)
  moltenTechnologyCapacities <- melt(df, id.vars = "tick", measure.vars = colnames(technologyCapacities))
  stack <- ggplot(moltenTechnologyCapacities, aes(x = moltenTechnologyCapacities$tick, y = moltenTechnologyCapacities$value, fill = moltenTechnologyCapacities$variable, order = moltenTechnologyCapacities$variable),
                  environment = localEnv)+
    geom_area(position="stack")+
    guides(fill = guide_legend(reverse=TRUE, title = "Legend", ncol = 1, keywidth = .8, keyheight = .8))+
    ggtitle("Capacity mix The Netherlands")+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11))+
    scale_fill_discrete(name = "Legend",
                        breaks = colnames(technologyCapacities),
                        labels = substring(colnames(technologyCapacities), 17))+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Capacity (MW)")+
    theme(axis.title.y = element_text(size = 9, angle = 90),
          axis.title.x = element_text(size = 9, angle = 0),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "stackedCapacityPlotNL.png", sep=""),
         plot = stack, width=30, height=16.51, units="cm", scale=scaleFactor)}
plotStackedCapacities(bigDF)

plotStackedCapacities <- function(df) {
  localEnv <- environment()
  technologyCapacities <- df[grepl( "CapacityinMWinB_" , names( df ))]
  colnames(technologyCapacities)
  moltenTechnologyCapacities <- melt(df, id.vars = "tick", measure.vars = colnames(technologyCapacities))
  stack <- ggplot(moltenTechnologyCapacities, aes(x = moltenTechnologyCapacities$tick, y = moltenTechnologyCapacities$value, fill = moltenTechnologyCapacities$variable, order = moltenTechnologyCapacities$variable),
                  environment = localEnv)+
    geom_area(position="stack")+
    guides(fill = guide_legend(reverse=TRUE, title = "Legend", ncol = 1, keywidth = .8, keyheight = .8))+
    ggtitle("Capacity mix Germany")+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11))+
    scale_fill_discrete(name = "Legend",
                        breaks = colnames(technologyCapacities),
                        labels = substring(colnames(technologyCapacities), 17))+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Capacity (MW)")+
    theme(axis.title.y = element_text(size = 9, angle = 90),
          axis.title.x = element_text(size = 9, angle = 0),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "stackedCapacityPlotDE.png", sep=""),
         plot = stack, width=30, height=16.51, units="cm", scale=scaleFactor)}
plotStackedCapacities(bigDF)

# Generation Mix 
plotStackedGeneration <- function(df) {
  localEnv <- environment()
  technologyGeneration <- df[grepl( "GenerationinMWhCountryA_" , names( df ))]
  colnames(technologyGeneration)
  moltenTechnologyGeneration <- melt(df, id.vars = "tick", measure.vars = colnames(technologyGeneration))
  stack <- ggplot(moltenTechnologyGeneration, aes(x = moltenTechnologyGeneration$tick, y = moltenTechnologyGeneration$value, fill = moltenTechnologyGeneration$variable, order = moltenTechnologyGeneration$variable),
                  environment = localEnv)+
    geom_area(position="stack")+
    guides(fill = guide_legend(reverse=TRUE, title = "Legend", ncol = 1, keywidth = .8, keyheight = .8))+
    ggtitle("Generation mix The Netherlands")+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11))+
    scale_fill_discrete(name = "Legend",
                        breaks = colnames(technologyGeneration),
                        labels = substring(colnames(technologyGeneration), 17))+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Generation (MWh)")+
    theme(axis.title.y = element_text(size = 9, angle = 90),
          axis.title.x = element_text(size = 9, angle = 0),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "stackedGenerationPlotNL.png", sep=""),
         plot = stack, width=30, height=16.51, units="cm", scale=scaleFactor)}
plotStackedGeneration(bigDF)

plotStackedGeneration <- function(df) {
  localEnv <- environment()
  technologyGeneration <- df[grepl( "GenerationinMWhCountryB_" , names( df ))]
  colnames(technologyGeneration)
  moltenTechnologyGeneration <- melt(df, id.vars = "tick", measure.vars = colnames(technologyGeneration))
  stack <- ggplot(moltenTechnologyGeneration, aes(x = moltenTechnologyGeneration$tick, y = moltenTechnologyGeneration$value, fill = moltenTechnologyGeneration$variable, order = moltenTechnologyGeneration$variable),
                  environment = localEnv)+
    geom_area(position="stack")+
    guides(fill = guide_legend(reverse=TRUE, title = "Legend", ncol = 1, keywidth = .8, keyheight = .8))+
    ggtitle("Generation mix Germany")+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11))+
    scale_fill_discrete(name = "Legend",
                        breaks = colnames(technologyGeneration),
                        labels = substring(colnames(technologyGeneration), 17))+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Generation (MWh)")+
    theme(axis.title.y = element_text(size = 9, angle = 90),
          axis.title.x = element_text(size = 9, angle = 0),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "stackedGenerationPlotDE.png", sep=""),
         plot = stack, width=30, height=16.51, units="cm", scale=scaleFactor)}
plotStackedGeneration(bigDF)


# Tender subsidy costs per year: threshold is 150 MEuro per year
yearlyTenderSubsidyplotA = ggplot(data=bigDF, aes(x=tick, y=yearlyTotalTenderSubsidyCountryA_Tender_Subsidy_Yearly_Country_A, group=runNumber)) + 
  geom_line() + #(aes(colour = runNumber))
  xlab("Year") +  
  ylab("Eur") + 
  ggtitle("Tender Subsidy \n The Netherlands") #give the plot a title
plot(yearlyTenderSubsidyplotA)
ggsave(filename = paste(filePrefix, "yearlyTenderSubsidyplotA.png", sep=""))

yearlyTenderSubsidyplotB = ggplot(data=bigDF, aes(x=tick, y=yearlyTotalTenderSubsidyCountryB_Tender_Subsidy_Yearly_Country_B, group=runNumber)) + 
  geom_line() + #(aes(colour = runNumber))
  xlab("Year") +  
  ylab("Eur") + 
  ggtitle("Tender Subsidy \n Germany") #give the plot a title
plot(yearlyTenderSubsidyplotB)
ggsave(filename = paste(filePrefix, "yearlyTenderSubsidyplotB.png", sep=""))

#Producer Welfare
#Producer Cash
plotCashBalances <- function(df){
  localEnv <- environment()
  cashA <- df$ProducerCash_Energy_Producer_A
  cashB <- df$ProducerCash_Energy_Producer_B
  cashC <- df$ProducerCash_Energy_Producer_C
  cashD <- df$ProducerCash_Energy_Producer_D
  cashE <- df$ProducerCash_Energy_Producer_E
  cashF <- df$ProducerCash_Energy_Producer_F
  cashG <- df$ProducerCash_Energy_Producer_G
  cashH <- df$ProducerCash_Energy_Producer_H
  cashI <- df$ProducerCash_Energy_Producer_I
  prodCashPlot <- ggplot(df, aes(x=df$tick), environment = localEnv)+
    geom_line(aes(y=cashA, colour="cashA"))+
    geom_line(aes(y=cashB, colour="cashB"))+
    geom_line(aes(y=cashC, colour="cashC"))+
    geom_line(aes(y=cashD, colour="cashD"))+
    geom_line(aes(y=cashE, colour="cashE"))+
    geom_line(aes(y=cashF, colour="cashF"))+
    geom_line(aes(y=cashG, colour="cashG"))+
    geom_line(aes(y=cashH, colour="cashH"))+
    geom_line(aes(y=cashI, colour="cashI"))+
    ggtitle("Overview of producer cash balances")+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Cash balance (EUR)")+
    scale_colour_manual(name = "Legend", values = c(cashA = "green", cashB = "blue", cashC = "yellow", cashD = "seashell4",
                                                    cashE = "purple", cashF = "red", cashG = "pink", cashH = "black", cashH = "grey"),
                        labels = c(cashA = "Energy producer A",cashB = "Energy producer B",cashC = "Energy producer C",
                                   cashD = "Energy producer D",cashE = "Energy producer E",cashF = "Energy producer F",
                                   cashG = "Energy producer G",cashH = "Energy producer H",cashI = "Energy producer I"))+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11),
          axis.title.x = element_text(size = 9, angle = 0),
          axis.title.y = element_text(size = 9, angle = 90),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "prodCashBalancePlot.png", sep=""),
         plot = prodCashPlot, width=30, height=16.51, units="cm", scale=scaleFactor)
}
plotCashBalances(bigDF)

#Producer welfare = Producers costs - revenues = profit per producer over time, including Tender subsidy
plotProfitIncludingTenderSubsidy <- function(df){
  localEnv <- environment()
  profitA <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdA
  profitB <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdB
  profitC <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdC
  profitD <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdD
  profitE <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdE
  profitF <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdF
  profitG <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdG
  profitH <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdH
  profitI <- df$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdI
  prodProfitPlot <- ggplot(df, aes(x=df$tick), environment = localEnv)+
    geom_line(aes(y=profitA, colour="profitA"))+
    geom_line(aes(y=profitB, colour="profitB"))+
    geom_line(aes(y=profitC, colour="profitC"))+
    geom_line(aes(y=profitD, colour="profitD"))+
    geom_line(aes(y=profitE, colour="profitE"))+
    geom_line(aes(y=profitF, colour="profitF"))+
    geom_line(aes(y=profitG, colour="profitG"))+
    geom_line(aes(y=profitH, colour="profitH"))+
    geom_line(aes(y=profitI, colour="profitI"))+
    ggtitle("Overview of producer profit \n Including Tender Subsidy")+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Profit (EUR)")+
    scale_colour_manual(name = "Legend", values = c(profitA = "green", profitB = "blue", profitC = "yellow", profitD = "seashell4",
                                                    profitE = "purple", profitF = "red", profitG = "pink", profitH = "black" , profitI = "grey"),
                        labels = c(profitA = "Energy producer A",profitB = "Energy producer B",profitC = "Energy producer C",
                                   profitD = "Energy producer D",profitE = "Energy producer E",profitF = "Energy producer F",
                                   profitG = "Energy producer G",profitH = "Energy producer H",profitI = "Energy producer I"))+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11),
          axis.title.x = element_text(size = 9, angle = 0),
          axis.title.y = element_text(size = 9, angle = 90),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "prodProfitIncludingTenderSubsidyPlot.png", sep=""),
         plot = prodProfitPlot, width=30, height=16.51, units="cm", scale=scaleFactor)
}
plotProfitIncludingTenderSubsidy(bigDF)


#Producer welfare = Producers costs - revenues = profit per producer over time, excluding Tender subsidy
producerWelfareExcTenderA <- bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdA + bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdB + bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdC + bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdD
producerWelfareExcTenderB <- bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdA + bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdB + bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdC + bigDF$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdD

producerWelfareIncTenderA <- bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdA + bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdB + bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdC + bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdD
producerWelfareIncTenderB <- bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdA + bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdB + bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdC + bigDF$ProfitProducersYearlyIncludingTenderSubsidy_ProfitProdD

#Income Distribution
# check different profits of producer whether the tender makes them really skewed or not
# profits with and without subsidy
plotProfitExcludingTenderSubsidy <- function(df){
  localEnv <- environment()
  profitA <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdA
  profitB <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdB
  profitC <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdC
  profitD <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdD
  profitE <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdE
  profitF <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdF
  profitG <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdG
  profitH <- df$ProfitProducersYearlyExcludingTenderSubsidy_ProfitProdH
  prodProfitPlot <- ggplot(df, aes(x=df$tick), environment = localEnv)+
    geom_line(aes(y=profitA, colour="profitA"))+
    geom_line(aes(y=profitB, colour="profitB"))+
    geom_line(aes(y=profitC, colour="profitC"))+
    geom_line(aes(y=profitD, colour="profitD"))+
    geom_line(aes(y=profitE, colour="profitE"))+
    geom_line(aes(y=profitF, colour="profitF"))+
    geom_line(aes(y=profitG, colour="profitG"))+
    geom_line(aes(y=profitH, colour="profitH"))+
    ggtitle("Overview of producer profit \n Excluding Tender Subsidy")+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Profit (EUR)")+
    scale_colour_manual(name = "Legend", values = c(profitA = "green", profitB = "blue", profitC = "yellow", profitD = "seashell4",
                                                    profitE = "purple", profitF = "red", profitG = "pink", profitH = "black"),
                        labels = c(profitA = "Energy producer A",profitB = "Energy producer B",profitC = "Energy producer C",
                                   profitD = "Energy producer D",profitE = "Energy producer E",profitF = "Energy producer F",
                                   profitG = "Energy producer G",profitH = "Energy producer H"))+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11),
          axis.title.x = element_text(size = 9, angle = 0),
          axis.title.y = element_text(size = 9, angle = 90),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "prodProfitExcludingTenderSubsidyPlot.png", sep=""),
         plot = prodProfitPlot, width=30, height=16.51, units="cm", scale=scaleFactor)
}
plotProfitExcludingTenderSubsidy(bigDF)


#Producer yearly tender subsidies
plotTenderSubsidy <- function(df){
  localEnv <- environment()
  subsidyA <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_A
  subsidyB <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_B
  subsidyC <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_C
  subsidyD <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_D
  subsidyE <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_E
  subsidyF <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_F
  subsidyG <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_G
  subsidyH <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_H
  subsidyI <- df$TenderYearlySubsidyProducer_Tender_Subsidy_Yearly_Producer_I
  prodSubsidyPlot <- ggplot(df, aes(x=df$tick), environment = localEnv)+
    geom_line(aes(y=subsidyA, colour="subsidyA"))+
    geom_line(aes(y=subsidyB, colour="subsidyB"))+
    geom_line(aes(y=subsidyC, colour="subsidyC"))+
    geom_line(aes(y=subsidyD, colour="subsidyD"))+
    geom_line(aes(y=subsidyE, colour="subsidyE"))+
    geom_line(aes(y=subsidyF, colour="subsidyF"))+
    geom_line(aes(y=subsidyG, colour="subsidyG"))+
    geom_line(aes(y=subsidyH, colour="subsidyH"))+
    geom_line(aes(y=subsidyI, colour="subsidyI"))+
    ggtitle("Overview of producer Tender Subsidy")+
    scale_x_continuous(name = "Time (year)")+
    scale_y_continuous(name = "Subsidy (EUR)")+
    scale_colour_manual(name = "Legend", values = c(subsidyA = "green", subsidyB = "blue", subsidyC = "yellow", subsidyD = "seashell4",
                                                    subsidyE = "purple", subsidyF = "red", subsidyG = "pink", subsidyH = "black", subsidyI = "grey"),
                        labels = c(subsidyA = "Energy producer A",subsidyB = "Energy producer B",subsidyC = "Energy producer C",
                                   subsidyD = "Energy producer D",subsidyE = "Energy producer E",subsidyF = "Energy producer F",
                                   subsidyG = "Energy producer G",subsidyH = "Energy producer H", subsidyI = "Energy producer I"))+
    theme(plot.title = element_text(lineheight = 0.8, face = "bold", size = 11),
          axis.title.x = element_text(size = 9, angle = 0),
          axis.title.y = element_text(size = 9, angle = 90),
          legend.text = element_text(size = 8),
          legend.title = element_text(size = 10))
  ggsave(filename = paste(filePrefix, "prodSubsidyPlot.png", sep=""),
         plot = prodSubsidyPlot, width=30, height=16.51, units="cm", scale=scaleFactor)
}
plotTenderSubsidy(bigDF)

#Consumer Welfare = Difference in consumer expenditure at the beginning and end of simulation
# s1 <- subset(bigDF, tick==1)
# s2 <- subset(bigDF, tick==30)
# changeConsumerExpenditureA <- (s2$ConsumerExpenditure_Country_A_electricity_spot_market/1000000 - s1$ConsumerExpenditure_Country_A_electricity_spot_market/1000000)
# changeConsumerExpenditureA
# 
# pcchangeConsumerExpenditureA <- changeConsumerExpenditureA*100000000/s1$ConsumerExpenditure_Country_A_electricity_spot_market
# pcchangeConsumerExpenditureA
# meanPercentagechangeConsumerExpenditureA <- mean(pcchangeConsumerExpenditureA)
# meanPercentagechangeConsumerExpenditureA
# 
# changeConsumerExpenditureB <- (s2$ConsumerExpenditure_Country_B_electricity_spot_market/1000000 - s1$ConsumerExpenditure_Country_B_electricity_spot_market/1000000)
# changeConsumerExpenditureB
# 
# pcchangeConsumerExpenditureB <- changeConsumerExpenditureB*100000000/s1$ConsumerExpenditure_Country_B_electricity_spot_market
# pcchangeConsumerExpenditureB
# meanPercentagechangeConsumerExpenditureB <- mean(pcchangeConsumerExpenditureB)
# meanPercentagechangeConsumerExpenditureB

# ConsumerExpenditureA = ggplot(data=bigDF, aes(x=tick, y=ConsumerExpenditureA, group=runNumber)) + #use myDataFrame for the data, columns for x and y
#   geom_line(aes(colour = runNumber)) + #we want to use points, colored by runNumber
#   xlab("Year") +  #specify x and y labels
#   ylab("Expenditure (MEur)") + 
#   ggtitle("Consumer expenditure - The Netherlands") #give the plot a title
# plot(ConsumerExpenditureA)
# ggsave(filename = paste(filePrefix, "ConsumerExpenditureA.png", sep=""))
# 
# 
# ConsumerExpenditureB = ggplot(data=bigDF, aes(x=tick, y=ConsumerExpenditure_Country_B_electricity_spot_market, group=runNumber)) + #use myDataFrame for the data, columns for x and y
#   geom_line(aes(colour = runNumber)) + #we want to use points, colored by runNumber
#   xlab("Year") +  #specify x and y labels
#   ylab("Expenditure (Eur)") + 
#   ggtitle("Consumer expenditure - Germany") #give the plot a title
# plot(ConsumerExpenditureB)
# ggsave(ConsumerExpenditureB,  file="ConsumerExpenditureB.png")
# 
# meanConsA <- mean(ConsumerExpenditureAinMillions)
# meanConsB <- mean(ConsumerExpenditureBinMillions)
# differenceInChangeOfConsumerExpenditureAB = changeConsumerExpenditureA - changeConsumerExpenditureB
# differenceInChangeOfConsumerExpenditureAB
# # meanDiff <- mean(diff)
# 
# DataTable <- c(changeConsumerExpenditureA)
# DataTable <- rbind(DataTable, c(changeConsumerExpenditureB))
# DataTable <- rbind(DataTable, c(differenceInChangeOfConsumerExpenditureAB))
# colnames(DataTable) <- c(filePrefix)
# rownames(DataTable) <- c("Change_Consumer_Expenditure_NL (MEuro)","Change_Consumer_Expenditure_DE (MEuro)", "Diff_NL-DE (MEuro)")
# write.csv(DataTable, "DataTableConsumerExpenditure.csv")
# read.csv("DataTableConsumerExpenditure.csv")





# Total welfare = consumer welfare and producer welfare

#Market Value of RES-E (PV, Wind, WindOfshore)
# market value = Volume PV per segment * Sgement Clearing Price / generation in MWh in tick
# system base price = Average electricity price in tick
# value factor = market value / system base price

# #TEST mean of GenerationinMWh_Biomass
# GenerationinMWhBiomassMean = 0
# CapacityinMWhOCGTMean = 0
# NationalTotalProductioninMWhCountryAMean = 0
# 
# for(i in 1:50){
# i=1
# bigDFbc$runId <- paste(bigDFbc$runId,"-",sep="")
# strTemp <- paste("CmPaper-1",i,"-",sep="")
# Temp <-  subset(bigDFbc, grepl(strTemp, runId))
# Temp <- bigDFbc
# GenerationinMWhBiomassMean[i] <- mean(Temp$GenerationinMWh_Biomass)
# }
# GenerationinMWhBiomassMean
# 
# for(i in 1:50){
#   i=1
#   bigDFbc$runId <- paste(bigDFbc$runId,"-",sep="")
#   strTemp <- paste("CmPaper-1",i,"-",sep="")
#   Temp <-  subset(bigDFbc, grepl(strTemp, runId))
#   Temp <- bigDFbc
#   CapacityinMWhOCGTMean[i] <- mean(Temp$CapacityinMW_OCGT)
# }
# CapacityinMWhOCGTMean
# 
# for(i in 1:50){
#   i=1
#   bigDFbc$runId <- paste(bigDFbc$runId,"-",sep="")
#   strTemp <- paste("CmPaper-1",i,"-",sep="")
#   Temp <-  subset(bigDFbc, grepl(strTemp, runId))
#   Temp <- bigDFbc
#   NationalTotalProductioninMWhCountryAMean[i] <- mean(Temp$NationalTotalProductioninMWh_Country.A)
# }
# NationalTotalProductioninMWhCountryAMean
# 
# 
# 
# #TEST Writing Results onto a Data Table
# DataTable <- c(GenerationinMWhBiomassMean)
# DataTable <- rbind(DataTable, c(CapacityinMWhOCGTMean))
# DataTable <- rbind(DataTable, c(NationalTotalProductioninMWhCountryAMean))
# colnames(DataTable) <- c("BaseCase_Mean")
# rownames(DataTable) <- c("GenerationinMWh_Biomass","CapacityinMWh_OCGT","NationalTotalProductioninMWh_Country A Mean")
# write.csv(DataTable, "DataTableBaseCase.csv")
# save()

# #Multiplot function
# multiplot <- function(..., plotlist=NULL, file, cols=1, layout=NULL) {
#   require(grid)
#   
#   # Make a list from the ... arguments and plotlist
#   plots <- c(list(...), plotlist)
#   
#   numPlots = length(plots)
#   
#   # If layout is NULL, then use 'cols' to determine layout
#   if (is.null(layout)) {
#     # Make the panel
#     # ncol: Number of columns of plots
#     # nrow: Number of rows needed, calculated from # of cols
#     layout <- matrix(seq(1, cols * ceiling(numPlots/cols)),
#                      ncol = cols, nrow = ceiling(numPlots/cols))
#   }
#   
#   if (numPlots==1) {
#     print(plots[[1]])
#     
#   } else {
#     # Set up the page
#     grid.newpage()
#     pushViewport(viewport(layout = grid.layout(nrow(layout), ncol(layout))))
#     
#     # Make each plot, in the correct location
#     for (i in 1:numPlots) {
#       # Get the i,j matrix positions of the regions that contain this subplot
#       matchidx <- as.data.frame(which(layout == i, arr.ind = TRUE))
#       
#       print(plots[[i]], vp = viewport(layout.pos.row = matchidx$row,
#                                       layout.pos.col = matchidx$col))
#     }
#   }
# }
# 
# pSA_1 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_1, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 1") #give the plot a title
# plot(pSA_1)
# 
# pSA_2 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_2, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 2") #give the plot a title
# plot(pSA_2)
# 
# pSA_3 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_3, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 3") #give the plot a title
# plot(pSA_3)
# 
# pSA_4 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_4, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 4") #give the plot a title
# plot(pSA_4)
# 
# pSA_5 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_5, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 5") #give the plot a title
# plot(pSA_5)
# 
# pSA_6 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_6, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 6") #give the plot a title
# plot(pSA_6)
# 
# pSA_7 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_7, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 7") #give the plot a title
# plot(pSA_7)
# 
# pSA_8 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_8, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 8") #give the plot a title
# plot(pSA_8)
# 
# pSA_9 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_9, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 9") #give the plot a title
# plot(pSA_9)
# 
# pSA_10 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_10, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 10") #give the plot a title
# plot(pSA_10)
# 
# pSA_11 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_11, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 11") #give the plot a title
# plot(pSA_11)
# 
# pSA_12 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_12, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 12") #give the plot a title
# plot(pSA_12)
# 
# pSA_13 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_13, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 13") #give the plot a title
# plot(pSA_13)
# 
# pSA_14 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_14, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 14") #give the plot a title
# plot(pSA_14)
# 
# pSA_15 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_15, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 15") #give the plot a title
# plot(pSA_15)
# 
# pSA_16 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_16, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 16") #give the plot a title
# plot(pSA_16)
# 
# pSA_17 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_17, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 17") #give the plot a title
# plot(pSA_17)
# 
# pSA_18 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_18, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 18") #give the plot a title
# plot(pSA_18)
# 
# pSA_19 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_19, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 19") #give the plot a title
# plot(pSA_19)
# 
# pSA_20 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_A_20, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 20") #give the plot a title
# plot(pSA_20)
# 
# pSB_1 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_1, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 1") #give the plot a title
# plot(pSB_1)
# 
# pSB_2 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_2, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 2") #give the plot a title
# plot(pSB_2)
# 
# pSB_3 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_3, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 3") #give the plot a title
# plot(pSB_3)
# 
# pSB_4 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_4, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 4") #give the plot a title
# plot(pSB_4)
# 
# pSB_5 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_5, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 5") #give the plot a title
# plot(pSB_5)
# 
# pSB_6 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_6, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 6") #give the plot a title
# plot(pSB_6)
# 
# pSB_7 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_7, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 7") #give the plot a title
# plot(pSB_7)
# 
# pSB_8 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_8, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 8") #give the plot a title
# plot(pSB_8)
# 
# pSB_9 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_9, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 9") #give the plot a title
# plot(pSB_9)
# 
# pSB_10 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_10, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 10") #give the plot a title
# plot(pSB_10)
# 
# pSB_11 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_11, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 11") #give the plot a title
# plot(pSB_11)
# 
# pSB_12 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_12, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 12") #give the plot a title
# plot(pSB_12)
# 
# pSB_13 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_13, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 13") #give the plot a title
# plot(pSB_13)
# 
# pSB_14 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_14, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 14") #give the plot a title
# plot(pSB_14)
# 
# pSB_15 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_15, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 15") #give the plot a title
# plot(pSB_15)
# 
# pSB_16 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_16, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 16") #give the plot a title
# plot(pSB_16)
# 
# pSB_17 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_17, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 17") #give the plot a title
# plot(pSB_17)
# 
# pSB_18 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_18, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 18") #give the plot a title
# plot(pSB_18)
# 
# pSB_19 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_19, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 19") #give the plot a title
# plot(pSB_19)
# 
# pSB_20 = 
#   ggplot(data=bigDF, aes(x=tick, y=bigDF$PriceInEURperMWh_Segment_Country_B_20, group=runNumber)) + 
#   geom_line() + (aes(colour = runNumber)) +
#   xlab("Tick") +  
#   ylab("Price (EUR/MWh)") + 
#   ggtitle("Segment 20") #give the plot a title
# plot(pSB_20)
# 
# 
# 
# segmentPricesA <-multiplot(pSA_1, pSA_2, pSA_3, pSA_4, pSA_5, pSA_6, pSA_7, pSA_8, pSA_9, pSA_10, pSA_11, pSA_12, pSA_13, pSA_14, pSA_15, pSA_16, pSA_17, pSA_18, pSA_19, pSA_20, cols=5)
# #ggsave(filename = paste(filePrefix, "segmentPricesA.png", sep=""), plot=segmentPricesA,width=30, height=16.51, units="cm", scale=1)
# 
# segmentPricesB <-multiplot(pSB_1, pSB_2, pSB_3, pSB_4, pSB_5, pSB_6, pSB_7, pSB_8, pSB_9, pSB_10, pSB_11, pSB_12, pSB_13, pSB_14, pSB_15, pSB_16, pSB_17, pSB_18, pSB_19, pSB_20, cols=5)
# #ggsave(filename = paste(filePrefix, "segmentPricesB.png", sep=""), plot=segmentPricesA,width=30, height=16.51, units="cm", scale=1)

