package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.Ong;

import java.time.LocalDateTime;
import java.util.List;

// Perfil completo de una ONG: estado, certificaciones (vigentes y vencidas) e inventario
public record OngDetalleDTO(
        String cuit,
        String nombre,
        String email,
        String provincia,
        String estado,
        String motivoEstado,
        LocalDateTime fechaAlta,
        List<CertificacionOngDTO> certificaciones,
        List<InventarioDTO> inventario
) {
    public static OngDetalleDTO desde(Ong o) {
        return new OngDetalleDTO(
                o.getCuit(),
                o.getNombre(),
                o.getEmail(),
                o.getProvincia(),
                o.getEstado().name(),
                o.getMotivoEstado(),
                o.getFechaAlta(),
                o.getCertificaciones().stream().map(CertificacionOngDTO::desde).toList(),
                o.getInventario().stream().map(InventarioDTO::desde).toList()
        );
    }
}
