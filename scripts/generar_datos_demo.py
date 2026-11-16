#!/usr/bin/env python3
"""
Recorre el proceso completo de RescueSync por la API del backend, como lo harían los usuarios
desde la interfaz, para generar datos para el dashboard (después de cada Run de Studio, que borra los casos).

Escenarios (corren en paralelo, ventana de ofertas de 1 minuto):
  1. Inundación: ONG 2 no tiene certificación de logística, así que el Sistema Nacional no le valida las
     raciones -> "No cubre" -> el coordinador reabre la convocatoria -> ONG 1 ofrece las raciones sola
     y sube su aporte en el consorcio de paramédicos (versión 2 de la oferta) -> se cierra.
  2. Incendio: dos ofertas individuales -> se cierra.
  3. Terremoto: un lote que ninguna ONG puede cubrir -> "No cubre" -> el coordinador sigue con
     cobertura parcial -> queda en ejecución (con recursos comprometidos en el Sistema Nacional).

Las cantidades respetan el límite de la interfaz (1 a 100 por lote).
Requisitos: backend en :8081, Bonita (Studio) en :8080 con el proceso desplegado, Sistema Nacional accesible.
Uso: python3 scripts/generar_datos_demo.py
"""
import threading
import time

import requests

API = "http://localhost:8081/api"
CLAVE = "bpm"
ONG_ID = {"ong1": 3, "ong2": 4}   # ids locales de ONG 1 y ONG 2 (los crea el DataLoader)
ESPERA_MAX = 600                   # segundos esperando a Bonita en cada paso


def log(escenario, texto):
    print(f"[{time.strftime('%H:%M:%S')}] {escenario}: {texto}", flush=True)


class Usuario:
    def __init__(self, nombre):
        self.nombre = nombre
        self.s = requests.Session()
        r = self.s.post(f"{API}/auth/login", json={"username": nombre, "password": CLAVE}, timeout=30)
        r.raise_for_status()

    def _check(self, r):
        if not r.ok:
            raise RuntimeError(f"{self.nombre} {r.request.method} {r.url} -> {r.status_code}: {r.text[:300]}")
        return r.json() if r.text else None

    def get(self, ruta):
        return self._check(self.s.get(f"{API}{ruta}", timeout=150))

    def post(self, ruta, datos=None):
        return self._check(self.s.post(f"{API}{ruta}", json=datos, timeout=150))

    def put(self, ruta, datos):
        return self._check(self.s.put(f"{API}{ruta}", json=datos, timeout=150))

    def tareas(self):
        return self.get("/tareas")


def esperar(condicion, que, escenario, cada=3):
    """Reintenta hasta que condicion() devuelva algo verdadero (Bonita avanza de forma asíncrona)."""
    inicio = time.time()
    while time.time() - inicio < ESPERA_MAX:
        try:
            resultado = condicion()
            if resultado:
                return resultado
        except RuntimeError as e:
            if " 409" not in str(e):   # 409 = Bonita todavía no tiene lista la tarea
                raise
        time.sleep(cada)
    raise TimeoutError(f"{escenario}: no llegó '{que}' en {ESPERA_MAX} s")


def tarea_de(usuario, nombre, emergencia_id):
    return next((t for t in usuario.tareas()
                 if t["nombreTarea"] == nombre and t["emergencia"]["id"] == emergencia_id), None)


def estado(municipio, emergencia_id):
    return next(e["estado"] for e in municipio.get("/emergencias/mias") if e["id"] == emergencia_id)


