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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.repository.Reps;
import emlab.gen.util.GeometricTrendRegression;

/**
 * @author Kaveri
 * 
 */
@NodeEntity
public class ForecastDemandRole extends AbstractRole<Regulator> implements Role<Regulator> {

    Reps reps;

    @Override
    public void act(Regulator regulator) {

        long capabilityYear = getCurrentTick() + regulator.getTargetPeriod();

        // Computing Demand (the current year's demand is not considered for the
        // regression, as it is forecasted.
        Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();

        for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
            GeometricTrendRegression gtr = new GeometricTrendRegression();
            for (long time = getCurrentTick() - 1; time > getCurrentTick() - 1
                    - regulator.getNumberOfYearsLookingBackToForecastDemand()
                    && time >= 0; time = time - 1) {
                gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
            }
            expectedDemand.put(elm, gtr.predict(capabilityYear));
        }
        // Calculate peak demand across all markets
        double peakExpectedDemand = (Collections.max(expectedDemand.values()));

        // Compute demand target by multiplying reserve margin
        double demandTarget = peakExpectedDemand * (1 + regulator.getReserveMargin());

        regulator.setDemandTarget(demandTarget);

        /*
         * Map<ElectricitySpotMarket, Double> peakDemand = null;
         * 
         * for (expectedDemand demand : map.entrySet()) { if (peakDemand == null
         * || entry.getValue().compareTo(peakDemand.getValue()) > 0) {
         * peakDemand = entry; } }
         */

    }

}
