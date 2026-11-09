package com.fairdraw.api.entry;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BallotRepository extends JpaRepository<Ballot, UUID> {

    List<Ballot> findByEntryId(UUID entryId);

    List<Ballot> findByEntryIdIn(Collection<UUID> entryIds);

    /** 봉인용: 이 공연의 살아 있는(active) 응모에 달린 제비 id 전부 */
    @Query("select b.id from Ballot b, Entry e "
         + "where e.id = b.entryId and e.eventId = :eventId and e.status = 'active'")
    List<UUID> findActiveBallotIds(@Param("eventId") UUID eventId);

    /** 현황용: 구역마다 [구역 id, 이름, 정원, 응모 인원, 제비 수] */
    @Query(value = """
            select s.id, s.name, s.capacity,
                   count(distinct e.id) as applicants,
                   count(e.id) as ballots
            from sections s
            left join (ballots b join entries e on e.id = b.entry_id and e.status = 'active')
                   on b.section_id = s.id
            where s.event_id = :eventId
            group by s.id, s.name, s.capacity
            order by s.name
            """, nativeQuery = true)
    List<Object[]> sectionStats(@Param("eventId") UUID eventId);
}