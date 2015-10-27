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
package emlab.gen.role.tender;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.agent.StochasticTargetInvestor;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.repository.StrategicReserveOperatorRepository;
import emlab.gen.role.AbstractEnergyProducerRole;
import emlab.gen.util.GeometricTrendRegression;
import emlab.gen.util.MapValueComparator;

/**
 * @author kaveri
 *
 */

@RoleComponent
public class SubmitTenderBidRole extends AbstractEnergyProducerRole<EnergyProducer> implements Role<EnergyProducer> {

    @Transient
    @Autowired
    Reps reps;

    @Transient
    @Autowired
    Neo4jTemplate template;

    @Transient
    @Autowired
    StrategicReserveOperatorRepository strategicReserveOperatorRepository;

    // market expectations
    @Transient
    Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();

    @Override
    public void act(EnergyProducer agent) {

        logger.warn("Submit Tender Bid Role started for " + agent);

        long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();
        // logger.warn(agent + " looking at timepoint " + futureTimePoint);

        // ==== Expectations ===

        Map<Substance, Double> expectedFuelPrices = predictFuelPrices(agent, futureTimePoint);

        // CO2
        Map<ElectricitySpotMarket, Double> expectedCO2Price = determineExpectedCO2PriceInclTaxAndFundamentalForecast(
                futureTimePoint, agent.getNumberOfYearsBacklookingForForecasting(), 0, getCurrentTick());

        // logger.warn("{} expects CO2 prices {}", agent.getName(),
        // expectedCO2Price);

        Map<ElectricitySpotMarket, Double> expectedCO2PriceOld = determineExpectedCO2PriceInclTax(futureTimePoint,
                agent.getNumberOfYearsBacklookingForForecasting(), getCurrentTick());

        // logger.warn("{} used to expect CO2 prices {}", agent.getName(),
        // expectedCO2PriceOld);

        // logger.warn(expectedCO2Price.toString());

        // Demand
        Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
        for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
            GeometricTrendRegression gtr = new GeometricTrendRegression();
            for (long time = getCurrentTick(); time > getCurrentTick()
                    - agent.getNumberOfYearsBacklookingForForecasting()
                    && time >= 0; time = time - 1) {
                gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
            }
            expectedDemand.put(elm, gtr.predict(futureTimePoint));
        }

        ElectricitySpotMarket market = agent.getInvestorMarket();

        // logger.warn("market is: " + market);

        MarketInformation marketInformation = new MarketInformation(market, expectedDemand, expectedFuelPrices,
                expectedCO2Price.get(market).doubleValue(), futureTimePoint);

