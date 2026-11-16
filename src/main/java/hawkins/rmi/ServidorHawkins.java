package hawkins.rmi;

import hawkins.eventos.GestorEventos;
import hawkins.eventos.TipoEvento;
import hawkins.hilos.ControlSimulacion;
import hawkins.hilos.Demogorgon;
import hawkins.zonas.comunes.Portal;
import hawkins.zonas.mundoreal.CallePrincipal;
import hawkins.zonas.mundoreal.RadioWSQK;
import hawkins.zonas.mundoreal.SotanoByers;
import hawkins.zonas.upsidedown.Alcantarillado;
import hawkins.zonas.upsidedown.Bosque;
import hawkins.zonas.upsidedown.CentroComercial;
import hawkins.zonas.upsidedown.Colmena;
import hawkins.zonas.upsidedown.Laboratorio;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementación del servidor RMI para la simulación de Hawkins.
 * 
 * Expone de forma remota los métodos definidos en {@link InterfaceHawkins},
 * permitiendo a los clientes conectarse y consultar el estado de las zonas,
 * los niños, los Demogorgons, los eventos y controlar la ejecución
 * (pausa/reanudación).
 */
public class ServidorHawkins extends UnicastRemoteObject implements InterfaceHawkins {

    // ── Referencias al modelo ─────────────────────────────────────────────────
    private final CallePrincipal callePrincipal;
    private final SotanoByers sotanoByers;
    private final RadioWSQK radioWSQK;
    private final Bosque bosque;
    private final Laboratorio laboratorio;
    private final CentroComercial centroComercial;
    private final Alcantarillado alcantarillado;
    private final Colmena colmena;
    private final Portal[] portales;
    private final GestorEventos gestorEventos;
    private final List<Demogorgon> demogorgonsActivos;
    private final ControlSimulacion controlSimulacion;

    /**
     * Constructor que activa el objeto remoto.
     * Recibe todas las dependencias del modelo construidas en Main.
     * 
     * @param callePrincipal     Referencia a la zona Calle Principal.
     * @param sotanoByers        Referencia a la zona Sótano de los Byers.
     * @param radioWSQK          Referencia a la zona Radio WSQK.
     * @param bosque             Referencia a la zona Bosque del Upside Down.
     * @param laboratorio        Referencia a la zona Laboratorio del Upside Down.
     * @param centroComercial    Referencia a la zona Centro Comercial del Upside
     *                           Down.
     * @param alcantarillado     Referencia a la zona Alcantarillado del Upside
     *                           Down.
     * @param colmena            Referencia a la Colmena.
     * @param portales           Array con los portales disponibles.
     * @param gestorEventos      Gestor de los eventos globales.
     * @param demogorgonsActivos Lista de los Demogorgons actualmente activos.
     * @param controlSimulacion  Controlador del estado de pausa/reanudación.
     * @throws RemoteException Si ocurre un error al exportar el objeto remoto.
     */
    public ServidorHawkins(
            CallePrincipal callePrincipal,
            SotanoByers sotanoByers,
            RadioWSQK radioWSQK,
            Bosque bosque,
            Laboratorio laboratorio,
            CentroComercial centroComercial,
            Alcantarillado alcantarillado,
            Colmena colmena,
            Portal[] portales,
            GestorEventos gestorEventos,
            List<Demogorgon> demogorgonsActivos,
            ControlSimulacion controlSimulacion) throws RemoteException {

        super();
        this.callePrincipal = callePrincipal;
        this.sotanoByers = sotanoByers;
        this.radioWSQK = radioWSQK;
        this.bosque = bosque;
        this.laboratorio = laboratorio;
        this.centroComercial = centroComercial;
        this.alcantarillado = alcantarillado;
        this.colmena = colmena;
        this.portales = portales;
        this.gestorEventos = gestorEventos;
        this.demogorgonsActivos = demogorgonsActivos;
        this.controlSimulacion = controlSimulacion;
    }

    // Imprementación de los métodos remotos definidos en la InterfaceHawkins

