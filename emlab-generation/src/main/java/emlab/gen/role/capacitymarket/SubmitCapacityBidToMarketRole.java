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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

/**
 * @author Kaveri
 * 
 */
public class SubmitCapacityBidToMarketRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(EnergyProducer producer) {

        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {

            // get market for the plant by zone
            CapacityMarket market = reps.capacityMarketRepository.findCapacityMarketForZone(plant.getLocation()
                    .getZone());

            double price = plant.getTechnology().getFixedOperatingCost(getCurrentTick());
            logger.info("Submitting offers for {} with technology {}", plant.getName(), plant.getTechnology().getName());

            // if price is > price cap, be a price taker and submit at 0 price
            double priceCap = market.getRegulator().getCapacityMarketPriceCap();
            if (price > priceCap)
                price = 0;

            double peakLoadSegment = 0;
            for (SegmentLoad segmentload : market.getLoadDurationCurve()) {

                if (segmentload.getBaseLoad() > peakLoadSegment)
                    peakLoadSegment = segmentload.getBaseLoad();

            }

            long numberOfSegments = reps.segmentRepository.count();

            double capacity = plant.getAvailableCapacity(getCurrentTick(), null, numberOfSegments);
            logger.info("I bid capacity: {} and price: {} into the capacity market", capacity, price);

            CapacityDispatchPlan plan = new CapacityDispatchPlan().persist();
            // plan.specifyNotPersist(plant, producer, market, segment,
            // time, price, bidWithoutCO2, spotMarketCapacity,
            // longTermContractCapacity, status);
            plan.specifyAndPersist(plant, producer, market, getCurrentTick(), price, capacity, Bid.SUBMITTED);

            logger.info("Submitted {} for iteration {} to capacity market", plan);

        }
    }

}