        for (PowerGeneratingTechnology technology : reps.renewableSupportSchemeTenderRepository
                .findPowerGeneratingTechnologiesEligible()) {

            DecarbonizationModel model = reps.genericRepository.findAll(DecarbonizationModel.class).iterator().next();

            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;

            Iterable<PowerGridNode> possibleInstallationNodes;

            /*
             * For dispatchable technologies just choose a random node. For
             * intermittent evaluate all possibilities.
             */
            if (technology.isIntermittent())
                possibleInstallationNodes = reps.powerGridNodeRepository.findAllPowerGridNodesByZone(market.getZone());
            else {
                possibleInstallationNodes = new LinkedList<PowerGridNode>();
                ((LinkedList<PowerGridNode>) possibleInstallationNodes).add(reps.powerGridNodeRepository
                        .findAllPowerGridNodesByZone(market.getZone()).iterator().next());
            }

            // logger.warn("technology is " + technology);

            // "technology is intermittent? " + technology.isIntermittent());
            // logger.warn("possibleInstallationNodes is: " +
            // possibleInstallationNodes);

            // logger.warn("Calculating for " + technology.getName() +
            // ", for Nodes: "
            // + possibleInstallationNodes.toString());

            for (PowerGridNode node : possibleInstallationNodes) {

                PowerPlant plant = new PowerPlant();

                plant.specifyNotPersist(getCurrentTick(), agent, node, technology);

                // logger.warn("SubmitBid 168 - Agent " + agent +
                // " looking at technology at tick " + getCurrentTick()
                // + " in tech " + technology);

                // logger.warn(" agent is " + agent + " with technology " +
                // technology + " and plant " + plant
                // + " in node " + node);

                // if too much capacity of this technology in the pipeline (not
                // limited to the 5 years)
                double expectedInstalledCapacityOfTechnology = reps.powerPlantRepository
                        .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market, technology,
                                futureTimePoint);

                // technology target for the tender role is null
                PowerGeneratingTechnologyTarget technologyTarget = reps.powerGenerationTechnologyTargetRepository
                        .findOneByTechnologyAndMarket(technology, market);
                if (technologyTarget != null) {
                    double technologyTargetCapacity = technologyTarget.getTrend().getValue(futureTimePoint);
                    expectedInstalledCapacityOfTechnology = (technologyTargetCapacity > expectedInstalledCapacityOfTechnology) ? technologyTargetCapacity
                            : expectedInstalledCapacityOfTechnology;
                }
                double pgtNodeLimit = Double.MAX_VALUE;

                // logger.warn("pgtNodeLimit 1 is: " + pgtNodeLimit);

                PowerGeneratingTechnologyNodeLimit pgtLimit = reps.powerGeneratingTechnologyNodeLimitRepository
                        .findOneByTechnologyAndNode(technology, plant.getLocation());

                // logger.warn("technology for pgtNodeLimit is" + technology);
                // logger.warn("plant location for pgtNodeLimit is" +
                // plant.getLocation());

                // logger.warn("pgtNodeLimit 2 is: " + pgtNodeLimit);

                if (pgtLimit != null) {
                    pgtNodeLimit = pgtLimit.getUpperCapacityLimit(futureTimePoint);
                }

                // Calculate bid quantity. Number of plants to be bid - as many
                // as
                // the node permits

                double ratioNodeCapacity = pgtNodeLimit / plant.getActualNominalCapacity();

                // capacityTesting
                double numberOfPlants = (long) ratioNodeCapacity; // truncates
                // towards
                // lower
                // integer

                // logger.warn("pgtNodeLimit 3 is: " + pgtNodeLimit);

                // logger.warn("actual nominal capacity is: " +
                // plant.getActualNominalCapacity());

                // logger.warn("ratioNodeCapacity: " + ratioNodeCapacity);

                // logger.warn("numberOfPlants is: " + numberOfPlants);

                // if cash strapped, bid quantity according to fraction of cash,
                // which is translated to the number of plants
                // available.

                // If cash needed is larger than current cash of agent
                if (numberOfPlants * plant.getActualInvestedCapital() * (1 - agent.getDebtRatioOfInvestments()) > agent
                        .getDownpaymentFractionOfCash() * agent.getCash()) {

                    // logger.warn("Cash fraction method needed to compute number of plants");

                    double cashAvailableFraction = (agent.getDownpaymentFractionOfCash() * agent.getCash())
                            / (numberOfPlants * plant.getActualInvestedCapital() * (1 - agent
                                    .getDebtRatioOfInvestments()));

                    if (cashAvailableFraction < 0) {
                        cashAvailableFraction = 0;
                    }

                    numberOfPlants = cashAvailableFraction * numberOfPlants;

                    // logger.warn("cash available fraction is: " +
                    // cashAvailableFraction);
                    // logger.warn("number of plants are: " + numberOfPlants);

                    // capacityTesting
                    numberOfPlants = (long) numberOfPlants; // truncates
                    // towards
                    // lower
                    // integer

                    // logger.warn("number of plants are after Cash Fraction: "
                    // + numberOfPlants);
                }

                // computing tender bid price

                Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                for (Substance fuel : technology.getFuels()) {
                    myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                }

                Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                        expectedCO2Price.get(market));
                plant.setFuelMix(fuelMix);

