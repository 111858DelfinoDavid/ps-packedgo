package com.packed_go.event_service.config;

import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.entities.Event;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.modelmapper.convention.MatchingStrategies;

@Configuration
public class MappersConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        GeometryFactory geometryFactory = new GeometryFactory();

        // Mapeo de DTO a Entidad
        modelMapper.typeMap(EventDTO.class, Event.class)
                .addMappings(mapper -> {
                    mapper.using((MappingContext<EventDTO, Point> context) -> {
                        EventDTO dto = context.getSource();
                        // Crea una nueva coordenada con la longitud y latitud del DTO
                        Coordinate coord = new Coordinate(dto.getLng(), dto.getLat());
                        // Usa GeometryFactory para crear el objeto Point
                        return geometryFactory.createPoint(coord);
                    }).map(src -> src, Event::setLocation);
                });

        // Configuración para el mapeo de Entidad a DTO
        modelMapper.typeMap(Event.class, EventDTO.class)
                .addMappings(mapper -> {
                    mapper.map(src -> src.getLocation().getY(), EventDTO::setLat);
                    mapper.map(src -> src.getLocation().getX(), EventDTO::setLng);
                });

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // Configura la estrategia de mapeo para evitar coincidencias laxas
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Agrega un TypeMap explícito para el mapeo de DTO a Entidad
        modelMapper.createTypeMap(CreateEventDTO.class, Event.class)
                .addMappings(mapper -> mapper.using(context -> {
                    // El contexto de mapeo contiene la instancia del DTO fuente
                    CreateEventDTO source = (CreateEventDTO) context.getSource();
                    // Crea un objeto Coordinate con la latitud y longitud del DTO
                    Coordinate coordinate = new Coordinate(source.getLng(), source.getLat());
                    // Usa GeometryFactory para crear un objeto Point con el SRID 4326

                    Point point = geometryFactory.createPoint(coordinate);
                    // Establece el SRID (Spatial Reference System Identifier) a 4326
                    point.setSRID(4326);
                    return point;
                }).map(source -> source, Event::setLocation)); // Mapea el resultado al campo 'location' de la entidad
        return modelMapper;
    }
}