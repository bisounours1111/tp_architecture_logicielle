package ynov.ydai.member_service.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ynov.ydai.member_service.entities.Member;
import ynov.ydai.member_service.services.MemberService;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Member Management", description = "Endpoints for managing members and subscriptions")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "Get all members")
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID")
    public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new member (automatically sets quotas)")
    public Member createMember(@RequestBody Member member) {
        return memberService.createMember(member);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing member")
    public ResponseEntity<Member> updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
        try {
            return ResponseEntity.ok(memberService.updateMember(id, memberDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a member (propagates cancellation to reservations via Kafka)")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/suspension")
    @Operation(summary = "Update member suspension status manually")
    public ResponseEntity<Void> updateSuspension(@PathVariable Long id, @RequestParam boolean suspended) {
        try {
            memberService.updateSuspensionStatus(id, suspended);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