                double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                        expectedCO2Price.get(market));
                double runningHours = 0d;
                double expectedGrossProfit = 0d;

                long numberOfSegments = reps.segmentRepository.count();
                double totalAnnualExpectedGenerationOfPlant = 0d;

                long tenderSchemeDuration = reps.renewableSupportSchemeTenderRepository
                        .determineSupportSchemeDurationForEnergyProducer(agent);

                // logger.warn("tender duration is" + tenderSchemeDuration);

                // should be
                // modified when
                // location
                // specific

                for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                    double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment
                            .get(segmentLoad.getSegment());
                    double hours = segmentLoad.getSegment().getLengthInHours();

                    if (expectedMarginalCost <= expectedElectricityPrice) {
                        // logger.warn("expectedMarginalCost: " +
                        // expectedMarginalCost +
                        // " and expectedElectricityPrice"
                        // + expectedElectricityPrice);

                        runningHours = runningHours + hours;

                        // logger.warn("hours is: " + hours);

                        // logger.warn("runningHours: " + runningHours);

                        if (technology.isIntermittent()) {

                            // logger.warn("This logger should not showing up: technology is intermittent");
                            expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost)
                                    * hours
                                    * plant.getActualNominalCapacity()
                                    * reps.intermittentTechnologyNodeLoadFactorRepository
                                            .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node,
                                                    technology).getLoadFactorForSegment(segmentLoad.getSegment());

                            totalAnnualExpectedGenerationOfPlant += hours
                                    * plant.getActualNominalCapacity()
                                    * reps.intermittentTechnologyNodeLoadFactorRepository
                                            .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node,
                                                    technology).getLoadFactorForSegment(segmentLoad.getSegment());

                            // logger.warn("INTERMITTENT totalAnnualExpectedGenerationOfPlant "
                            // + totalAnnualExpectedGenerationOfPlant +
                            // " of tech " + technology);

                        } else {
                            expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost)
                                    * hours
                                    * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(),
                                            numberOfSegments);

                            // logger.warn("expectedGrossProfit: " +
                            // expectedGrossProfit);

                            totalAnnualExpectedGenerationOfPlant += hours
                                    * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(),
                                            numberOfSegments);

                            // logger.warn("hours: " + hours);

                            // logger.warn("getAvailableCapacity: "
                            // + plant.getAvailableCapacity(futureTimePoint,
                            // segmentLoad.getSegment(),
                            // numberOfSegments));

                            // logger.warn("DISPATCH totalAnnualExpectedGenerationOfPlant "
                            // + totalAnnualExpectedGenerationOfPlant +
                            // " of tech " + technology);

                        }
                    }
                }

                double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());
                // logger.warn("fixedOMCost is: " + fixedOMCost);

                double operatingProfit = expectedGrossProfit - fixedOMCost;
                // logger.warn("operatingProfit is: " + operatingProfit);

                double wacc = (1 - agent.getDebtRatioOfInvestments()) * agent.getEquityInterestRate()
                        + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();

                // Creation of out cash-flow during power plant building
                // phase (note that the cash-flow is negative!)
                TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(),
                        (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime()),
                        plant.getActualInvestedCapital(), 0);
                // Creation of in cashflow during operation
                TreeMap<Integer, Double> discountedProjectCashInflow = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(),
                        (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime()), 0, operatingProfit);

                double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);
                // logger.warn("discountedCapitalCosts is: " +
                // discountedCapitalCosts);
                double discountedOpProfit = npv(discountedProjectCashInflow, wacc);
                // logger.warn("discountedOpProfit is: " + discountedOpProfit);

                double projectValue = discountedOpProfit + discountedCapitalCosts; // tendertesting

                // logger.warn("project value: " + projectValue +
                // ", discountedOpProfit: " + discountedOpProfit
                // + ", discountedCapitalCosts: " + discountedCapitalCosts);

                // logger.warn("projectValue is: " + projectValue);
                // logger.warn("totalAnnualExpectedGenerationOfPlant is: " +
                // totalAnnualExpectedGenerationOfPlant);

                double bidPricePerMWh = 0d;

                if (projectValue >= 0 || totalAnnualExpectedGenerationOfPlant == 0) {
                    bidPricePerMWh = 0d;

                } else {

                    // calculate discounted tender return factor term

                    TreeMap<Integer, Double> discountedTenderReturnFactorSummingTerm = calculateSimplePowerPlantInvestmentCashFlow(
                            (int) tenderSchemeDuration,
                            (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime()), 0, 1);
                    double discountedTenderReturnFactor = npv(discountedTenderReturnFactorSummingTerm, wacc);

                    // logger.warn("discountedTenderReturnFactor is: " +
                    // discountedTenderReturnFactor);

                    if (discountedTenderReturnFactor == 0) {
                        bidPricePerMWh = 0d;
                        // logger.warn(" zero discountedTenderReturnFactor -
                        // bid price per mwh is "
                        // + bidPricePerMWh);

                    } else {

                        // calculate generation in MWh per year
                        bidPricePerMWh = -projectValue
                                / (discountedTenderReturnFactor * totalAnnualExpectedGenerationOfPlant);

                        Zone zone = market.getZone();

                        logger.warn("get zone via market: " + market.getZone());
                        logger.warn("zone: " + zone);

                        RenewableSupportSchemeTender scheme = reps.renewableSupportSchemeTenderRepository
                                .determineSupportSchemeForZone(zone);

                        logger.warn("reps Scheme: "
                                + reps.renewableSupportSchemeTenderRepository.determineSupportSchemeForZone(zone));

                        logger.warn("scheme is: " + scheme);

                        for (long i = 1; i <= numberOfPlants; i++) {

                            TenderBid bid = new TenderBid();
                            bid.specifyAndPersist(
                                    totalAnnualExpectedGenerationOfPlant,
                                    plant,
                                    agent,
                                    zone,
                                    node,
                                    (getCurrentTick() + (plant.calculateActualLeadtime() + plant
                                            .calculateActualPermittime())),
                                    (getCurrentTick()
                                            + (plant.calculateActualLeadtime() + plant.calculateActualPermittime()) + tenderSchemeDuration),
                                    bidPricePerMWh, technology, getCurrentTick(), Bid.SUBMITTED, scheme);

                            logger.warn("SubmitBid 454 - Agent " + agent + " at tick " + getCurrentTick() + " in tech "
                                    + technology + " with plant name " + plant.getName() + " with bidprice "
                                    + bidPricePerMWh + " with generation " + totalAnnualExpectedGenerationOfPlant
                                    + "in zone " + zone);

                        } // end for loop for tender bids

                    } // end else calculate generation in MWh per year

                } // end else calculate discounted tender return factor term

            } // end for loop possible installation nodes

        } // end for (PowerGeneratingTechnology technology :
          // reps.genericRepository.findAll(PowerGeneratingTechnology.class))

    }

    // }

    // Creates n downpayments of equal size in each of the n building years of a
    // power plant
    @Transactional
    private void createSpreadOutDownPayments(EnergyProducer agent, PowerPlantManufacturer manufacturer,
            double totalDownPayment, PowerPlant plant) {
        int buildingTime = (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime());
        reps.nonTransactionalCreateRepository.createCashFlow(agent, manufacturer, totalDownPayment / buildingTime,
                CashFlow.DOWNPAYMENT, getCurrentTick(), plant);
        Loan downpayment = reps.loanRepository.createLoan(agent, manufacturer, totalDownPayment / buildingTime,
                buildingTime - 1, getCurrentTick(), plant);
        plant.createOrUpdateDownPayment(downpayment);
    }

    /**
     * Predicts fuel prices for {@link futureTimePoint} using a geometric trend
     * regression forecast. Only predicts fuels that are traded on a commodity
     * market.
     * 
     * @param agent
     * @param futureTimePoint
     * @return Map<Substance, Double> of predicted prices.
     */
    public Map<Substance, Double> predictFuelPrices(EnergyProducer agent, long futureTimePoint) {
        // Fuel Prices
        Map<Substance, Double> expectedFuelPrices = new HashMap<Substance, Double>();
        for (Substance substance : reps.substanceRepository.findAllSubstancesTradedOnCommodityMarkets()) {
            // Find Clearing Points for the last 5 years (counting current year
            // as one of the last 5 years).
            Iterable<ClearingPoint> cps = reps.clearingPointRepository
                    .findAllClearingPointsForSubstanceTradedOnCommodityMarkesAndTimeRange(substance, getCurrentTick()
                            - (agent.getNumberOfYearsBacklookingForForecasting() - 1), getCurrentTick(), false);
            // logger.warn("{}, {}",
            // getCurrentTick()-(agent.getNumberOfYearsBacklookingForForecasting()-1),
            // getCurrentTick());
            // Create regression object
            SimpleRegression gtr = new SimpleRegression();
            for (ClearingPoint clearingPoint : cps) {
                // logger.warn("CP {}: {} , in" + clearingPoint.getTime(),
                // substance.getName(), clearingPoint.getPrice());
                gtr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            }
            gtr.addData(getCurrentTick(), findLastKnownPriceForSubstance(substance, getCurrentTick()));
            expectedFuelPrices.put(substance, gtr.predict(futureTimePoint));
            // logger.warn("Forecast {}: {}, in Step " + futureTimePoint,
            // substance, expectedFuelPrices.get(substance));
        }
        return expectedFuelPrices;
    }

    // Create a powerplant investment and operation cash-flow in the form of a
    // map. If only investment, or operation costs should be considered set
    // totalInvestment or operatingProfit to 0
    private TreeMap<Integer, Double> calculateSimplePowerPlantInvestmentCashFlow(int depriacationTime,
            int buildingTime, double totalInvestment, double operatingProfit) {
        TreeMap<Integer, Double> investmentCashFlow = new TreeMap<Integer, Double>();
        double equalTotalDownPaymentInstallement = totalInvestment / buildingTime;
        for (int i = 0; i < buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), -equalTotalDownPaymentInstallement);
        }
        for (int i = buildingTime; i < depriacationTime + buildingTime; i++) {
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

    public double determineExpectedMarginalCost(PowerPlant plant, Map<Substance, Double> expectedFuelPrices,
            double expectedCO2Price) {
        double mc = determineExpectedMarginalFuelCost(plant, expectedFuelPrices);
        double co2Intensity = plant.calculateEmissionIntensity();
        mc += co2Intensity * expectedCO2Price;
        return mc;
    }

    public double determineExpectedMarginalFuelCost(PowerPlant powerPlant, Map<Substance, Double> expectedFuelPrices) {
        double fc = 0d;
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {
            double amount = mix.getShare();
            double fuelPrice = expectedFuelPrices.get(mix.getSubstance());
            fc += amount * fuelPrice;
        }
        return fc;
    }

    private PowerGridNode getNodeForZone(Zone zone) {
        for (PowerGridNode node : reps.genericRepository.findAll(PowerGridNode.class)) {
            if (node.getZone().equals(zone)) {
                return node;
            }
        }
        return null;
    }

    private class MarketInformation {

        Map<Segment, Double> expectedElectricityPricesPerSegment;
        double maxExpectedLoad = 0d;
        Map<PowerPlant, Double> meritOrder;
        double capacitySum;

        MarketInformation(ElectricitySpotMarket market, Map<ElectricitySpotMarket, Double> expectedDemand,
                Map<Substance, Double> fuelPrices, double co2price, long time) {
            // determine expected power prices
            expectedElectricityPricesPerSegment = new HashMap<Segment, Double>();
            Map<PowerPlant, Double> marginalCostMap = new HashMap<PowerPlant, Double>();
            capacitySum = 0d;

            // get merit order for this market
            for (PowerPlant plant : reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarket(market, time)) {

                double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                marginalCostMap.put(plant, plantMarginalCost);
                capacitySum += plant.getActualNominalCapacity();
            }

            // get difference between technology target and expected operational
            // capacity
            for (TargetInvestor targetInvestor : reps.targetInvestorRepository.findAllByMarket(market)) {
                if (!(targetInvestor instanceof StochasticTargetInvestor)) {
                    for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
                        double expectedTechnologyCapacity = reps.powerPlantRepository
                                .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
                                        pggt.getPowerGeneratingTechnology(), time);
                        double targetDifference = pggt.getTrend().getValue(time) - expectedTechnologyCapacity;
                        if (targetDifference > 0) {

                            PowerPlant plant = new PowerPlant();

                            plant.specifyNotPersist(getCurrentTick(), new EnergyProducer(),
                                    reps.powerGridNodeRepository.findFirstPowerGridNodeByElectricitySpotMarket(market),
                                    pggt.getPowerGeneratingTechnology());
                            plant.setActualNominalCapacity(targetDifference);
                            double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                            marginalCostMap.put(plant, plantMarginalCost);
                            capacitySum += targetDifference;

                            logger.warn("SubmitBid 618 - Agent " + targetInvestor + " invested in technology at tick "
                                    + getCurrentTick() + " in tech " + pggt.getPowerGeneratingTechnology());

                        }
                    }

                } else {
                    for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
                        double expectedTechnologyCapacity = reps.powerPlantRepository
                                .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
                                        pggt.getPowerGeneratingTechnology(), time);
                        double expectedTechnologyAddition = 0;
                        long contructionTime = getCurrentTick()
                                + pggt.getPowerGeneratingTechnology().getExpectedLeadtime()
                                + pggt.getPowerGeneratingTechnology().getExpectedPermittime();
                        for (long investmentTimeStep = contructionTime + 1; investmentTimeStep <= time; investmentTimeStep = investmentTimeStep + 1) {
                            expectedTechnologyAddition += (pggt.getTrend().getValue(investmentTimeStep) - pggt
                                    .getTrend().getValue(investmentTimeStep - 1));
                        }
                        if (expectedTechnologyAddition > 0) {
                            PowerPlant plant = new PowerPlant();
                            plant.specifyNotPersist(getCurrentTick(), new EnergyProducer(),
                                    reps.powerGridNodeRepository.findFirstPowerGridNodeByElectricitySpotMarket(market),
                                    pggt.getPowerGeneratingTechnology());
                            plant.setActualNominalCapacity(expectedTechnologyAddition);
                            double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                            marginalCostMap.put(plant, plantMarginalCost);
                            capacitySum += expectedTechnologyAddition;

                            logger.warn("SubmitBid 648 - Agent " + targetInvestor + " invested in technology at tick "
                                    + getCurrentTick() + " in tech " + pggt.getPowerGeneratingTechnology());

                        }
                    }
                }

            }

            MapValueComparator comp = new MapValueComparator(marginalCostMap);
            meritOrder = new TreeMap<PowerPlant, Double>(comp);
            meritOrder.putAll(marginalCostMap);

            long numberOfSegments = reps.segmentRepository.count();

            double demandFactor = expectedDemand.get(market).doubleValue();

            // find expected prices per segment given merit order
            for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {

                double expectedSegmentLoad = segmentLoad.getBaseLoad() * demandFactor;

                if (expectedSegmentLoad > maxExpectedLoad) {
                    maxExpectedLoad = expectedSegmentLoad;
                }

                double segmentSupply = 0d;
                double segmentPrice = 0d;
                double totalCapacityAvailable = 0d;

                for (Entry<PowerPlant, Double> plantCost : meritOrder.entrySet()) {
                    PowerPlant plant = plantCost.getKey();
                    double plantCapacity = 0d;
                    // Determine available capacity in the future in this
                    // segment
                    plantCapacity = plant
                            .getExpectedAvailableCapacity(time, segmentLoad.getSegment(), numberOfSegments);
                    totalCapacityAvailable += plantCapacity;
                    // logger.warn("Capacity of plant " + plant.toString() +
                    // " is " +
                    // plantCapacity/plant.getActualNominalCapacity());
                    if (segmentSupply < expectedSegmentLoad) {
                        segmentSupply += plantCapacity;
                        segmentPrice = plantCost.getValue();
                    }

                }

                // logger.warn("Segment " +
                // segmentLoad.getSegment().getSegmentID() + " supply equals " +
                // segmentSupply + " and segment demand equals " +
                // expectedSegmentLoad);

                // Find strategic reserve operator for the market.
                double reservePrice = 0;
                double reserveVolume = 0;
                for (StrategicReserveOperator operator : strategicReserveOperatorRepository.findAll()) {
                    ElectricitySpotMarket market1 = reps.marketRepository.findElectricitySpotMarketForZone(operator
                            .getZone());
                    if (market.getNodeId().intValue() == market1.getNodeId().intValue()) {
                        reservePrice = operator.getReservePriceSR();
                        reserveVolume = operator.getReserveVolume();
                    }
                }

                if (segmentSupply >= expectedSegmentLoad
                        && ((totalCapacityAvailable - expectedSegmentLoad) <= (reserveVolume))) {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), reservePrice);
                    // logger.warn("Price: "+
                    // expectedElectricityPricesPerSegment);
                } else if (segmentSupply >= expectedSegmentLoad
                        && ((totalCapacityAvailable - expectedSegmentLoad) > (reserveVolume))) {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), segmentPrice);
                    // logger.warn("Price: "+
                    // expectedElectricityPricesPerSegment);
                } else {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), market.getValueOfLostLoad());
                }

            }
        }
    }

    /**
     * Calculates expected CO2 price based on a geometric trend estimation, of
     * the past years. The adjustmentForDetermineFuelMix needs to be set to 1,
     * if this is used in the determine fuel mix role.
     *
     * @param futureTimePoint
     *            Year the prediction is made for
     * @param yearsLookingBackForRegression
     *            How many years are used as input for the regression, incl the
     *            current tick.
     * @return
     */
    protected HashMap<ElectricitySpotMarket, Double> determineExpectedCO2PriceInclTaxAndFundamentalForecast(
            long futureTimePoint, long yearsLookingBackForRegression, int adjustmentForDetermineFuelMix,
            long clearingTick) {
        HashMap<ElectricitySpotMarket, Double> co2Prices = new HashMap<ElectricitySpotMarket, Double>();
        CO2Auction co2Auction = reps.genericRepository.findFirst(CO2Auction.class);
        Iterable<ClearingPoint> cps = reps.clearingPointRepository.findAllClearingPointsForMarketAndTimeRange(
                co2Auction, clearingTick - yearsLookingBackForRegression + 1 - adjustmentForDetermineFuelMix,
                clearingTick - adjustmentForDetermineFuelMix, false);
        // Create regression object and calculate average
        SimpleRegression sr = new SimpleRegression();
        Government government = reps.template.findAll(Government.class).iterator().next();
        double lastPrice = 0;
        double averagePrice = 0;
        int i = 0;
        for (ClearingPoint clearingPoint : cps) {
            sr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            lastPrice = clearingPoint.getPrice();
            averagePrice += lastPrice;
            i++;
        }
        averagePrice = averagePrice / i;
        double expectedCO2Price;
        double expectedRegressionCO2Price;
        if (i > 1) {
            expectedRegressionCO2Price = sr.predict(futureTimePoint);
            expectedRegressionCO2Price = Math.max(0, expectedRegressionCO2Price);
            expectedRegressionCO2Price = Math
                    .min(expectedRegressionCO2Price, government.getCo2Penalty(futureTimePoint));
        } else {
            expectedRegressionCO2Price = lastPrice;
        }
        ClearingPoint expectedCO2ClearingPoint = reps.clearingPointRepository.findClearingPointForMarketAndTime(
                co2Auction, getCurrentTick()
                        + reps.genericRepository.findFirst(DecarbonizationModel.class).getCentralForecastingYear(),
                true);
        expectedCO2Price = (expectedCO2ClearingPoint == null) ? 0 : expectedCO2ClearingPoint.getPrice();
        expectedCO2Price = (expectedCO2Price + expectedRegressionCO2Price) / 2;
        for (ElectricitySpotMarket esm : reps.marketRepository.findAllElectricitySpotMarkets()) {
            double nationalCo2MinPriceinFutureTick = reps.nationalGovernmentRepository
                    .findNationalGovernmentByElectricitySpotMarket(esm).getMinNationalCo2PriceTrend()
                    .getValue(futureTimePoint);
            double co2PriceInCountry = 0d;
            if (expectedCO2Price > nationalCo2MinPriceinFutureTick) {
                co2PriceInCountry = expectedCO2Price;
            } else {
                co2PriceInCountry = nationalCo2MinPriceinFutureTick;
            }
            co2PriceInCountry += reps.genericRepository.findFirst(Government.class).getCO2Tax(futureTimePoint);
            co2Prices.put(esm, Double.valueOf(co2PriceInCountry));
        }
        return co2Prices;
    }

}
