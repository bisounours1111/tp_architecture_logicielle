package ynov.ydai.reservation_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ynov.ydai.reservation_service.dto.MemberDTO;

@FeignClient(name = "member-service")
public interface MemberClient {
    @GetMapping("/api/members/{id}")
    MemberDTO getMemberById(@PathVariable("id") Long id);
}
