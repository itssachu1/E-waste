package com.janvoice.ai.service.impl;

import com.janvoice.ai.dto.LotRecyclerRequest;
import com.janvoice.ai.dto.LotRequest;
import com.janvoice.ai.dto.LotStatusRequest;
import com.janvoice.ai.entity.*;
import com.janvoice.ai.repository.*;
import com.janvoice.ai.service.LotService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LotServiceImpl implements LotService {
    private final LotRepository lots;
    private final LotCounterRepository counters;
    private final MaterialMasterRepository materials;
    private final PriceRecordRepository prices;
    private final RecyclerRepository recyclers;

    public LotServiceImpl(LotRepository lots, LotCounterRepository counters, MaterialMasterRepository materials, PriceRecordRepository prices, RecyclerRepository recyclers) {
        this.lots = lots; this.counters = counters; this.materials = materials; this.prices = prices; this.recyclers = recyclers;
    }

    @Override @Transactional
    public Map<String, Object> create(LotRequest request, User collector) {
        requireCollector(collector);
        MaterialMaster material = materials.findById(request.getMaterialId()).filter(MaterialMaster::isActive).orElseThrow(() -> bad("material_id must reference an active Material Master record"));
        Lot lot = new Lot(); lot.setLotReference(nextReference()); lot.setCollector(collector); lot.setMaterial(material); lot.setDescription(request.getDescription()); lot.setPhotoUrl(request.getPhotoUrl()); lot.setApproximateWeight(request.getApproximateWeight()); lot.setStatus(Lot.Status.CREATED);
        if (request.getSelectedPriceRecordId() != null) {
            PriceRecord price = prices.findById(request.getSelectedPriceRecordId()).orElseThrow(() -> bad("selected_price_record_id was not found"));
            if (!price.getMaterial().getId().equals(material.getId())) throw bad("selected price record must match material_id");
            if (expired(price)) throw bad("selected price record is expired");
            lot.setSelectedPriceRecord(price); lot.setEstimatedValue(price.getRate().multiply(request.getApproximateWeight()).setScale(2, RoundingMode.HALF_UP));
        }
        return response(lots.save(lot));
    }

    @Override @Transactional
    public List<Map<String, Object>> find(User actor, String status) {
        requireAuthenticated(actor);
        List<Lot> result = "ADMIN".equalsIgnoreCase(actor.getRole()) ? lots.findAll() : lots.findByCollectorOrderByCreatedAtDesc(actor);
        if (status != null) { Lot.Status requested = parseStatus(status); result = result.stream().filter(lot -> lot.getStatus() == requested).collect(Collectors.toList()); }
        return result.stream().map(this::response).collect(Collectors.toList());
    }

    @Override @Transactional
    public Map<String, Object> findById(Long id, User actor) {
        requireAuthenticated(actor); Lot lot = lots.findById(id).orElseThrow(() -> notFound("Lot not found"));
        if (!canAccess(lot, actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this lot");
        return response(lot);
    }

    @Override @Transactional
    public Map<String, Object> updateStatus(Long id, LotStatusRequest request, User actor) {
        requireAuthenticated(actor); Lot lot = lots.findById(id).orElseThrow(() -> notFound("Lot not found"));
        Lot.Status target = request.getStatus();
        if (target == Lot.Status.QUOTE_RECEIVED || target == Lot.Status.HANDED_OVER || target == Lot.Status.RECYCLER_CONFIRMED || target == Lot.Status.PAID) throw bad("This status is reserved for a later phase");
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) throw bad("Admin cannot bypass the lot state machine");
        boolean owner = lot.getCollector().getId().equals(actor.getId());
        if (owner) {
            if (!lot.getCollector().getId().equals(actor.getId()) || !allowedCollector(lot.getStatus(), target)) throw bad("Invalid collector status transition");
        } else if (!isRecyclerActor(lot, actor) || lot.getStatus() != Lot.Status.QUOTE_REQUESTED || target != Lot.Status.QUOTE_RECEIVED) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the lot owner or matched recycler may make this transition");
        lot.setStatus(target); return response(lots.save(lot));
    }

    @Override @Transactional
    public Map<String, Object> assignRecycler(Long id, LotRecyclerRequest request, User actor) {
        requireAuthenticated(actor); Lot lot = lots.findById(id).orElseThrow(() -> notFound("Lot not found"));
        if (!lot.getCollector().getId().equals(actor.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the collector may select a recycler");
        Recycler recycler = recyclers.findById(request.getRecyclerId()).filter(Recycler::isActive).orElseThrow(() -> notFound("Recycler not found"));
        if (recycler.getAuthorizationStatus() == Recycler.AuthorizationStatus.REJECTED) throw bad("Rejected recyclers cannot be selected");
        if (recycler.getAcceptedMaterials().stream().noneMatch(m -> m.getId().equals(lot.getMaterial().getId()))) throw bad("Recycler does not accept this material");
        lot.setRecycler(recycler); return response(lots.save(lot));
    }

    private String nextReference() { int year = LocalDate.now().getYear(); return String.format("EWS-%04d-%06d", year, counters.nextNumber(year)); }
    private boolean expired(PriceRecord p) { return p.getValidUntil() != null ? p.getValidUntil().isBefore(LocalDateTime.now()) : p.getQuotedAt().plusDays(30).isBefore(LocalDateTime.now()); }
    private boolean allowedCollector(Lot.Status from, Lot.Status to) { return (from == Lot.Status.CREATED && (to == Lot.Status.QUOTE_REQUESTED || to == Lot.Status.CANCELLED)) || (from == Lot.Status.ACCEPTED && (to == Lot.Status.READY_FOR_HANDOVER || to == Lot.Status.CANCELLED)); }
    private boolean isRecyclerActor(Lot lot, User actor) { return lot.getRecycler() != null && lot.getRecycler().getCreatedBy() != null && lot.getRecycler().getCreatedBy().equals(actor.getId()); }
    private boolean canAccess(Lot lot, User actor) { return "ADMIN".equalsIgnoreCase(actor.getRole()) || lot.getCollector().getId().equals(actor.getId()) || isRecyclerActor(lot, actor); }
    private void requireCollector(User actor) { requireAuthenticated(actor); if (!"COLLECTOR".equalsIgnoreCase(actor.getRole()) && !"CITIZEN".equalsIgnoreCase(actor.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Collector role required"); }
    private void requireAuthenticated(User actor) { if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session token required"); }
    private Lot.Status parseStatus(String value) { try { return Lot.Status.valueOf(value.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { throw bad("Unknown lot status"); } }
    private Map<String, Object> response(Lot lot) { Map<String,Object> map=new LinkedHashMap<>(); map.put("id",lot.getId()); map.put("lot_reference",lot.getLotReference()); map.put("collector_id",lot.getCollector().getId()); map.put("material_id",lot.getMaterial().getId()); map.put("material_name",lot.getMaterial().getCommonName()); map.put("description",lot.getDescription()); map.put("photo_url",lot.getPhotoUrl()); map.put("approximate_weight",lot.getApproximateWeight()); map.put("final_weight",lot.getFinalWeight()); map.put("estimated_value",lot.getEstimatedValue()); map.put("selected_price_record_id",lot.getSelectedPriceRecord()==null?null:lot.getSelectedPriceRecord().getId()); map.put("final_sale_amount",lot.getFinalSaleAmount()); map.put("recycler_id",lot.getRecycler()==null?null:lot.getRecycler().getId()); map.put("status",lot.getStatus()); map.put("created_at",lot.getCreatedAt()); map.put("updated_at",lot.getUpdatedAt()); return map; }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);} private ResponseStatusException notFound(String message){return new ResponseStatusException(HttpStatus.NOT_FOUND,message);}
}