    /**
     * Calcula y devuelve el número total de niños que se encuentran en el mundo
     * real (Hawkins).
     * Suma los niños de la Calle Principal, el Sótano de los Byers y la Radio WSQK.
     */
    @Override
    public int getNinosHawkins() throws RemoteException {
        System.out.println("[RMI] Petición getNinosHawkins recibida.");
        try {
            if (callePrincipal != null && sotanoByers != null && radioWSQK != null) {
                return callePrincipal.getNumNinos() + sotanoByers.getNumNinos() + radioWSQK.getNumNinos();
            } else {
                System.err.println("[ServidorHawkins] WARN: Zonas de Hawkins son null.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getNinosHawkins: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al contar niños en Hawkins", e);
        }
    }

    /**
     * Devuelve el número de niños esperando para cruzar en cada portal.
     * El array devuelto sigue el orden:
     * [0] Bosque
     * [1] Laboratorio
     * [2] Centro Comercial
     * [3] Alcantarillado.
     */
    @Override
    public int[] getNinosPortales() throws RemoteException {
        // Orden: Bosque(0), Laboratorio(1), CentroComercial(2), Alcantarillado(3)
        System.out.println("[RMI] Petición getNinosPortales recibida.");
        int[] contador = new int[4];
        try {
            if (portales != null && portales.length >= 4) {
                for (int i = 0; i < 4; i++) {
                    if (portales[i] != null) {
                        contador[i] = portales[i].getNumEsperandoGrupo();
                    }
                }
            } else {
                System.err.println("[ServidorHawkins] WARN: Array de portales nulo o incompleto.");
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getNinosPortales: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al contar niños en portales", e);
        }

        return contador;
    }

    /**
     * Devuelve el número de niños explorando cada zona del Upside Down.
     * El array devuelto sigue el orden:
     * [0] Bosque
     * [1] Laboratorio
     * [2] Centro Comercial
     * [3] Alcantarillado.
     */
    @Override
    public int[] getNinosUpsideDown() throws RemoteException {
        // Orden: Bosque(0), Laboratorio(1), CentroComercial(2), Alcantarillado(3)
        System.out.println("[RMI] Petición getNinosUpsideDown recibida.");
        int[] counts = new int[4];
        try {
            if (bosque != null && laboratorio != null && centroComercial != null && alcantarillado != null) {
                counts[0] = bosque.getNumNinos();
                counts[1] = laboratorio.getNumNinos();
                counts[2] = centroComercial.getNumNinos();
                counts[3] = alcantarillado.getNumNinos();
            } else {
                System.err.println("[ServidorHawkins] WARN: Zonas del Upside Down son null.");
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getNinosUpsideDown: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al contar niños en Upside Down", e);
        }
        return counts;
    }

    /**
     * Consulta y devuelve la cantidad de niños que han sido capturados y se
     * encuentran en la Colmena.
     */
    @Override
    public int getNinosCapturadosEnColmena() throws RemoteException {
        System.out.println("[RMI] Petición getNinosCapturadosEnColmena recibida.");
        try {
            if (colmena != null) {
                return colmena.getNumNinosCapturados();
            } else {
                System.err.println("[ServidorHawkins] WARN: Colmena es null.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getNinosCapturadosEnColmena: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al contar niños capturados", e);
        }
    }

    /**
     * Devuelve el número de Demogorgons presentes en cada zona del Upside Down.
     * El array devuelto sigue el orden:
     * [0] Bosque
     * [1] Laboratorio
     * [2] Centro Comercial
     * [3] Alcantarillado.
     */
    @Override
    public int[] getDemogorgonsUpsideDown() throws RemoteException {
        // Orden: Bosque(0), Laboratorio(1), CentroComercial(2), Alcantarillado(3)
        System.out.println("[RMI] Petición getDemogorgonsUpsideDown recibida.");
        int[] counts = new int[4];
        try {
            if (bosque != null && laboratorio != null && centroComercial != null && alcantarillado != null) {
                counts[0] = bosque.getNumDemogorgons();
                counts[1] = laboratorio.getNumDemogorgons();
                counts[2] = centroComercial.getNumDemogorgons();
                counts[3] = alcantarillado.getNumDemogorgons();
            } else {
                System.err.println("[ServidorHawkins] WARN: Zonas del Upside Down son null.");
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getDemogorgonsUpsideDown: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al contar demogorgons", e);
        }
        return counts;
    }

    /**
     * Genera y devuelve una lista con el top 3 de Demogorgons con más capturas.
     */
    @Override
    public List<String> getRankingDemogorgons() throws RemoteException {
        // Copia para no bloquear la lista original durante el ordenamiento
        System.out.println("[RMI] Petición getRankingDemogorgons recibida.");
        List<String> ranking = new ArrayList<>();
        try {
            if (demogorgonsActivos == null) {
                System.err.println("[ServidorHawkins] WARN: demogorgonsActivos es null.");
                return ranking;
            }

            List<Demogorgon> copia;
            synchronized (demogorgonsActivos) {
                copia = new ArrayList<>(demogorgonsActivos);
            }

            copia.sort(Comparator.comparingInt(Demogorgon::getCapturas).reversed());

            int limite = Math.min(3, copia.size());
            for (int i = 0; i < limite; i++) {
                Demogorgon d = copia.get(i);
                if (d != null) {
                    ranking.add(d.getNombre() + ": " + d.getCapturas() + " capturas");
                }
            }
            if (ranking.isEmpty()) {
                ranking.add("Sin demogorgons activos");
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getRankingDemogorgons: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al calcular ranking", e);
        }
        return ranking;
    }

    /**
     * Obtiene y formatea el nombre del evento global que se encuentra activo en la
     * simulación.
     */
    @Override
    public String getEventoActivo() throws RemoteException {
        System.out.println("[RMI] Petición getEventoActivo recibida.");
        try {
            if (gestorEventos != null) {
                TipoEvento evento = gestorEventos.getEventoActivo();
                if (evento == null)
                    return "Sin evento activo";
                switch (evento) {
                    case APAGON_LABORATORIO:
                        return "Apagón del Laboratorio";
                    case TORMENTA_UPSIDE_DOWN:
                        return "Tormenta del Upside Down";
                    case INTERVENCION_ELEVEN:
                        return "Intervención de Eleven";
                    case RED_MENTAL:
                        return "La Red Mental";
                    default:
                        return evento.name();
                }
            } else {
                System.err.println("[ServidorHawkins] WARN: gestorEventos es null.");
                return "Error";
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getEventoActivo: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al obtener evento", e);
        }
    }

    /**
     * Consulta el tiempo restante (en segundos) para que finalice el evento activo.
     */
    @Override
    public long getTiempoRestanteEvento() throws RemoteException {
        System.out.println("[RMI] Petición getTiempoRestanteEvento recibida.");
        try {
            if (gestorEventos != null) {
                return gestorEventos.getTiempoRestante(); // getTiempoRestante() ya devuelve segundos
            } else {
                System.err.println("[ServidorHawkins] WARN: gestorEventos es null.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en getTiempoRestanteEvento: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al obtener tiempo restante", e);
        }
    }

    // Métodos de Control de la Simulación

    /**
     * Pausa la ejecución de la simulación mediante el controlador asociado.
     */
    @Override
    public void pausarSimulacion() throws RemoteException {
        System.out.println("[RMI] Petición pausarSimulacion recibida.");
        try {
            if (controlSimulacion != null) {
                controlSimulacion.pausar();
                System.out.println("[ServidorHawkins] Simulación pausada vía RMI.");
            } else {
                System.err.println("[ServidorHawkins] WARN: controlSimulacion es null.");
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en pausarSimulacion: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al pausar la simulación", e);
        }
    }

    /**
     * Reanuda la ejecución de la simulación mediante el controlador asociado.
     */
    @Override
    public void reanudarSimulacion() throws RemoteException {
        System.out.println("[RMI] Petición reanudarSimulacion recibida.");
        try {
            if (controlSimulacion != null) {
                controlSimulacion.reanudar();
                System.out.println("[ServidorHawkins] Simulación reanudada vía RMI.");
            } else {
                System.err.println("[ServidorHawkins] WARN: controlSimulacion es null.");
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en reanudarSimulacion: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al reanudar la simulación", e);
        }
    }

    /**
     * Comprueba si la simulación se encuentra actualmente en estado de pausa.
     */
    @Override
    public boolean estaSimulacionPausada() throws RemoteException {
        System.out.println("[RMI] Petición isSimulacionPausada recibida.");
        try {
            if (controlSimulacion != null) {
                return controlSimulacion.isPausada();
            } else {
                System.err.println("[ServidorHawkins] WARN: controlSimulacion es null.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ServidorHawkins] ERROR en isSimulacionPausada: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error del servidor al consultar estado de pausa", e);
        }
    }
}