package com.fluxisai.audit;
import com.fluxisai.common.web.PageResponse; import org.springframework.data.domain.*; import org.springframework.data.web.PageableDefault; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/audit-events") public class AuditEventController {private final AuditEventService service;public AuditEventController(AuditEventService s){service=s;} @GetMapping public PageResponse<AuditEventResponse> list(@PageableDefault(size=50,sort="createdAt",direction=Sort.Direction.DESC)Pageable p){return service.list(p);}}
