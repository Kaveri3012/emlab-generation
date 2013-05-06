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
package emlab.gen.role.capacitymarket;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentClearingPoint;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.trend.TriangularTrend;

/**
 * @author Kaveri
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/emlab-gen-test-context.xml" })
@Transactional
public class SubmitCapacityBidToMarketRoleTest {

    Logger logger = Logger.getLogger(SubmitCapacityBidToMarketRole.class);

    @Autowired
    Reps reps;

    @Autowired
    SubmitCapacityBidToMarketRole submitCapacityBidRole;

    @Test
    public void testSubmitBidFunctionality() {

        Segment S1 = new Segment();
        S1.setLengthInHours(20);
        S1.persist();

        Segment S2 = new Segment();
        S2.setLengthInHours(30);
        S2.persist();

        SegmentLoad SG1 = new SegmentLoad();
        SG1.setSegment(S2);
        SG1.setBaseLoad(2500);

        // SegmentLoad SG2 = new SegmentLoad();
        // SG2.setSegment(S2);
        // SG2.setBaseLoad(2000);

        SegmentLoad SG3 = new SegmentLoad();
        SG3.setSegment(S1);
        SG3.setBaseLoad(3700);

        // SegmentLoad SG4 = new SegmentLoad();
        // SG4.setSegment(S1);
        // SG4.setBaseLoad(4000);

        SG1.persist();
        // SG2.persist();
        SG3.persist();
        // SG4.persist();

        Set<SegmentLoad> segmentLoads = new HashSet<SegmentLoad>();
        segmentLoads.add(SG1);
        segmentLoads.add(SG3);

        // Set<SegmentLoad> segmentLoads2 = new HashSet<SegmentLoad>();
        // segmentLoads2.add(SG2);
        // segmentLoads2.add(SG4);

        /*
         * TriangularTrend demandGrowthTrend = new TriangularTrend();
         * demandGrowthTrend.setMax(2); demandGrowthTrend.setMin(0);
         * demandGrowthTrend.setStart(1); demandGrowthTrend.setTop(1);
         * 
         * demandGrowthTrend.persist();
         */

        Zone zone = new Zone();
        zone.persist();

        PowerGridNode location = new PowerGridNode();
        location.setZone(zone);
        location.persist();

        ElectricitySpotMarket market = new ElectricitySpotMarket();
        market.setName("Market1");
        market.setZone(zone);
        market.setLoadDurationCurve(segmentLoads);
        // market.setDemandGrowthTrend(demandGrowthTrend);
        market.persist();

        PowerGeneratingTechnology coal1 = new PowerGeneratingTechnology();
        PowerGeneratingTechnology coal2 = new PowerGeneratingTechnology();
        PowerGeneratingTechnology gas1 = new PowerGeneratingTechnology();
        PowerGeneratingTechnology gas2 = new PowerGeneratingTechnology();

        coal1.persist();
        coal2.persist();
        gas1.persist();
        gas2.persist();

        EnergyProducer e1 = new EnergyProducer();
        e1.setName("E1");
        e1.setCash(0);
        e1.setPriceMarkUp(1);

        EnergyProducer e2 = new EnergyProducer();
        e2.setCash(0);
        e2.setPriceMarkUp(1);
        e2.setName("E2");

        e1.persist();
        e2.persist();

        PowerPlant pp1 = new PowerPlant();
        pp1.setTechnology(coal1);
        pp1.setOwner(e1);
        pp1.setActualFixedOperatingCost(99000);
        pp1.setLocation(location);
        // pp1.setName("PP1");

        PowerPlant pp2 = new PowerPlant();
        pp2.setTechnology(coal2);
        pp2.setOwner(e2);
        pp2.setActualFixedOperatingCost(111000);
        pp2.setLocation(location);
        // pp2.setName("PP2");

        PowerPlant pp3 = new PowerPlant();
        pp3.setTechnology(gas1);
        pp3.setOwner(e2);
        pp3.setActualFixedOperatingCost(56000);

        pp3.setLocation(location);

        PowerPlant pp4 = new PowerPlant();
        pp4.setTechnology(gas2);
        pp4.setOwner(e1);
        pp4.setActualFixedOperatingCost(65000);
        pp4.setLocation(location);

        PowerPlant pp5 = new PowerPlant();
        pp5.setTechnology(gas1);
        pp5.setOwner(e2);
        pp5.setActualFixedOperatingCost(56000);
        pp5.setLocation(location);

        PowerPlant pp6 = new PowerPlant();
        pp6.setTechnology(gas2);
        pp6.setOwner(e1);
        pp6.setActualFixedOperatingCost(65000);
        pp6.setLocation(location);

        pp1.persist();
        pp2.persist();
        pp3.persist();
        pp4.persist();
        pp5.persist();
        pp6.persist();

        CapacityMarket cMarket = new CapacityMarket();
        cMarket.setZone(zone);

        TriangularTrend gasFixedOperatingCostTimeSeries = new TriangularTrend();
        gasFixedOperatingCostTimeSeries.setMax(1.10);
        gasFixedOperatingCostTimeSeries.setMin(0.96);
        gasFixedOperatingCostTimeSeries.setStart(0.25);
        gasFixedOperatingCostTimeSeries.setTop(1.03);

        TriangularTrend coalFixedOperatingCostTimeSeries = new TriangularTrend();
        coalFixedOperatingCostTimeSeries.setMax(1.05);
        coalFixedOperatingCostTimeSeries.setMin(0.97);
        coalFixedOperatingCostTimeSeries.setStart(100);
        coalFixedOperatingCostTimeSeries.setTop(1.01);

        SegmentClearingPoint clearingPoint1 = new SegmentClearingPoint();
        clearingPoint1.setSegment(S1);
        clearingPoint1.setAbstractMarket(market);
        clearingPoint1.setPrice(25);
        clearingPoint1.setTime(0l);

        SegmentClearingPoint clearingPoint2 = new SegmentClearingPoint();
        clearingPoint2.setSegment(S2);
        clearingPoint2.setAbstractMarket(market);
        clearingPoint2.setPrice(7);
        clearingPoint2.setTime(0l);

        for (EnergyProducer ep : reps.energyProducerRepository
                .findAllEnergyProducersExceptForRenewableTargetInvestorsAtRandom()) {
            submitCapacityBidRole.act(ep);
            // logger.warn("Submitted");

        }

    }

}
