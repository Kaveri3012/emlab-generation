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
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.capacity.CapacityClearingPoint;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.domain.market.capacity.CapacityMarket;
import emlab.gen.repository.Reps;
import emlab.gen.role.market.AbstractMarketRole;

/**
 * @author Kaveri
 * 
 */
public class PaymentFromConsumerToProducerforCapacityRole extends AbstractMarketRole<CapacityMarket> implements
        Role<CapacityMarket> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(CapacityMarket capacityMarket) {

        for (CapacityDispatchPlan plan : reps.capacityMarketRepository.findAllAcceptedCapacityDispatchPlansForTime(
                capacityMarket, getCurrentTick())) {

            CapacityClearingPoint capacityClearingPoint = reps.capacityMarketRepository
                    .findOneCapacityClearingPointForTime(getCurrentTick(), capacityMarket);

            reps.nonTransactionalCreateRepository.createCashFlow(capacityMarket.getConsumer(), plan.getBidder(),
                    plan.getAcceptedAmount() * capacityClearingPoint.getPrice(), CashFlow.SIMPLE_CAPACITY_MARKET,
                    getCurrentTick(), plan.getPlant());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see emlab.gen.role.market.AbstractMarketRole#getReps()
     */
    @Override
    public Reps getReps() {
        // TODO Auto-generated method stub
        return null;
    }

}
