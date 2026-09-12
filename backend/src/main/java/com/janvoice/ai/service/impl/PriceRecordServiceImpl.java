package com.janvoice.ai.service.impl;

import com.janvoice.ai.dto.PriceRecordRequest;
import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.entity.PriceRecord;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.MaterialMasterRepository;
import com.janvoice.ai.repository.PriceRecordRepository;
import com.janvoice.ai.service.PriceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PriceRecordServiceImpl implements PriceRecordService {
    private static final int STALENESS_DAYS = 30;
    private final PriceRecordRepository repository;
    private final MaterialMasterRepository materials;

    public PriceRecordServiceImpl(PriceRecordRepository repository, MaterialMasterRepository materials) {
        this.repository = repository;
        this.materials = materials;
    }

    @Override
    public List<Map<String, Object>> find(Long materialId, String location, String status, boolean includeExpired) {
        List<PriceRecord> records = load(materialId, location);
        LocalDateTime now = LocalDateTime.now();
        return records.stream().map(record -> response(record, now))
                .filter(record -> includeExpired || !"EXPIRED".equals(String.valueOf(record.get("verification_status"))))
                .filter(record -> status == null || status.equalsIgnoreCase(String.valueOf(record.get("verification_status"))))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> history(Long materialId, String location) {
        return find(materialId, location, null, true);
    }

    @Override
    public Map<String, Object> create(PriceRecordRequest request, User actor) {
        requireRole(actor.getRole(), false);
        MaterialMaster material = materials.findById(request.getMaterialId())
                .filter(MaterialMaster::isActive)
                .orElseThrow(() -> bad("material_id must reference an active Material Master record"));
        if (request.getUnit() != material.getTypicalUnit()) throw bad("unit must match material typical_unit");
        if (request.getQuotedAt().isAfter(LocalDateTime.now())) throw bad("quoted_at cannot be in the future");
        if (request.getValidUntil() != null && !request.getValidUntil().isAfter(request.getQuotedAt())) throw bad("valid_until must be after quoted_at");

        PriceRecord record = new PriceRecord();
        record.setMaterial(material); record.setLocation(request.getLocation().trim()); record.setBuyerType(request.getBuyerType());
        record.setBuyerId(request.getBuyerId()); record.setRate(request.getRate()); record.setUnit(request.getUnit());
        record.setGrade(request.getGrade()); record.setCondition(request.getCondition()); record.setSourceType(request.getSourceType());
        record.setSourceReference(request.getSourceReference()); record.setQuotedAt(request.getQuotedAt()); record.setValidUntil(request.getValidUntil());
        record.setCreatedBy(actor.getId()); record.setVerificationStatus(isVerifier(actor.getRole()) ? PriceRecord.VerificationStatus.VERIFIED : PriceRecord.VerificationStatus.UNVERIFIED);
        if (isVerifier(actor.getRole())) { record.setVerifiedBy(actor.getId()); record.setVerifiedAt(LocalDateTime.now()); }
        return response(repository.save(record), LocalDateTime.now());
    }

    @Override
    public Map<String, Object> verify(Long id, User actor) {
        requireRole(actor.getRole(), true);
        PriceRecord record = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Price record not found"));
        if (isExpired(record, LocalDateTime.now())) throw bad("Expired price records cannot be verified");
        record.setVerificationStatus(PriceRecord.VerificationStatus.VERIFIED); record.setVerifiedBy(actor.getId()); record.setVerifiedAt(LocalDateTime.now());
        return response(repository.save(record), LocalDateTime.now());
    }

    private List<PriceRecord> load(Long materialId, String location) {
        if (materialId != null && location != null && !location.isBlank()) return repository.findByMaterial_IdAndLocationIgnoreCaseOrderByQuotedAtDesc(materialId, location.trim());
        if (materialId != null) return repository.findByMaterial_IdOrderByQuotedAtDesc(materialId);
        if (location != null && !location.isBlank()) return repository.findByLocationIgnoreCaseOrderByQuotedAtDesc(location.trim());
        return repository.findAllByOrderByQuotedAtDesc();
    }

    private Map<String, Object> response(PriceRecord record, LocalDateTime now) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", record.getId()); result.put("material_id", record.getMaterial().getId()); result.put("material_name", record.getMaterial().getCommonName());
        result.put("location", record.getLocation()); result.put("buyer_type", record.getBuyerType()); result.put("buyer_id", record.getBuyerId());
        result.put("rate", record.getRate()); result.put("unit", record.getUnit()); result.put("grade", record.getGrade()); result.put("condition", record.getCondition());
        result.put("source_type", record.getSourceType()); result.put("source_reference", record.getSourceReference()); result.put("quoted_at", record.getQuotedAt()); result.put("valid_until", record.getValidUntil());
        result.put("verification_status", isExpired(record, now) ? PriceRecord.VerificationStatus.EXPIRED : record.getVerificationStatus());
        result.put("created_by", record.getCreatedBy()); result.put("verified_by", record.getVerifiedBy()); result.put("verified_at", record.getVerifiedAt()); result.put("created_at", record.getCreatedAt()); result.put("updated_at", record.getUpdatedAt());
        return result;
    }

    private boolean isExpired(PriceRecord record, LocalDateTime now) {
        return record.getValidUntil() != null ? record.getValidUntil().isBefore(now) : record.getQuotedAt().plusDays(STALENESS_DAYS).isBefore(now);
    }
    private void requireRole(String role, boolean adminOnly) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (adminOnly ? !"ADMIN".equals(normalized) : !("ADMIN".equals(normalized) || "FIELD_RESEARCHER".equals(normalized) || "VERIFIED_RECYCLER".equals(normalized) || "VERIFIED_RECYCLER_SUBMITTING_OWN_QUOTE".equals(normalized)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role for price records");
    }
    private boolean isVerifier(String role) { return "ADMIN".equalsIgnoreCase(role); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