def cargar_ofertas(escenario, eid, ongs, ofertas):
    por_codigo = {l["recursoCodigo"]: l["id"] for l in ongs["ong1"].get(f"/emergencias/{eid}/lotes")}
    for codigo, lider, cantidad, invitadas, *edicion in ofertas:
        o = ongs[lider].post("/ofertas", {
            "loteId": por_codigo[codigo], "cantidad": cantidad,
            "ongsInvitadas": [ONG_ID[n] for n, _ in invitadas]})
        for invitada, aporte in invitadas:   # la invitada acepta poniendo su cantidad
            time.sleep(1)
            ongs[invitada].put(f"/ofertas/{o['id']}/aporte", {"cantidad": aporte})
        texto = f"oferta {codigo}: {lider} {cantidad}" + "".join(f" + {n} {c}" for n, c in invitadas)
        if edicion:                           # la líder modifica su oferta dentro de la ventana
            time.sleep(3)
            ongs[lider].put(f"/ofertas/{o['id']}/aporte", {"cantidad": edicion[0]})
            texto += f" (después la edita a {edicion[0]})"
        log(escenario, texto)


def correr(escenario, emergencia, lotes, ofertas, cobertura_parcial=False, reabrir=None, cerrar=True, pausa=0):
    time.sleep(pausa)
    municipio, coord = Usuario("municipio1"), Usuario("coord1")
    ongs = {n: Usuario(n) for n in ("ong1", "ong2")}

    # 1. Municipio: registrar la emergencia (instancia el caso en Bonita)
    e = municipio.post("/emergencias", emergencia)
    eid = e["id"]
    log(escenario, f"emergencia #{eid} registrada")

    # 2. Coordinador: generar lotes y publicar la convocatoria (ventana de 1 minuto)
    time.sleep(8)
    esperar(lambda: tarea_de(coord, "Generar lotes", eid), "Generar lotes", escenario)
    coord.post(f"/emergencias/{eid}/lotes", {"duracionMinutos": 1, "lotes": lotes})
    log(escenario, "convocatoria publicada")

    # 3. ONGs: ofertas individuales o en consorcio
    time.sleep(3)
    cargar_ofertas(escenario, eid, ongs, ofertas)

    # 4. Vence el timer: Bonita pide las ofertas, las valida en el Sistema Nacional y evalúa la cobertura
    if reabrir:
        esperar(lambda: tarea_de(coord, "Evaluar curso de acción alternativo", eid), "No cubre", escenario)
        time.sleep(6)
        coord.post(f"/emergencias/{eid}/reapertura", {"duracionMinutos": 1, "lotes": reabrir["lotes"]})
        log(escenario, "no cubre: el coordinador reabre la convocatoria")
        time.sleep(3)
        cargar_ofertas(escenario, eid, ongs, reabrir["ofertas"])
    elif cobertura_parcial:
        esperar(lambda: tarea_de(coord, "Evaluar curso de acción alternativo", eid), "No cubre", escenario)
        time.sleep(6)
        coord.post(f"/emergencias/{eid}/cobertura-parcial")
        log(escenario, "no cubre: el coordinador sigue con cobertura parcial")

    esperar(lambda: tarea_de(municipio, "Adjudicar y notificar", eid), "Adjudicar y notificar", escenario)
    log(escenario, "ofertas validadas por el Sistema Nacional")

    # 5. Municipio: adjudica la primera oferta validada de cada lote que tenga opciones
    time.sleep(10)
    opciones = municipio.get(f"/emergencias/{eid}/adjudicacion")
    selecciones = [{"loteId": l["loteId"], "ofertaId": l["ofertas"][0]["id"]}
                   for l in opciones["lotes"] if l["ofertas"]]
    municipio.post(f"/emergencias/{eid}/adjudicacion", {"selecciones": selecciones})
    log(escenario, f"adjudicados {len(selecciones)} lotes")

    # 6. Bonita compromete los recursos en el Sistema Nacional -> el coordinador inicia el despliegue
    esperar(lambda: tarea_de(coord, "Monitorear estado de despliegue", eid), "Monitorear", escenario)
    time.sleep(12)
    coord.post(f"/emergencias/{eid}/despliegue/inicio")
    log(escenario, "recursos comprometidos, despliegue iniciado")
    if not cerrar:
        log(escenario, "queda en ejecución")
        return

    # 7. Cada ONG marca sus actividades como finalizadas; con la última, Bonita cierra el proyecto
    despliegue = coord.get(f"/emergencias/{eid}/despliegue")
    participantes = sorted({p["ongNombre"] for a in despliegue["asignaciones"] for p in a["ongs"]})
    for nombre in participantes:
        time.sleep(15)
        usuario = ongs["ong1" if nombre == "ONG 1" else "ong2"]
        esperar(lambda: usuario.post(f"/emergencias/{eid}/finalizacion"), "finalización", escenario)
        log(escenario, f"{nombre} finalizó")

    esperar(lambda: estado(municipio, eid) == "CERRADA", "cierre", escenario)
    log(escenario, "proyecto cerrado y recursos liberados")


