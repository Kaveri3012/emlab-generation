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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.RenewableTargetForTender;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.util.GeometricTrendRegression;

/**
 * @author Kaveri, rjjdejeu
 *
 */

@RoleComponent
public class CalculateRenewableTargetForTenderRole extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(RenewableSupportSchemeTender scheme) {

        double demandFactor;
        double targetFactor;
        Zone zone = scheme.getRegulator().getZone();

        logger.warn("Calculate Renewable Target Role started of zone: " + zone);

        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(zone);

        // get demand factor
        demandFactor = predictDemandForElectricitySpotMarket(market,
                scheme.getRegulator().getNumberOfYearsLookingBackToForecastDemand(),
                scheme.getFutureTenderOperationStartTime());

        logger.warn("demandGrowth; " + demandFactor);

        // get renewable energy target in factor (percent)
        RenewableTargetForTender target = reps.renewableTargetForTenderRepository
                .findRenewableTargetForTenderByRegulator(scheme.getRegulator());

        targetFactor = target.getYearlyRenewableTargetTimeSeries()
                .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());
        logger.warn("targetFactor; " + targetFactor);

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
        logger.warn("renewableTargetInMwh; " + renewableTargetInMwh);

        // calculate expected generation, and subtract that from annual
        // target.
        // will be ActualTarget
        double totalExpectedGeneration = 0d;
        double expectedGenerationPerTechnology = 0d;
        double expectedGenerationPerPlant = 0d;
        long numberOfSegments = reps.segmentRepository.count();
        // logger.warn("numberOfsegments: " + numberOfSegments);
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

        /*
         * To compare
         * 
         * Electricity consumption in 2011 was for 1) NL: 1.17E+08 MWh 2) DE:
         * 5.79E+08 MWh 3) source http://www.indexmundi.com/facts/indicators
         * /EG.USE.ELEC.KH/rankings
         * 
         * Total electricity production in 2011 for 1) NL: 1.09E+08 MWh 2) DE:
         * 5.77+08 MWh 3) source
         * http://ec.europa.eu/eurostat/statistics-explained/images
         * /d/d9/Net_electricity_generation
         * %2C_1990%E2%80%932013_%28thousand_GWh%29_YB15.png
         * 
         * Total RENEWABLE electricity production in 2010 for 1) NL: 1.04E+07
         * MWh DE: 1.05E+08 MWh 2) sources http://www.cbs.nl/NR/rdonlyres
         * /BED23760-23C0-47D0-8A2A-224402F055F 3/0/2012c90pub.pdf
         * https://en.wikipedia.org/wiki/Renewable_energy_in_Germany#Sources
         * 
         * totalExpectedGeneration EMLab RES-E for 2020 1) NL: 2.51E+10 MWh 2)
         * DE: 2.51E+10 MWh
         * 
         * Conclusions: 0) Although I compare 2010 with 2020, the numbers should
         * be more or less in the same ball park, and they are not. 1) There is
         * a factor 100(0) too much in EMLab most likely, originating from
         * totalExpectingGeneration 2) Also, NL and DE start with the same
         * totalExpectedGeneration, which could not be right. Probably due to
         * the initial portfolios.
         * 
         * --> solved, the queries in line 118 and 122 were not market/zone
         * specific and summed all the totalExpectedGeneration instead of doing
         * it per market/zone
         */

        // logger.warn("renewabeTargetInMWh; " + renewableTargetInMwh);
        logger.warn("totalExpectedGeneration; " + totalExpectedGeneration);

        renewableTargetInMwh = renewableTargetInMwh - totalExpectedGeneration;

        if (renewableTargetInMwh < 0) {
            renewableTargetInMwh = 0;
        }
        scheme.getRegulator().setAnnualRenewableTargetInMwh(renewableTargetInMwh);

        logger.warn("actualRenewableTargetInMwh; " + renewableTargetInMwh);
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
