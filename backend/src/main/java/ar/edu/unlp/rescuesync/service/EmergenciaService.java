package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.bonita.BonitaSession;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.NuevaEmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.NuevoLoteDTO;
import ar.edu.unlp.rescuesync.dto.PublicarLotesDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.entity.Municipio;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.LoteRepository;
import ar.edu.unlp.rescuesync.repository.MunicipioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import ar.edu.unlp.rescuesync.dto.LoteDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EmergenciaService {

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;          // para crear el case en Bonita y completar tareas
    private final LoteRepository loteRepository;      // NUEVO: para guardar los lotes

    // Spring pasa todas las dependencias por el constructor (inyección de dependencias)
    public EmergenciaService(
            EmergenciaRepository emergenciaRepository,
            MunicipioRepository municipioRepository,
            BonitaClient bonitaClient,
            LoteRepository loteRepository) {

        this.emergenciaRepository = emergenciaRepository;
        this.municipioRepository = municipioRepository;
        this.bonitaClient = bonitaClient;
        this.loteRepository = loteRepository;
    }

    // Alta de emergencia de punta a punta: Postgres + Bonita.
    // @Transactional: si algo falla (incluido Bonita), se deshace lo guardado en Postgres.
    @Transactional
    public EmergenciaDTO registrarEmergencia(NuevaEmergenciaDTO dto, UsuarioLogueado usuario) {

        // 1. El municipio sale del usuario logueado (antes estaba fijo en 1)
        Municipio m = municipioRepository
                .findById(usuario.organizacionId())
                .orElseThrow(() -> new IllegalStateException(
                        "El usuario no pertenece a un municipio válido"));

        // 2. Guardamos la emergencia en Postgres (así obtenemos su id)
        Emergencia e = new Emergencia(
                dto.getZonaAfectada(),
                dto.getDescripcion(),
                dto.getTipoDesastre(),
                dto.getGravedad(),
                m
        );
        m.cargarEmergencia(e);
        Emergencia guardada = emergenciaRepository.save(e);

        // 3. En Bonita, con la sesión del usuario que carga la emergencia:
        BonitaSession sesion = usuario.bonita();

        //    3a. crear el case (una "copia viva" del proceso para esta emergencia)
        long caseId = bonitaClient.instanciar(sesion);

        //    3b. buscar la primera tarea del case: "Registrar Emergencia"
        long taskId = bonitaClient.buscarTareaLista(sesion, caseId);

        //    3c. completarla mandando el contract -> Bonita guarda emergenciaId = id de Postgres
        //        y el case avanza a "Generar lotes" (tarea del coordinador)
        bonitaClient.ejecutarTarea(sesion, taskId, Map.of("emergenciaIdInput", guardada.getId()));

        // 4. Guardamos el vínculo: esta emergencia <-> este case de Bonita
        //    (no hace falta save: la entidad está "administrada" y JPA la actualiza al terminar la transacción)
        guardada.setBonitaCaseId(caseId);

        return convertirADTO(guardada);
    }

    // El coordinador divide la emergencia en lotes y abre la convocatoria a las ONGs.
    // @Transactional: si Bonita falla, no quedan lotes guardados ni cambia el estado.
    @Transactional
    public EmergenciaDTO publicarLotes(Long emergenciaId, PublicarLotesDTO dto, UsuarioLogueado usuario) {

        // 1. REVISAR: que la emergencia exista, no esté publicada y los datos sean válidos
        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la emergencia " + emergenciaId));

        // Solo se publica una vez: si ya no está REGISTRADA, alguien ya lo hizo
        if (emergencia.getEstado() != EstadoEmergencia.REGISTRADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La emergencia ya fue publicada (estado: " + emergencia.getEstado() + ")");
        }

        if (dto.getLotes() == null || dto.getLotes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay que cargar al menos un lote");
        }

        if (dto.getDuracionMinutos() == null || dto.getDuracionMinutos() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La duración debe ser de al menos 1 minuto");
        }

        // 2. GUARDAR EN POSTGRES: cada lote asociado a la emergencia
        for (NuevoLoteDTO nuevo : dto.getLotes()) {
            if (nuevo.getRecurso() == null || nuevo.getRecurso().isBlank() || nuevo.getCantidad() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada lote necesita un recurso y una cantidad mayor a 0");
            }
            Lote lote = new Lote(nuevo.getRecurso(), nuevo.getCantidad(), emergencia);
            emergencia.addLote(lote);          // mantiene la relación en los dos sentidos
            loteRepository.save(lote);
        }

        //    La ventana de ofertas cierra dentro de N minutos
        //    (el backend la usa después para rechazar ofertas que lleguen tarde)
        emergencia.setFechaCierreOfertas(LocalDateTime.now().plusMinutes(dto.getDuracionMinutos()));
        emergencia.setEstado(EstadoEmergencia.EN_CONVOCATORIA);

        // 3. AVISARLE A BONITA: completar "Generar lotes" con la sesión del coordinador
        BonitaSession sesion = usuario.bonita();
        long taskId = bonitaClient.buscarTareaLista(sesion, emergencia.getBonitaCaseId());

        long duracionMs = dto.getDuracionMinutos() * 60_000L;   // Bonita espera milisegundos (L = long)
        bonitaClient.ejecutarTarea(sesion, taskId, Map.of("duracionVentanaMsInput", duracionMs));
        //    -> Bonita guarda duracionVentanaMs y el case pasa a "Cargar o editar ofertas" con el TIMER andando

        // 4. DEVOLVER: no hace falta save(emergencia), JPA guarda los cambios al cerrar la transacción
        return convertirADTO(emergencia);
    }

    @Transactional(readOnly = true)
    public List<EmergenciaDTO> listarMisEmergencias(UsuarioLogueado usuario) {
        Long municipioId = usuario.organizacionId();

        if (municipioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no tiene un municipio asociado"
            );
        }

        return emergenciaRepository
                .findByMunicipio_IdOrderByIdDesc(municipioId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> consultarLotes(
            Long emergenciaId,
            UsuarioLogueado usuario
    ) {
        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe la emergencia"
                ));

        // La convocatoria debe estar disponible para esta ONG en Bonita.
        boolean tieneAcceso = bonitaClient.tareasPendientes(usuario.bonita())
                .stream()
                .anyMatch(tarea ->
                        "Cargar o editar ofertas".equals(tarea.get("name"))
                                && String.valueOf(emergencia.getBonitaCaseId())
                                .equals(String.valueOf(tarea.get("caseId")))
                );

        if (!tieneAcceso) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La convocatoria no está disponible para tu organización"
            );
        }

        if (emergencia.getEstado() != EstadoEmergencia.EN_CONVOCATORIA
                || emergencia.getFechaCierreOfertas() == null
                || !emergencia.getFechaCierreOfertas()
                .isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La convocatoria ya no está vigente"
            );
        }

        return loteRepository.findByEmergencia_IdOrderByIdAsc(emergenciaId)
                .stream()
                .map(lote -> {
                    LoteDTO dto = new LoteDTO();
                    dto.setId(lote.getId());
                    dto.setRecurso(lote.getRecurso());
                    dto.setCantidad(lote.getCantidad());
                    dto.setEmergenciaId(emergenciaId);
                    return dto;
                })
                .toList();
    }

    private EmergenciaDTO convertirADTO(Emergencia emergencia) {
       return EmergenciaDTO.desde(emergencia);

    }

}
