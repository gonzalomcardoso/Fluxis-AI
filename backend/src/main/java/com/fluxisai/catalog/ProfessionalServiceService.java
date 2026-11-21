package com.fluxisai.catalog;

import java.util.UUID;
import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ProfessionalServiceService {
    private final ProfessionalServiceRepository services; private final CurrentUser currentUser;
    public ProfessionalServiceService(ProfessionalServiceRepository services,CurrentUser currentUser){this.services=services;this.currentUser=currentUser;}
    @Transactional public ProfessionalServiceResponse create(ProfessionalServiceRequest r){var user=currentUser.id();var name=clean(r.name());if(services.existsByUserIdAndNameIgnoreCase(user,name))throw new ConflictException("Ya existe un servicio con ese nombre");return ProfessionalServiceResponse.from(services.save(new ProfessionalService(user,name,optional(r.description()),optional(r.category()),r.pricingModel(),r.rate(),r.currency(),r.available(),r.status(),optional(r.notes()))));}
    @Transactional(readOnly=true) public ProfessionalServiceResponse get(UUID id){return ProfessionalServiceResponse.from(find(id));}
    @Transactional(readOnly=true) public PageResponse<ProfessionalServiceResponse> list(Pageable p){return PageResponse.from(services.findAllByUserId(currentUser.id(),p).map(ProfessionalServiceResponse::from));}
    @Transactional public ProfessionalServiceResponse update(UUID id,ProfessionalServiceRequest r){var service=find(id);var name=clean(r.name());if(services.existsByUserIdAndNameIgnoreCaseAndIdNot(currentUser.id(),name,id))throw new ConflictException("Ya existe un servicio con ese nombre");service.update(name,optional(r.description()),optional(r.category()),r.pricingModel(),r.rate(),r.currency(),r.available(),r.status(),optional(r.notes()));return ProfessionalServiceResponse.from(services.save(service));}
    @Transactional public void delete(UUID id){services.delete(find(id));}
    private ProfessionalService find(UUID id){return services.findByIdAndUserId(id,currentUser.id()).orElseThrow(()->new ResourceNotFoundException("No se encontró el servicio"));}
    private static String clean(String value){return value.trim();} private static String optional(String value){if(value==null)return null;var v=value.trim();return v.isEmpty()?null:v;}
}
