/*******************************************************************************
 * Copyright 2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package emlab.gen.role.investment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.transaction.annotation.Transactional;

import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.util.GeometricTrendRegression;

/**
 * @author JCRichstein
 *
 */
@Configurable
@NodeEntity
public class TargetInvestmentRole extends GenericInvestmentRole<TargetInvestor> {

    @Transient
    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(TargetInvestor targetInvestor) {

        for (PowerGeneratingTechnologyTarget target : targetInvestor.getPowerGenerationTechnologyTargets()) {
            PowerGeneratingTechnology pgt = target.getPowerGeneratingTechnology();
            long futureTimePoint = getCurrentTick() + pgt.getExpectedLeadtime() + pgt.getExpectedPermittime();
            double expectedInstalledCapacity = reps.powerPlantRepository
                    .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(
                            targetInvestor.getInvestorMarket(), pgt, futureTimePoint);
            double pgtNodeLimit = Double.MAX_VALUE;
            // For simplicity using the market, instead of the node here. Needs
            // to be changed, if more than one node per market exists.
            PowerGeneratingTechnologyNodeLimit pgtLimit = reps.powerGeneratingTechnologyNodeLimitRepository
                    .findOneByTechnologyAndMarket(pgt, targetInvestor.getInvestorMarket());
            if (pgtLimit != null) {
                pgtNodeLimit = pgtLimit.getUpperCapacityLimit(futureTimePoint);
            }
            double targetCapacity;
            if (!targetInvestor.isRelativeInvestor()) {
                targetCapacity = target.getTrend().getValue(futureTimePoint);
            } else {
                double expectedDemand = predictDemandForElectricitySpotMarket(targetInvestor.getInvestorMarket(),
                        targetInvestor.getNumberOfYearsBacklookingForForecasting(), futureTimePoint);
                PowerPlant calculationPlant = new PowerPlant();
                calculationPlant.specifyNotPersist(getCurrentTick() - 6, targetInvestor, reps.powerGridNodeRepository
                        .findFirstPowerGridNodeByElectricitySpotMarket(targetInvestor.getInvestorMarket()), pgt);
                calculationPlant.setActualNominalCapacity(1);
                double productionHoursPerYear = 0;
                long numberOfSegments = reps.segmentRepository.count();
                double totalNumberOfHoursAYear = 0;
                double totalDemand = 0;
                for (SegmentLoad segmentLoad : reps.segmentLoadRepository.findAll()) {
                    productionHoursPerYear += calculationPlant.getCapacityFactorForSegment(segmentLoad.getSegment(),
                            numberOfSegments) * segmentLoad.getSegment().getLengthInHours();
                    totalNumberOfHoursAYear += segmentLoad.getSegment().getLengthInHours();
                    totalDemand = segmentLoad.getBaseLoad() * expectedDemand
                            * segmentLoad.getSegment().getLengthInHours();
                }
                targetCapacity = totalDemand / productionHoursPerYear;

            }

            double installedCapacityDeviation = 0;
            if (pgtNodeLimit > targetCapacity) {
                installedCapacityDeviation = targetCapacity - expectedInstalledCapacity;
            } else {
                installedCapacityDeviation = pgtNodeLimit - expectedInstalledCapacity;
            }

            if (installedCapacityDeviation > 0 && installedCapacityDeviation > pgt.getCapacity()) {

                double powerPlantCapacityRatio = installedCapacityDeviation / pgt.getCapacity();

                PowerPlant plant = new PowerPlant();
                plant.specifyNotPersist(getCurrentTick(), targetInvestor, reps.powerGridNodeRepository
                        .findFirstPowerGridNodeByElectricitySpotMarket(targetInvestor.getInvestorMarket()), pgt);
                plant.setActualNominalCapacity(pgt.getCapacity() * powerPlantCapacityRatio);
                PowerPlantManufacturer manufacturer = reps.genericRepository.findFirst(PowerPlantManufacturer.class);
                BigBank bigbank = reps.genericRepository.findFirst(BigBank.class);

                double investmentCostPayedByEquity = plant.getActualInvestedCapital()
                        * (1 - targetInvestor.getDebtRatioOfInvestments()) * powerPlantCapacityRatio;
                double investmentCostPayedByDebt = plant.getActualInvestedCapital()
                        * targetInvestor.getDebtRatioOfInvestments() * powerPlantCapacityRatio;
                double downPayment = investmentCostPayedByEquity;
                createSpreadOutDownPayments(targetInvestor, manufacturer, downPayment, plant);

                double amount = determineLoanAnnuities(investmentCostPayedByDebt, plant.getTechnology()
                        .getDepreciationTime(), targetInvestor.getLoanInterestRate());
                // logger.warn("Loan amount is: " + amount);
                Loan loan = reps.loanRepository.createLoan(targetInvestor, bigbank, amount, plant.getTechnology()
                        .getDepreciationTime(), getCurrentTick(), plant);
                // Create the loan
                plant.createOrUpdateLoan(loan);

            }
        }

    }

    private void createSpreadOutDownPayments(EnergyProducer agent, PowerPlantManufacturer manufacturer,
            double totalDownPayment, PowerPlant plant) {
        int buildingTime = (int) plant.getActualLeadtime();
        for (int i = 0; i < buildingTime; i++) {
            reps.nonTransactionalCreateRepository.createCashFlow(agent, manufacturer, totalDownPayment / buildingTime,
                    CashFlow.DOWNPAYMENT, getCurrentTick() + i, plant);
        }
    }

    @Override
    public double determineLoanAnnuities(double totalLoan, double payBackTime, double interestRate) {

        double q = 1 + interestRate;
        double annuity = totalLoan * (Math.pow(q, payBackTime) * (q - 1)) / (Math.pow(q, payBackTime) - 1);

        return annuity;
    }

    public double predictDemandForElectricitySpotMarket(ElectricitySpotMarket market,
            long numberOfYearsBacklookingForForecasting, long futureTimePoint) {
        GeometricTrendRegression gtr = new GeometricTrendRegression();
        for (long time = getCurrentTick(); time > getCurrentTick() - numberOfYearsBacklookingForForecasting
                && time >= 0; time = time - 1) {
            gtr.addData(time, market.getDemandGrowthTrend().getValue(time));
        }
        double forecast = gtr.predict(futureTimePoint);
        if (Double.isNaN(forecast))
            forecast = market.getDemandGrowthTrend().getValue(getCurrentTick());
        return forecast;
    }

}
