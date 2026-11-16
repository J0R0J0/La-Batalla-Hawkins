package hawkins.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interfaz remota RMI para la monitorización y control de la simulación de
 * Hawkins.
 * Permite a los clientes remotos consultar el estado del sistema y pausar o
 * reanudar la ejecución.
 */
public interface InterfaceHawkins extends Remote {

    /**
     * Obtiene el número de niños actualmente en las zonas de Hawkins (mundo real).
     * 
     * @return Cantidad total de niños en Hawkins.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    int getNinosHawkins() throws RemoteException;

    /**
     * Obtiene la cantidad de niños que se encuentran cruzando cada uno de los
     * portales.
     * 
     * @return Array con el recuento de niños agrupados por portal.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    int[] getNinosPortales() throws RemoteException;

    /**
     * Obtiene la cantidad de niños distribuidos en las diferentes zonas del Upside
     * Down.
     * 
     * @return Array con el recuento de niños por zona del Upside Down.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    int[] getNinosUpsideDown() throws RemoteException;

    /**
     * Obtiene el número total de niños que han sido capturados y están en la
     * Colmena.
     * 
     * @return Cantidad de niños capturados actualmente.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    int getNinosCapturadosEnColmena() throws RemoteException;

    /**
     * Obtiene la cantidad de Demogorgons presentes en cada zona del Upside Down.
     * 
     * @return Array con el recuento de Demogorgons por zona.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    int[] getDemogorgonsUpsideDown() throws RemoteException;

    /**
     * Obtiene una lista con el ranking de los Demogorgons basado en su número de
     * capturas.
     * 
     * @return Lista de cadenas de texto formateadas con el ranking de Demogorgons.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    List<String> getRankingDemogorgons() throws RemoteException;

    /**
     * Obtiene el nombre o identificador del evento global que se encuentra activo.
     * 
     * @return Nombre del evento activo, o cadena vacía/null si no hay evento en
     *         curso.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    String getEventoActivo() throws RemoteException;

    /**
     * Obtiene el tiempo restante de duración del evento global actualmente activo.
     * 
     * @return Milisegundos restantes para que el evento finalice.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    long getTiempoRestanteEvento() throws RemoteException;

    // --- Métodos para control ---

    /**
     * Pausa la ejecución de todos los hilos de la simulación de forma global.
     * 
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    void pausarSimulacion() throws RemoteException;

    /**
     * Reanuda la ejecución de la simulación si esta se encontraba pausada.
     * 
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    void reanudarSimulacion() throws RemoteException;

    /**
     * Consulta si la simulación se encuentra actualmente en estado de pausa.
     * 
     * @return {@code true} si la simulación está pausada, {@code false} en caso
     *         contrario.
     * @throws RemoteException Si ocurre un error de comunicación RMI.
     */
    boolean estaSimulacionPausada() throws RemoteException;
}