#tThis file can generate n number of trends in the form of a triangular distribution, whose mode follows the 
#sequence 

library(reshape)
library(ggplot2)
library(plyr)
library(triangle)


noOfTicks = 50 
noOfRepetitions = 120 
columnNames = as.character(seq.int(2014,2063)) 

setwd("~/emlab-generation/emlab-generation/src/main/resources/data/stochasticDemandNLandDE")

#For NL
start = 1
top = 1.01
max = 1.03
min = 0.99
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
demandNL<- matrix(nrow = noOfRepetitions, ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  demandNLGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  demandNL[j,1]=start
for(i in 2:noOfTicks){
  demandNL[j,i] <- demandNL[j,i-1]*demandNLGrowthRate[i]
}
  rowNames <- c(rowNames, paste('demandNL-',j,sep = ""))
}
colnames(demandNL) <- columnNames
write.csv(demandNL, file =paste('demandGrowthNL.csv'), quote = F, row.names = rowNames)

#For DE
start = 1 
top = 1.01
max = 1.03
min = 0.99
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
demandDE<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  demandDEGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  demandDE[j,1]=start
  for(i in 2:noOfTicks){
    demandDE[j,i] <- demandDE[j,i-1]*demandDEGrowthRate[i]
  }
  rowNames <- c(rowNames, paste('demandDE-',j,sep = ""))
}
colnames(demandDE) <- columnNames
write.csv(demandDE, file =paste('demandGrowthDE.csv'), quote = F, row.names = rowNames)

setwd("~/emlab-generation/emlab-generation/src/main/resources/data/stochasticFuelPrices")

#For Coal Price
start = 3.6 
top = 1.01
max = 1.05
min = 0.97
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
coalPrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  coalPriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  coalPrice[j,1]=start
  for(i in 2:noOfTicks){
    coalPrice[j,i] <- coalPrice[j,i-1]*coalPriceGrowthRate[i]
  }
  rowNames <- c(rowNames, paste('coalPrice-',j,sep = ""))
}
colnames(coalPrice) <- columnNames
write.csv(coalPrice, file =paste('coalPrice.csv'), quote = F, row.names = rowNames)


#For gas Price
#For gas Price Constant
#reference: https://ycharts.com/indicators/europe_natural_gas_price


setwd("~/emlab-generation/emlab-generation/src/main/resources/data/stochasticFuelPrices")
#For NL Constant
start = 4
top = 1.00
max = 1
min = 1
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
gasPrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  gasPriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  gasPrice[j,1]=start
  for(i in 2:noOfTicks){
    gasPrice[j,i] <- gasPrice[j,i-1]*gasPriceGrowthRate[i]
    
  }
  rowNames <- c(rowNames, paste('gasPrice-',j,sep = ""))
}
colnames(gasPrice) <- columnNames

#For NL High
start = 4
top = 1.04
max = 1.02
min = 1.00
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
gasPrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  gasPriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  gasPrice[j,1]=start
  for(i in 2:noOfTicks){
    gasPrice[j,i] <- gasPrice[j,i-1]*gasPriceGrowthRate[i]
    
  }
  rowNames <- c(rowNames, paste('gasPrice-',j,sep = ""))
}
colnames(gasPrice) <- columnNames
plot(gasPriceGrowthRate)
write.csv(gasPrice, file =paste('gasPriceHigh.csv'), quote = F, row.names = rowNames)

#For NL High
start = 4
top = 0.98
max = 1.00
min = 0.96
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
gasPrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  gasPriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  gasPrice[j,1]=start
  for(i in 2:noOfTicks){
    gasPrice[j,i] <- gasPrice[j,i-1]*gasPriceGrowthRate[i]
    
  }
  rowNames <- c(rowNames, paste('gasPrice-',j,sep = ""))
}
colnames(gasPrice) <- columnNames
write.csv(gasPrice, file =paste('gasPriceLow.csv'), quote = F, row.names = rowNames)


#For biomass Price
start = 4.5
top = 1.01
max = 1.05
min = 0.97
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
biomassPrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  biomassPriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  biomassPrice[j,1]=start
  for(i in 2:noOfTicks){
    biomassPrice[j,i] <- biomassPrice[j,i-1]*biomassPriceGrowthRate[i]

  }
  rowNames <- c(rowNames, paste('biomassPrice-',j,sep = ""))
}
colnames(biomassPrice) <- columnNames
write.csv(biomassPrice, file =paste('biomassPrice.csv'), quote = F, row.names = rowNames)

#For uranium Price
start = 1.29 
top = 1.00
max = 1.01
min = 0.99
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
uraniumPrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  uraniumPriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  uraniumPrice[j,1]=start
  for(i in 2:noOfTicks){
    uraniumPrice[j,i] <- uraniumPrice[j,i-1]*uraniumPriceGrowthRate[i]

  }
  rowNames <- c(rowNames, paste('uraniumPrice-',j,sep = ""))
}
colnames(uraniumPrice) <- columnNames
write.csv(uraniumPrice, file =paste('uraniumPrice.csv'), quote = F, row.names = rowNames)

#For lignite Price
start = 1.428
top = 1.00
max = 1.01
min = 0.99
noOfTicks = 50 
rowNames = c()
modeSeq = seq(start, top, length.out = noOfTicks)
lignitePrice<- matrix(nrow = noOfRepetitions , ncol = noOfTicks )
for(j in 1:noOfRepetitions){
  lignitePriceGrowthRate=rtriangle(noOfTicks,a=min,b=max, c=top)
  lignitePrice[j,1]=start
  for(i in 2:noOfTicks){
    lignitePrice[j,i] <- lignitePrice[j,i-1]*lignitePriceGrowthRate[i]
  }
  rowNames <- c(rowNames, paste('lignitePrice-',j,sep = ""))
}
colnames(lignitePrice) <- columnNames
write.csv(lignitePrice, file =paste('lignitePrice.csv'), quote = F, row.names = rowNames)


