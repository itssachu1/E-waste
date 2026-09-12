package com.janvoice.ai.service.impl;

import com.janvoice.ai.dto.RecyclerDecisionRequest;
import com.janvoice.ai.dto.RecyclerRequest;
import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.entity.Recycler;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.MaterialMasterRepository;
import com.janvoice.ai.repository.RecyclerRepository;
import com.janvoice.ai.service.RecyclerService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecyclerServiceImpl implements RecyclerService {
    private final RecyclerRepository repository;
    private final MaterialMasterRepository materials;

    public RecyclerServiceImpl(RecyclerRepository repository, MaterialMasterRepository materials) {
        this.repository = repository;
        this.materials = materials;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> find(Long materialId, String city, Recycler.AuthorizationStatus status, Boolean active) {
        return repository.findAll().stream()
                .filter(r -> materialId == null || r.getAcceptedMaterials().stream().anyMatch(m -> m.getId().equals(materialId)))
                .filter(r -> city == null || r.getCity().equalsIgnoreCase(city))
                .filter(r -> status == null ? r.getAuthorizationStatus() != Recycler.AuthorizationStatus.REJECTED : r.getAuthorizationStatus() == status)
                .filter(r -> active == null ? (status != null || r.isActive()) : r.isActive() == active)
                .sorted(Comparator.comparing(Recycler::getBusinessName))
                .map(this::response).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> findById(Long id) {
        return response(repository.findById(id).orElseThrow(() -> notFound("Recycler not found")));
    }

    @Override
    @Transactional
    public List<Map<String, Object>> match(Long materialId, String location) {
        if (materialId == null) throw bad("material_id is required");
        String search = location == null ? "" : location.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(Recycler::isActive)
                .filter(r -> r.getAuthorizationStatus() != Recycler.AuthorizationStatus.REJECTED)
                .filter(r -> r.getAcceptedMaterials().stream().anyMatch(m -> m.getId().equals(materialId)))
                .sorted(Comparator.comparingInt((Recycler r) -> r.getAuthorizationStatus() == Recycler.AuthorizationStatus.VERIFIED ? 0 : 1)
                        .thenComparingInt(r -> locationMatch(r, search) ? 0 : 1)
                        .thenComparing((Recycler r) -> !r.isPickupAvailable())
                        .thenComparing(Recycler::getBusinessName))
                .map(this::response).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> create(RecyclerRequest request, User actor) {
        requireAuthenticated(actor);
        Recycler recycler = new Recycler();
        apply(request, recycler);
        recycler.setCreatedBy(actor.getId());
        recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.PENDING_VERIFICATION);
        return response(repository.save(recycler));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, RecyclerRequest request, User actor) {
        Recycler recycler = repository.findById(id).orElseThrow(() -> notFound("Recycler not found"));
        requireOwnerOrAdmin(recycler, actor);
        apply(request, recycler);
        return response(repository.save(recycler));
    }

    @Override
    @Transactional
    public Map<String, Object> verify(Long id, RecyclerDecisionRequest request, User actor) {
        requireAdmin(actor);
        Recycler recycler = repository.findById(id).orElseThrow(() -> notFound("Recycler not found"));
        if (request.getVerificationSource() == null || request.getVerificationSource().isBlank()) throw bad("verificationSource is required for verification");
        recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.VERIFIED);
        recycler.setVerificationSource(request.getVerificationSource().trim());
        recycler.setVerifiedBy(actor.getId());
        recycler.setLastVerifiedAt(LocalDateTime.now());
        return response(repository.save(recycler));
    }

    @Override
    @Transactional
    public Map<String, Object> reject(Long id, RecyclerDecisionRequest request, User actor) {
        requireAdmin(actor);
        Recycler recycler = repository.findById(id).orElseThrow(() -> notFound("Recycler not found"));
        if (request.getReason() == null || request.getReason().isBlank()) throw bad("reason is required for rejection");
        recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.REJECTED);
        recycler.setVerificationSource(request.getReason().trim());
        recycler.setVerifiedBy(actor.getId());
        recycler.setLastVerifiedAt(LocalDateTime.now());
        return response(repository.save(recycler));
    }

    private void apply(RecyclerRequest request, Recycler recycler) {
        if (request.getLatitude() != null && (request.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0 || request.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0)) throw bad("latitude must be between -90 and 90");
        if (request.getLongitude() != null && (request.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0 || request.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0)) throw bad("longitude must be between -180 and 180");
        Set<MaterialMaster> accepted = request.getAcceptedMaterialIds().stream().map(id -> materials.findById(id).filter(MaterialMaster::isActive).orElseThrow(() -> bad("accepted material must be active"))).collect(Collectors.toSet());
        recycler.setBusinessName(request.getBusinessName().trim()); recycler.setContactName(request.getContactName()); recycler.setPhone(request.getPhone().trim()); recycler.setAddress(request.getAddress()); recycler.setCity(request.getCity().trim());
        recycler.setLatitude(request.getLatitude()); recycler.setLongitude(request.getLongitude()); recycler.setAcceptedMaterials(accepted); recycler.setPickupAvailable(request.isPickupAvailable()); recycler.setPickupRadius(request.getPickupRadius()); recycler.setServiceArea(request.getServiceArea()); recycler.setRegistrationNumber(request.getRegistrationNumber());
    }

    private boolean locationMatch(Recycler recycler, String location) { return !location.isEmpty() && (recycler.getCity().toLowerCase(Locale.ROOT).contains(location) || (recycler.getServiceArea() != null && recycler.getServiceArea().toLowerCase(Locale.ROOT).contains(location))); }
    private Map<String, Object> response(Recycler r) { Map<String, Object> map = new LinkedHashMap<>(); map.put("id", r.getId()); map.put("business_name", r.getBusinessName()); map.put("contact_name", r.getContactName()); map.put("phone", r.getPhone()); map.put("address", r.getAddress()); map.put("city", r.getCity()); map.put("latitude", r.getLatitude()); map.put("longitude", r.getLongitude()); map.put("accepted_materials", r.getAcceptedMaterials().stream().map(m -> Map.of("id", m.getId(), "common_name", m.getCommonName(), "typical_unit", m.getTypicalUnit())).collect(Collectors.toList())); map.put("pickup_available", r.isPickupAvailable()); map.put("pickup_radius", r.getPickupRadius()); map.put("service_area", r.getServiceArea()); map.put("authorization_status", r.getAuthorizationStatus()); map.put("registration_number", r.getRegistrationNumber()); map.put("verification_source", r.getVerificationSource()); map.put("last_verified_at", r.getLastVerifiedAt()); map.put("verified_by", r.getVerifiedBy()); map.put("created_by", r.getCreatedBy()); map.put("active", r.isActive()); map.put("created_at", r.getCreatedAt()); map.put("updated_at", r.getUpdatedAt()); return map; }
    private void requireAuthenticated(User actor) { if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session token required"); }
    private void requireAdmin(User actor) { requireAuthenticated(actor); if (!"ADMIN".equalsIgnoreCase(actor.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required"); }
    private void requireOwnerOrAdmin(Recycler recycler, User actor) { requireAuthenticated(actor); if (!"ADMIN".equalsIgnoreCase(actor.getRole()) && !actor.getId().equals(recycler.getCreatedBy())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recycler owner or an admin may update this record"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
