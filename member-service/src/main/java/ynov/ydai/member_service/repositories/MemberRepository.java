package ynov.ydai.member_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ynov.ydai.member_service.entities.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
