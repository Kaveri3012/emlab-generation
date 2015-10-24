/*******************************************************************************
 * Copyright 2013 the original author or authors.
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
package emlab.gen.role.renewablesupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.RelativeRenewableTarget;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * @author Kaveri3012 This role loops through eligible technologies, eligible
 *         nodes,
 * 
 *         computes LCOE per technology per node and creates an object,
 *         BaseCost, to store it.
 * 
 *         In technology neutral mode, after computing LCOE per technology, it
 *         should store LCOE per technology and create a merit order upto which
 *         a cetrain target is filled.
 * 
 */

@RoleComponent
public class ComputePremiumRole extends AbstractEnergyProducerRole<EnergyProducer>implements Role<EnergyProducer> {

    @Transient
    @Autowired
    Reps reps;

    @Transient
    @Autowired
    Neo4jTemplate template;

    @SuppressWarnings("unchecked")
    @Transactional
    public void act(RenewableSupportFipScheme scheme) {

        Regulator regulator = scheme.getRegulator();

        ElectricitySpotMarket eMarket = reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone());

        Iterable<PowerGeneratingTechnology> eligibleTechnologies = scheme.getPowerGeneratingTechnologiesEligible();

        for (PowerGeneratingTechnology technology : eligibleTechnologies) {
            // for (PowerGeneratingTechnology technology :
            // reps.powerGeneratingTechnologyRepository.findAll()) {
            DecarbonizationModel model = reps.genericRepository.findAll(DecarbonizationModel.class).iterator().next();
            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;
            // logger.warn(
            // "Calculating for " + technology.getName() + ", for Nodes: " +
            // possibleInstallationNodes.toString());

            for (PowerGridNode node : reps.powerGridNodeRepository.findAllPowerGridNodesByZone(regulator.getZone())) {

                // or create a new power plant if above statement returns null,
                // and assign it to a random energy producer.
                PowerPlant plant = new PowerPlant();

                EnergyProducer producer = reps.energyProducerRepository.findAll().iterator().next();

                plant.specifyNotPersist(getCurrentTick(), producer, node, technology);
                logger.warn("creating a new power plant for " + producer.getName() + ", of technology "
                        + plant.getTechnology().getName() + ", with node" + node.getName());

                Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                for (Substance fuel : technology.getFuels()) {
                    myFuelPrices.put(fuel, findLastKnownPriceForSubstance(fuel, getCurrentTick()));
                }

                Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                        findLastKnownCO2Price(getCurrentTick()));
                plant.setFuelMix(fuelMix);

                double mc = 0d;
                double annualMarginalCost = 0d;
                double totalGenerationinMWh = 0d;
                double lcoe = 0d;
                long numberOfSegments = reps.segmentRepository.count();
                double factor = 0d;
                double fullLoadHours = 0d;

                mc = calculateMarginalCostExclCO2MarketCost(plant, getCurrentTick());

                for (SegmentLoad segmentLoad : eMarket.getLoadDurationCurve()) {
                    double hours = segmentLoad.getSegment().getLengthInHours();
                    Segment segment = segmentLoad.getSegment();

                    if (technology.isIntermittent()) {
                        factor = plant.getIntermittentTechnologyNodeLoadFactor().getLoadFactorForSegment(segment);
                    } else {
                        double segmentID = segment.getSegmentID();
                        double min = technology.getPeakSegmentDependentAvailability();
                        double max = technology.getBaseSegmentDependentAvailability();
                        double segmentPortion = (numberOfSegments - segmentID) / (numberOfSegments - 1); // start
                        // counting
                        // at
                        // 1.

                        double range = max - min;
                        factor = max - segmentPortion * range;

                    }

                    fullLoadHours += factor * segment.getLengthInHours();

                }

                totalGenerationinMWh = fullLoadHours * plant.getActualNominalCapacity();
                annualMarginalCost = totalGenerationinMWh * mc;

                // logger.warn("for technology " +
                // plant.getTechnology().getName() + " total generation is "
                // + totalGenerationinMWh + " and running hours is " +
                // fullLoadHours);

                double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());
                double operatingCost = fixedOMCost + annualMarginalCost;

                long durationOfSupportScheme = scheme.getSupportSchemeDuration();

                logger.warn("Fixed OM cost for technology " + plant.getTechnology().getName() + " is " + fixedOMCost
                        + " and operatingCost is " + operatingCost);

                TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) durationOfSupportScheme, (int) plant.getActualLeadtime(),
                        plant.getActualInvestedCapital(), 0);

                // Creation of in cashflow during operation
                TreeMap<Integer, Double> discountedProjectCashOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) durationOfSupportScheme, (int) plant.getActualLeadtime(), 0, operatingCost);

                TreeMap<Integer, Double> factorDiscountedGenerationSeries = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) durationOfSupportScheme, (int) plant.getActualLeadtime(), 0, 1);

                // Calculation of weighted average cost of capital,
                // based on regulator's assumption of companies debt-ratio
                double wacc = (1 - regulator.getDebtRatioOfInvestments()) * regulator.getEquityInterestRate()
                        + regulator.getDebtRatioOfInvestments() * regulator.getLoanInterestRate();

                double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);
                // logger.warn("discountedCapitalCosts " +
                // discountedCapitalCosts);
                double discountedOpCost = npv(discountedProjectCashOutflow, wacc);
                double factorDiscountedGeneration = npv(factorDiscountedGenerationSeries, wacc);
                // logger.warn("discountedOpCost " + discountedOpCost);
                BiasFactor biasFactor = reps.renewableSupportSchemeRepository
                        .findBiasFactorGivenTechnologyNodeAndScheme(technology.getName(), node.getName(), scheme);

                if (scheme.isCostContainmentMechanismEnabled()) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor);
                }
                // double biasFactor = 1.0d;
                double biasFactorValue = biasFactor.getFeedInPremiumBiasFactor();
                lcoe = (discountedCapitalCosts + discountedOpCost) * biasFactorValue
                        / (totalGenerationinMWh * factorDiscountedGeneration);

                BaseCostFip baseCostFip = new BaseCostFip();

                baseCostFip.setCostPerMWh(lcoe);
                baseCostFip.setStartTime(getCurrentTick());
                baseCostFip.setNode(node);
                baseCostFip.setTechnology(technology);
                baseCostFip.setEndTime(getCurrentTick() + scheme.getSupportSchemeDuration());
                baseCostFip.persist();

                logger.warn("LCOE in per MWH for technology " + plant.getTechnology().getName() + "for node "
                        + baseCostFip.getNode().getNodeId() + " is , " + baseCostFip.getCostPerMWh());

            }
        }

    }

    private void computeDegressionAndResetBiasFactor(RenewableSupportFipScheme scheme, BiasFactor biasFactor) {

        // get target value of renewable generation
        double renewableTargetInMwh = computeRenewableGenerationTarget(scheme);
        double generationFromRenewables = totalExpectedGenerationFromRenewables(scheme);
        double degressionFactor = biasFactor.getDegressionFactor();

        if (generationFromRenewables >= renewableTargetInMwh) {
            double newBiasFactor = biasFactor.getFeedInPremiumBiasFactor() * (1 - degressionFactor);
            biasFactor.setFeedInPremiumBiasFactor(newBiasFactor);
        }
        // if expected generation exceeds target, degress by a certain
        // percentage.
        // else if expected generation is lower than a certain margin, increase
        // bias factor. - will have to create targetLowerMargin and
        // targetUpperMargin for it, best created in target object.

    }

    private double computeRenewableGenerationTarget(RenewableSupportFipScheme scheme) {
        double demandFactor;
        double targetFactor;
        Zone zone = scheme.getRegulator().getZone();

        logger.warn("Calculate Renewable Target Role started of zone: " + zone);

        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(zone);

        // get demand factor
        demandFactor = market.getDemandGrowthTrend().getValue(getCurrentTick());
        /*
         * it aggregates segments from both countries, so the boolean should
         * actually be true here and the code adjusted to FALSE case. Or a query
         * should be adjusted what probably will take less time.
         */

        // get renewable energy target in factor (percent)
        RelativeRenewableTarget target = reps.relativeRenewableTargetRepository
                .findRelativeRenewableTargetByRegulator(scheme.getRegulator());

        targetFactor = target.getYearlyRenewableTargetTimeSeries().getValue(getCurrentTick());
        // logger.warn("targetFactor is " + targetFactor);

        // get totalLoad in MWh
        double totalExpectedConsumption = 0d;

        for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
            // logger.warn("segmentLoad: " + segmentLoad);
            totalExpectedConsumption += segmentLoad.getBaseLoad() * demandFactor
                    * segmentLoad.getSegment().getLengthInHours();

            // logger.warn("demand factor is: " + demandFactor);

        }
        logger.warn("totalExpectedConsumption; " + totalExpectedConsumption);
        // renewable target for tender operation start year in MWh is
        double renewableTargetInMwh = targetFactor * totalExpectedConsumption;

        return renewableTargetInMwh;
    }

    private double totalExpectedGenerationFromRenewables(RenewableSupportFipScheme scheme) {

        double totalExpectedGeneration = 0d;
        double expectedGenerationPerTechnology = 0d;
        double expectedGenerationPerPlant = 0d;
        long numberOfSegments = reps.segmentRepository.count();
        // logger.warn("numberOfsegments: " + numberOfSegments);
        ElectricitySpotMarket market = reps.marketRepository
                .findElectricitySpotMarketForZone(scheme.getRegulator().getZone());

        for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
            expectedGenerationPerTechnology = 0d;
            for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByMarketAndTechnology(market,
                    technology, getCurrentTick())) {
                expectedGenerationPerPlant = 0d;
                for (Segment segment : reps.segmentRepository.findAll()) {
                    double availablePlantCapacity = plant.getAvailableCapacity(getCurrentTick(), segment,
                            numberOfSegments);
                    double lengthOfSegmentInHours = segment.getLengthInHours();
                    expectedGenerationPerPlant += availablePlantCapacity * lengthOfSegmentInHours;
                }
                expectedGenerationPerTechnology += expectedGenerationPerPlant;
            }
            totalExpectedGeneration += expectedGenerationPerTechnology;

        }

        return totalExpectedGeneration;
    }

    private TreeMap<Integer, Double> calculateSimplePowerPlantInvestmentCashFlow(int depreciationTime, int buildingTime,
            double totalInvestment, double operatingProfit) {
        TreeMap<Integer, Double> investmentCashFlow = new TreeMap<Integer, Double>();
        double equalTotalDownPaymentInstallement = totalInvestment / buildingTime;
        for (int i = 0; i < buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), equalTotalDownPaymentInstallement);
        }
        for (int i = buildingTime; i < depreciationTime + buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), operatingProfit);
        }
        return investmentCashFlow;
    }

    private double npv(TreeMap<Integer, Double> netCashFlow, double wacc) {
        double npv = 0;
        for (Integer iterator : netCashFlow.keySet()) {
            npv += netCashFlow.get(iterator).doubleValue() / Math.pow(1 + wacc, iterator.intValue());
        }
        return npv;
    }

    /*
     * (non-Javadoc)
     * 
     * @see agentspring.role.Role#act(agentspring.agent.Agent)
     */
    @Override
    public void act(EnergyProducer arg0) {
        // TODO Auto-generated method stub

    }

}