ESCENARIOS = [
    dict(escenario="Inundación", pausa=0,
         emergencia={"zonaAfectada": "Barrio La Ribera, Ensenada", "tipoDesastre": "INUNDACION", "gravedad": "ALTA",
                     "descripcion": "Desborde del arroyo tras 180 mm de lluvia; familias evacuadas."},
         lotes=[{"recursoCodigo": "AGUA_POTABLE", "cantidad": 100},
                {"recursoCodigo": "PARAMEDICO", "cantidad": 8},
                {"recursoCodigo": "RACION_ALIMENTO", "cantidad": 90}],
         ofertas=[("AGUA_POTABLE", "ong1", 100, []),
                  ("PARAMEDICO", "ong1", 5, [("ong2", 3)]),
                  ("RACION_ALIMENTO", "ong1", 60, [("ong2", 30)])],
         reabrir={"lotes": [{"recursoCodigo": "AGUA_POTABLE", "cantidad": 100},
                            {"recursoCodigo": "PARAMEDICO", "cantidad": 8},
                            {"recursoCodigo": "RACION_ALIMENTO", "cantidad": 90}],
                  "ofertas": [("AGUA_POTABLE", "ong1", 100, []),
                              ("PARAMEDICO", "ong1", 4, [("ong2", 3)], 5),
                              ("RACION_ALIMENTO", "ong1", 90, [])]}),
    dict(escenario="Incendio", pausa=20,
         emergencia={"zonaAfectada": "Sierra de la Ventana", "tipoDesastre": "INCENDIO", "gravedad": "ALTA",
                     "descripcion": "Incendio forestal en avance sobre zona de cabañas."},
         lotes=[{"recursoCodigo": "BOMBERO_FORESTAL", "cantidad": 12},
                {"recursoCodigo": "KIT_MEDICO", "cantidad": 30}],
         ofertas=[("BOMBERO_FORESTAL", "ong2", 12, []),
                  ("KIT_MEDICO", "ong1", 30, [])]),
    dict(escenario="Terremoto", pausa=40, cobertura_parcial=True, cerrar=False,
         emergencia={"zonaAfectada": "San Juan capital", "tipoDesastre": "TERREMOTO", "gravedad": "MEDIA",
                     "descripcion": "Sismo de magnitud 5,8; daños en viviendas antiguas."},
         lotes=[{"recursoCodigo": "RESCATISTA_USAR", "cantidad": 4},
                {"recursoCodigo": "CARPA", "cantidad": 20},
                {"recursoCodigo": "VOLUNTARIO_LOGISTICA", "cantidad": 10}],
         ofertas=[("CARPA", "ong2", 20, []),
                  ("VOLUNTARIO_LOGISTICA", "ong2", 10, [])]),
]

if __name__ == "__main__":
    errores = []

    def ejecutar(cfg):
        try:
            correr(**cfg)
        except Exception as ex:   # noqa: BLE001 - se informa y siguen los otros escenarios
            errores.append(f"{cfg['escenario']}: {ex}")
            log(cfg["escenario"], f"ERROR {ex}")

    hilos = [threading.Thread(target=ejecutar, args=(c,)) for c in ESCENARIOS]
    for h in hilos:
        h.start()
    for h in hilos:
        h.join()
    print("\nTerminado." if not errores else "\nCon errores:\n" + "\n".join(errores))
