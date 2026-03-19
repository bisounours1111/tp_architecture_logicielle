package ynov.ydai.member_service.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ynov.ydai.member_service.entities.Member;
import ynov.ydai.member_service.entities.SubscriptionType;
import ynov.ydai.member_service.repositories.MemberRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String MEMBER_DELETED_TOPIC = "member-deleted-topic";

    public MemberService(MemberRepository memberRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.memberRepository = memberRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    public Member createMember(Member member) {
        setQuotas(member);
        member.setSuspended(false);
        return memberRepository.save(member);
    }

    public Member updateMember(Long id, Member memberDetails) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        
        member.setFullName(memberDetails.getFullName());
        member.setEmail(memberDetails.getEmail());
        member.setSubscriptionType(memberDetails.getSubscriptionType());
        setQuotas(member);
        
        return memberRepository.save(member);
    }

    public void deleteMember(Long id) {
        if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id);
            try {
                kafkaTemplate.send(MEMBER_DELETED_TOPIC, id.toString());
            } catch (Exception e) {
                System.err.println("Kafka indisponible, suppression effectuée en base uniquement.");
            }
        }
    }

    private void setQuotas(Member member) {
        if (member.getSubscriptionType() == SubscriptionType.BASIC) {
            member.setMaxConcurrentBookings(2);
        } else if (member.getSubscriptionType() == SubscriptionType.PRO) {
            member.setMaxConcurrentBookings(5);
        } else if (member.getSubscriptionType() == SubscriptionType.ENTERPRISE) {
            member.setMaxConcurrentBookings(10);
        }
    }

    public void updateSuspensionStatus(Long id, boolean suspended) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        member.setSuspended(suspended);
        memberRepository.save(member);
    }

    @KafkaListener(topics = "member-suspension-topic", groupId = "member-service-group")
    public void handleSuspensionEvent(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2) {
            String action = parts[0];
            Long memberId = Long.parseLong(parts[1]);

            if ("SUSPEND".equals(action)) {
                updateSuspensionStatus(memberId, true);
            } else if ("UNSUSPEND".equals(action)) {
                updateSuspensionStatus(memberId, false);
            }
        }
    }
}
