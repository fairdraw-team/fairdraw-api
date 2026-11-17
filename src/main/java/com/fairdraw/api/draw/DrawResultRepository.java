package com.fairdraw.api.draw;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrawResultRepository extends JpaRepository<DrawResult, UUID> {

    List<DrawResult> findByDrawId(UUID drawId);

    Optional<DrawResult> findByDrawIdAndEntryId(UUID drawId, UUID entryId);

    boolean existsByDrawId(UUID drawId);

    /**
     * 주최자 결과 명단 한 페이지: [응모 id, 실명, 전화번호, 구역 이름, 결과, 대기 순번, 팀 id]
     * 정렬: 구역 이름 → 결과(당첨, 대기, 낙첨) → 대기 순번 → 제비 순서 → 응모 id(같은 값끼리 순서 고정)
     */
    @Query(value = """
            select cast(r.entry_id as text), p.real_name, p.phone, s.name,
                   r.result, r.rank, cast(e.team_id as text)
            from draw_results r
            join entries e on e.id = r.entry_id
            join profiles p on p.id = e.user_id
            left join sections s on s.id = r.section_id
            where r.draw_id = :drawId
              and (cast(:result as text) is null or r.result = cast(:result as text))
            order by s.name nulls last,
                     case r.result when 'won' then 0 when 'waitlisted' then 1 else 2 end,
                     r.rank nulls last, r.draw_order nulls last, r.entry_id
            """,
            countQuery = """
            select count(*) from draw_results r
            where r.draw_id = :drawId
              and (cast(:result as text) is null or r.result = cast(:result as text))
            """,
            nativeQuery = true)
    Page<Object[]> findResultRows(@Param("drawId") UUID drawId,
                                  @Param("result") String result,
                                  Pageable pageable);
}