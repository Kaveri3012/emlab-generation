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
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.policy.renewablesupport.TenderClearingPoint;
import emlab.gen.repository.Reps;

/**
 * @author rjjdejeu
 */
@RoleComponent
public class ClearRenewableTenderRole extends AbstractRole<Regulator> implements Role<Regulator> {

    @Autowired
    Reps reps;

    @Autowired
    Neo4jTemplate template;

    @Override
    @Transactional
    public void act(Regulator regulator) {

        logger.warn("Clear Renewable Tender Role started");

        Zone zone = regulator.getZone();
        RenewableSupportSchemeTender scheme = reps.renewableSupportSchemeTenderRepository
                .determineSupportSchemeForZone(zone);

        // logger.warn("scheme is: " + scheme);

        // Initialize a sorted list for tender bids
        Iterable<TenderBid> sortedTenderBidsbyPriceAndZone = null;
        sortedTenderBidsbyPriceAndZone = reps.tenderBidRepository.findAllSortedTenderBidsbyTime(getCurrentTick(), zone);

        double tenderQuota = regulator.getAnnualRenewableTargetInMwh();
        logger.warn("TenderQuota; " + tenderQuota);
        double sumOfTenderBidQuantityAccepted = 0d;
        double acceptedSubsidyPrice = 0d;
        boolean isTheTenderCleared = false;

        if (tenderQuota == 0) {
            isTheTenderCleared = true;
            acceptedSubsidyPrice = 0;
        }

        // This epsilon is to account for rounding errors for java (only
        // relevant for exact clearing)
        double clearingEpsilon = 0.0001d;

        // Goes through the list of the bids that are sorted on ascending order
        // by price
        for (TenderBid currentTenderBid : sortedTenderBidsbyPriceAndZone) {

            // if the tender is not cleared yet, it collects complete bids
            if (isTheTenderCleared == false) {
                if (tenderQuota - (sumOfTenderBidQuantityAccepted + currentTenderBid.getAmount()) >= -clearingEpsilon) {
                    acceptedSubsidyPrice = currentTenderBid.getPrice();
                    currentTenderBid.setStatus(Bid.ACCEPTED);
                    currentTenderBid.setAcceptedAmount(currentTenderBid.getAmount());

                    logger.warn("bidder; " + currentTenderBid.getBidder());
                    logger.warn("bidAmount; " + currentTenderBid.getAmount());
                    logger.warn("acceptedSubsidyPrice; " + acceptedSubsidyPrice);
                    logger.warn("Technology; " + currentTenderBid.getTechnology());
                    logger.warn("Status; " + currentTenderBid.getStatus());

                    sumOfTenderBidQuantityAccepted = sumOfTenderBidQuantityAccepted + currentTenderBid.getAmount();

                    logger.warn("sumOfTenderBidQuantityAccepted; " + sumOfTenderBidQuantityAccepted);

                }

                // it collects a bid partially if that bid fulfills the quota
                // partially
                else if (tenderQuota - (sumOfTenderBidQuantityAccepted + currentTenderBid.getAmount()) < clearingEpsilon) {
                    acceptedSubsidyPrice = currentTenderBid.getPrice();
                    currentTenderBid.setStatus(Bid.PARTLY_ACCEPTED);
                    currentTenderBid.setAcceptedAmount((tenderQuota - sumOfTenderBidQuantityAccepted));

                    logger.warn("Tender Quota minus sumofTenderBidQAccepted: "
                            + (tenderQuota - sumOfTenderBidQuantityAccepted));

                    logger.warn("PARTLYbidder; " + currentTenderBid.getBidder());
                    logger.warn("PARTLYbidAmount; " + currentTenderBid.getAmount());
                    logger.warn("PARTLYacceptedSubsidyPrice; " + acceptedSubsidyPrice);
                    logger.warn("PARTLYTechnology; " + currentTenderBid.getTechnology());
                    logger.warn("PARTLYStatus; " + currentTenderBid.getStatus());

                    sumOfTenderBidQuantityAccepted = sumOfTenderBidQuantityAccepted
                            + currentTenderBid.getAcceptedAmount();

                    logger.warn("PARTLYsumOfTenderBidQuantityAccepted; " + sumOfTenderBidQuantityAccepted);

                    isTheTenderCleared = true;

                }
                // the tenderQuota is reached and the bids after that are not
                // accepted

            } else {
                currentTenderBid.setStatus(Bid.FAILED);
                currentTenderBid.setAcceptedAmount(0);

            }

            currentTenderBid.persist();

        } // FOR Loop ends here

        // This creates a clearing point that contains general information about
        // the cleared tender
        // volume, subsidy price, current tick, and stores it in the graph
        // database

        if (isTheTenderCleared == true) {
            TenderClearingPoint tenderClearingPoint = new TenderClearingPoint();
            // logger.warn("Tender CLEARED at price: " + acceptedSubsidyPrice);
            tenderClearingPoint.setPrice(acceptedSubsidyPrice);
            tenderClearingPoint.setRenewableSupportSchemeTender(scheme);
            tenderClearingPoint.setVolume(sumOfTenderBidQuantityAccepted);
            tenderClearingPoint.setTime(getCurrentTick());
            tenderClearingPoint.persist();
            logger.warn("Tender CLEARED at price {} and volume " + tenderClearingPoint.getVolume(),
                    tenderClearingPoint.getPrice());

        } else {
            TenderClearingPoint tenderClearingPoint = new TenderClearingPoint();
            // logger.warn("MARKET UNCLEARED at price: " +
            // acceptedSubsidyPrice);
            tenderClearingPoint.setPrice(acceptedSubsidyPrice);
            tenderClearingPoint.setVolume(sumOfTenderBidQuantityAccepted);
            tenderClearingPoint.setRenewableSupportSchemeTender(scheme);
            tenderClearingPoint.setTime(getCurrentTick());
            tenderClearingPoint.persist();
            logger.warn("Tender UNCLEARED at price {} and volume " + tenderClearingPoint.getVolume(),
                    tenderClearingPoint.getPrice());

        }

    }

}
