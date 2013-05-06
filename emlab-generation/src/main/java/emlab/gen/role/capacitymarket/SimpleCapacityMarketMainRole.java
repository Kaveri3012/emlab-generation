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

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.repository.Reps;

/**
 * @author Kaveri
 * 
 */
public class SimpleCapacityMarketMainRole extends AbstractRole<EnergyProducer> implements Role<EnergyProducer> {

    @Autowired
    Reps reps;

    @Autowired
    ForecastDemandRole forecastDemandRole;

    @Autowired
    SubmitCapacityBidToMarketRole submitCapacityBidToMarketRole;

    @Autowired
    ClearCapacityMarketRole clearCapacityMarketRole;

    @Autowired
    PaymentFromConsumerToProducerforCapacityRole paymentFromConsumerToProducerforCapacityRole;

    @Override
    public void act(EnergyProducer agent) {

        // loop through every zone?
        for (Zone zone : reps.zoneRepository.findAll()) {

            // boolean

            // create regulator
            Regulator regulator = new Regulator();

            CapacityMarket capacityMarket = new CapacityMarket();

            regulator.setZone(zone);
            capacityMarket.setZone(zone);
            capacityMarket.setRegulator(regulator);

            // Forecast Demand
            forecastDemandRole.act(regulator);

            // Energy producers submit Bids to Capacity market

            submitCapacityBidToMarketRole.act(agent);

            // Clear capacity market
            clearCapacityMarketRole.act(regulator);

            // ensure cash flows
            paymentFromConsumerToProducerforCapacityRole.act(capacityMarket);

            // create boolean in the investment algorithm.
            // add to decarb role.
            // do a sanity check once.

        }

    }
}
