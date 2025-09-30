package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.repositories.TicketConsumptionDetailRepository;
import com.packed_go.event_service.services.TicketConsumptionDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketConsumptionDetailServiceImpl implements TicketConsumptionDetailService {
    private final ConsumptionRepository consumptionRepository;
    private final TicketConsumptionDetailRepository ticketConsumptionDetailRepository;
    private final ModelMapper modelMapper;

    @Override
    public TicketConsumptionDetailDTO create(TicketConsumptionDetailDTO ticketConsumptionDetailDTO) {
        Consumption consumption = consumptionRepository.findById(ticketConsumptionDetailDTO.getConsumptionId()).orElseThrow(() -> new RuntimeException("Consumption with id " + ticketConsumptionDetailDTO.getConsumptionId() + " not found"));
        TicketConsumptionDetail ticketConsumptionDetail = modelMapper.map(ticketConsumptionDetailDTO, TicketConsumptionDetail.class);
        ticketConsumptionDetail.setConsumption(consumption);
        TicketConsumptionDetail saved = ticketConsumptionDetailRepository.save(ticketConsumptionDetail);
        return modelMapper.map(saved, TicketConsumptionDetailDTO.class);

    }

    @Override
    public TicketConsumptionDetailDTO update(Long id, TicketConsumptionDetailDTO dto) {

        TicketConsumptionDetail existingDetail = ticketConsumptionDetailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TicketConsumptionDetail with id " + id + " not found"));

        Consumption consumption = consumptionRepository.findById(dto.getConsumptionId())
                .orElseThrow(() -> new RuntimeException("Consumption with id " + dto.getConsumptionId() + " not found"));

        if (!consumption.isActive()) {
            throw new RuntimeException("Consumption with id " + dto.getConsumptionId() + " is not active");
        }

        // Mapear todos los campos del DTO al detalle existente usando ModelMapper
        modelMapper.map(dto, existingDetail);

        // Sobrescribir el objeto Consumption con la entidad validada
        existingDetail.setConsumption(consumption);

        // Actualizar el priceAtPurchase con el precio actual del consumption
        existingDetail.setPriceAtPurchase(consumption.getPrice());

        // Guardar y retornar
        TicketConsumptionDetail updated = ticketConsumptionDetailRepository.save(existingDetail);
        return modelMapper.map(updated, TicketConsumptionDetailDTO.class);
    }

    @Override
    public List<TicketConsumptionDetailDTO> findAllByTicketId(Long id) {
        List<TicketConsumptionDetail> ticketDetails = ticketConsumptionDetailRepository.findByTicketConsumption_Id(id);
        return ticketDetails.stream().map(entity -> modelMapper.map(entity, TicketConsumptionDetailDTO.class)).toList();

    }

    @Override
    public List<TicketConsumptionDetailDTO> findAllByConsumptionName(String name) {
        List<TicketConsumptionDetail> ticketDetails = ticketConsumptionDetailRepository.findByConsumption_Name(name);
        return ticketDetails.stream().map(entity -> modelMapper.map(entity, TicketConsumptionDetailDTO.class)).toList();
    }

    @Transactional
    @Override
    public TicketConsumptionDetail deleteLogical(Long id) {
        Optional<TicketConsumptionDetail> ticketDetail = ticketConsumptionDetailRepository.findById(id);
        if (ticketDetail.isPresent()) {
            TicketConsumptionDetail ticket = modelMapper.map(ticketDetail.get(), TicketConsumptionDetail.class);
            ticket.setActive(false);
            ticketConsumptionDetailRepository.save(ticket);
            return modelMapper.map(ticket, TicketConsumptionDetail.class);
        } else {
            throw new RuntimeException("TicketConsumptionDetail with id " + id + " not found");
        }
    }

}
