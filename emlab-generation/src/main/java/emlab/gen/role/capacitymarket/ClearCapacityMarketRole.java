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

import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.market.capacity.CapacityClearingPoint;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.repository.CapacityMarketRepository;

/**
 * @author Kaveri
 * 
 */
public class ClearCapacityMarketRole extends AbstractRole<Regulator> implements Role<Regulator> {

    CapacityMarketRepository capacityMarketRepository;

    @Override
    @Transactional
    public void act(Regulator regulator) {

        Iterable<CapacityDispatchPlan> sortedListofCDP = capacityMarketRepository
                .findAllSortedCapacityDispatchPlansByPrice(getCurrentTick());
        double demand = 0d;
        double sumofSupplyBidsAccepted = 0d;
        double acceptedPrice = 0d;
        boolean isTheMarketCleared = false;

        // This epsilon is to account for rounding errors for java (only
        // relevant for exact clearing)
        double clearingEpsilon = 0.001;

        if (demand == 0) {
            isTheMarketCleared = true;
            acceptedPrice = 0;
        }

        for (CapacityDispatchPlan currentCDP : sortedListofCDP) {

            if (currentCDP.getPrice() <= regulator.getCapacityMarketPriceCap()) {

                demand = regulator.getDemandTarget()
                        * (1 - regulator.getReserveDemandLowerMargin())
                        + ((regulator.getCapacityMarketPriceCap() - currentCDP.getPrice())
                                * (regulator.getReserveDemandUpperMargin() + regulator.getReserveDemandLowerMargin()) * regulator
                                    .getDemandTarget()) / regulator.getCapacityMarketPriceCap();

                if (isTheMarketCleared == false) {

                    if (demand - (sumofSupplyBidsAccepted + currentCDP.getAmount()) >= -clearingEpsilon) {
                        acceptedPrice = currentCDP.getPrice();
                        currentCDP.setStatus(currentCDP.ACCEPTED);
                        currentCDP.setAmount(currentCDP.getAmount());
                        sumofSupplyBidsAccepted += currentCDP.getAmount();

                        // logger.warn("{}", sumofSupplyBidsAccepted);
                    }

                    else if (demand - (sumofSupplyBidsAccepted + currentCDP.getAmount()) < clearingEpsilon) {

                        currentCDP.setStatus(currentCDP.PARTLY_ACCEPTED);
                        currentCDP.setAmount((demand - sumofSupplyBidsAccepted));
                        acceptedPrice = currentCDP.getPrice();
                        sumofSupplyBidsAccepted += currentCDP.getAmount();
                        isTheMarketCleared = true;

                        // logger.warn("Accepted" +
                        // currentBid.getAcceptedVolume());

                    }
                }

            } else {
                currentCDP.setStatus(currentCDP.FAILED);
                currentCDP.setAmount(0);
            }

            if (demand - sumofSupplyBidsAccepted < clearingEpsilon)
                isTheMarketCleared = true;

        }
        if (isTheMarketCleared == true) {
            // sumofSupplyBidsAccepted = demand;
            CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
            clearingPoint.setPrice(acceptedPrice);
            clearingPoint.setVolume(sumofSupplyBidsAccepted);
            clearingPoint.setTime(getCurrentTick());
            clearingPoint.persist();
        } else {
            CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
            clearingPoint.setPrice(regulator.getCapacityMarketPriceCap());
            clearingPoint.setVolume(sumofSupplyBidsAccepted);
            clearingPoint.setTime(getCurrentTick());
            clearingPoint.persist();
        }
    }

}
