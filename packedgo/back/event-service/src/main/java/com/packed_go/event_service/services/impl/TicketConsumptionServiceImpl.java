package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.TicketConsumption;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.repositories.TicketConsumptionDetailRepository;
import com.packed_go.event_service.repositories.TicketConsumptionRepository;
import com.packed_go.event_service.services.TicketConsumptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TicketConsumptionServiceImpl implements TicketConsumptionService {
    private final ModelMapper modelMapper;
    private final TicketConsumptionRepository ticketConsumptionRepository;
    private final ConsumptionRepository consumptionRepository;
    private final TicketConsumptionDetailRepository ticketConsumptionDetailRepository;


    @Override
    @Transactional
    public TicketConsumptionDTO create(TicketConsumptionDTO ticketConsumptionDTO) {
        TicketConsumption ticket = new TicketConsumption();
        ticket.setActive(true);
        ticket = ticketConsumptionRepository.save(ticket);

        List<TicketConsumptionDetail> details = new ArrayList<>();
        for (TicketConsumptionDetailDTO detailDto : ticketConsumptionDTO.getTicketDetails()) {
            //Validar existencia y que esté activo
            Consumption consumption = consumptionRepository.findById(detailDto.getConsumptionId())
                    .orElseThrow(() -> new RuntimeException("Consumption with id " + detailDto.getConsumptionId() + " not found"));
            if (!consumption.isActive()) {
                throw new RuntimeException("Consumption with id " + detailDto.getConsumptionId() + " is not active");
            }

            //Mapeamos DTOS a Entidades
            TicketConsumptionDetail detail = modelMapper.map(detailDto, TicketConsumptionDetail.class);
            detail.setTicketConsumption(ticket);
            detail.setConsumption(consumption);

            //Guardamos el preico historico
            detail.setPriceAtPurchase(consumption.getPrice());

            details.add(detail);
        }

        //Guardamos todos los detalles en batch
        ticketConsumptionDetailRepository.saveAll(details);

        //Asociar detalles al ticket

        ticket.setConsumptionDetails(details);


        return modelMapper.map(ticket, TicketConsumptionDTO.class);

    }

    @Override
    public TicketConsumption update(Long id, TicketConsumptionDTO ticketConsumptionDTO) {
        return null;
    }

    @Override
    public TicketConsumption deleteLogical(Long id) {
        return null;
    }

    @Override
    public boolean redeemTicketDetails() {
        return false;
    }

    @Override
    public List<TicketConsumptionDetailDTO> getTicketConsumptionDetails(Long id) {
        return List.of();
    }
}
