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

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.market.capacity.CapacityClearingPoint;
import emlab.gen.domain.market.capacity.CapacityDispatchPlan;
import emlab.gen.repository.Reps;

/**
 * @author Kaveri
 * 
 */

@RoleComponent
public class ClearCapacityMarketRole extends AbstractRole<Regulator> implements Role<Regulator> {

    // CapacityMarketRepository capacityMarketRepository;

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(Regulator regulator) {

        // CapacityMarket market = new CapacityMarket();
        // market =
        // reps.capacityMarketRepository.findCapacityMarketForZone(regulator.getZone());

        Iterable<CapacityDispatchPlan> sortedListofCDP = null;
        sortedListofCDP = reps.capacityMarketRepository.findAllSortedCapacityDispatchPlansByTime(getCurrentTick());
        double demand = 0d;
        double sumofSupplyBidsAccepted = 0d;
        double acceptedPrice = 0d;
        boolean isTheMarketCleared = false;

        // This epsilon is to account for rounding errors for java (only
        // relevant for exact clearing)
        double clearingEpsilon = 0.001d;

        if (regulator.getDemandTarget() == 0) {
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

                // logger.warn("Price of this cdp is " + currentCDP.getPrice());
                // logger.warn("Demand at this cdp is " + demand);

                if (isTheMarketCleared == false) {
                    double temp = demand - (sumofSupplyBidsAccepted + currentCDP.getAmount());
                    logger.warn("Demand minus currently considered bids " + temp);
                    if (demand - (sumofSupplyBidsAccepted + currentCDP.getAmount()) >= -clearingEpsilon) {
                        acceptedPrice = currentCDP.getPrice();
                        currentCDP.setStatus(currentCDP.ACCEPTED);
                        currentCDP.setAcceptedAmount(currentCDP.getAmount());
                        sumofSupplyBidsAccepted = sumofSupplyBidsAccepted + currentCDP.getAmount();

                        // logger.warn("{}", sumofSupplyBidsAccepted);
                    }

                    else if (demand - (sumofSupplyBidsAccepted + currentCDP.getAmount()) < clearingEpsilon) {

                        currentCDP.setStatus(currentCDP.PARTLY_ACCEPTED);
                        currentCDP.setAcceptedAmount((demand - sumofSupplyBidsAccepted));
                        acceptedPrice = currentCDP.getPrice();
                        sumofSupplyBidsAccepted = sumofSupplyBidsAccepted + currentCDP.getAcceptedAmount();
                        isTheMarketCleared = true;

                        // logger.warn("Accepted" +
                        // currentBid.getAcceptedVolume());

                    }
                }

            } else {
                currentCDP.setStatus(currentCDP.FAILED);
                currentCDP.setAcceptedAmount(0);
            }

            if (demand - sumofSupplyBidsAccepted < clearingEpsilon) {
                isTheMarketCleared = true;
            }

            logger.warn("Cumulatively Accepted Supply " + sumofSupplyBidsAccepted);

        }

        if (isTheMarketCleared == true) {
            // sumofSupplyBidsAccepted = demand;
            CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
            clearingPoint.setPrice(acceptedPrice);
            clearingPoint.setVolume(sumofSupplyBidsAccepted);
            clearingPoint.setTime(getCurrentTick());
            // clearingPoint.setCapacityMarket(market);
            clearingPoint.persist();
            logger.warn("Clearing point Price" + clearingPoint.getPrice());
            logger.warn("Clearing Point Volume" + clearingPoint.getVolume());
        } else {
            acceptedPrice = regulator.getCapacityMarketPriceCap()
                    * (1 + ((regulator.getDemandTarget() * (1 - regulator.getReserveDemandLowerMargin()) - sumofSupplyBidsAccepted) / ((regulator
                            .getReserveDemandUpperMargin() + regulator.getReserveDemandLowerMargin()) * regulator
                            .getDemandTarget())));
            CapacityClearingPoint clearingPoint = new CapacityClearingPoint();
            clearingPoint.setPrice(max(regulator.getCapacityMarketPriceCap(), acceptedPrice));
            clearingPoint.setVolume(sumofSupplyBidsAccepted);
            clearingPoint.setTime(getCurrentTick());
            // clearingPoint.setCapacityMarket(market);
            clearingPoint.persist();
            logger.warn("Clearing point Price" + clearingPoint.getPrice());
            logger.warn("Clearing Point Volume" + clearingPoint.getVolume());
        }

        logger.warn("is the market cleared? " + isTheMarketCleared);

    }

    /**
     * @param capacityMarketPriceCap
     * @param acceptedPrice
     * @return
     */
    private double max(double capacityMarketPriceCap, double acceptedPrice) {
        if (acceptedPrice >= capacityMarketPriceCap)
            return capacityMarketPriceCap;
        else
            return acceptedPrice;
    }

}
