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
package emlab.gen.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;

/**
 * @author rjjdejeu
 *
 */
public interface TenderBidRepository extends GraphRepository<TenderBid> {

    // This sorts the submitted tender bids by price
    @Query(value = "g.v(zone).out('ZONE').filter{it.time == tick}.sort{it.price}._()", type = QueryType.Gremlin)
    public Iterable<TenderBid> findAllSortedTenderBidsbyTime(@Param("tick") long time, @Param("zone") Zone zone);

    // This returns the (partly) accepted bids for the current tick, needed to
    // create the corresponding power plant
    @Query(value = "g.v(scheme).in('TENDERBID_SUPPORTSCHEME')"
            + ".propertyFilter('time', FilterPipe.Filter.EQUAL, tick)"
            + ".propertyFilter('status', FilterPipe.Filter.GREATER_THAN_EQUAL, 2)", type = QueryType.Gremlin)
    public Iterable<TenderBid> findAllAcceptedTenderBidsByTime(
            @Param("scheme") RenewableSupportSchemeTender renewableSupportSchemeTender, @Param("tick") long time);

    // this returns the accepted tender bids Scheme that needs to be paid out
    @Query(value = "g.v(scheme).in('TENDERBID_SUPPORTSCHEME')"
            + ".propertyFilter('start', FilterPipe.Filter.LESS_THAN_EQUAL, tick)"
            + ".propertyFilter('finish', FilterPipe.Filter.GREATER_THAN, tick)"
            + ".propertyFilter('status', FilterPipe.Filter.GREATER_THAN_EQUAL, 2)", type = QueryType.Gremlin)
    public Iterable<TenderBid> findAllTenderBidsThatShouldBePaidInTimeStep(
            @Param("scheme") RenewableSupportSchemeTender renewableSupportSchemeTender, @Param("tick") long time);

}
